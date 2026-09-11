package com.hackx.ruraledtech.core.audio

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioManager {

    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableStateFlow(AudioPlaybackState.IDLE)
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState

    override fun play(assetPath: String) {
        release()
        try {
            mediaPlayer = MediaPlayer().apply {
                if (assetPath.startsWith("asset://")) {
                    val afd = context.assets.openFd(assetPath.removePrefix("asset://"))
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } else {
                    setDataSource(File(assetPath).absolutePath)
                }
                setOnCompletionListener { _playbackState.value = AudioPlaybackState.COMPLETED }
                setOnErrorListener { _, _, _ -> _playbackState.value = AudioPlaybackState.ERROR; true }
                prepare()
                start()
            }
            _playbackState.value = AudioPlaybackState.PLAYING
        } catch (e: Exception) {
            _playbackState.value = AudioPlaybackState.ERROR
        }
    }

    override fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
        _playbackState.value = AudioPlaybackState.PAUSED
    }

    override fun resume() {
        mediaPlayer?.start()
        _playbackState.value = AudioPlaybackState.PLAYING
    }

    override fun stop() {
        mediaPlayer?.stop()
        _playbackState.value = AudioPlaybackState.IDLE
    }

    override fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
