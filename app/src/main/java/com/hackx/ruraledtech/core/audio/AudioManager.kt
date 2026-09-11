package com.hackx.ruraledtech.core.audio

import kotlinx.coroutines.flow.StateFlow

enum class AudioPlaybackState { IDLE, PLAYING, PAUSED, COMPLETED, ERROR }

/**
 * Plays packaged/local audio only (PS section 26) — lesson narration, instructions. Never
 * hits a network TTS API; that would silently reintroduce an internet dependency for a
 * "works offline" feature.
 */
interface AudioManager {
    val playbackState: StateFlow<AudioPlaybackState>
    fun play(assetPath: String)
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
