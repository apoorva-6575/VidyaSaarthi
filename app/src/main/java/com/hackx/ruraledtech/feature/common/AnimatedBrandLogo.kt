package com.hackx.ruraledtech.feature.common

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hackx.ruraledtech.R
import kotlinx.coroutines.delay

/**
 * The brand reveal used at the top of the app's first screen and above the teacher login
 * form: fade+pop in, overshoot past full size (zoom in), settle back to rest (zoom out),
 * then stay. [onSettled] fires once the sequence finishes, so callers can reveal whatever
 * comes next (auto-navigate on Splash, or fade in a login form on the same screen).
 */
@Composable
fun AnimatedBrandLogo(
    modifier: Modifier = Modifier,
    onSettled: () -> Unit = {},
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    var settled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing))
        scale.animateTo(
            targetValue = 1.15f,
            animationSpec = tween(durationMillis = 450, easing = { OvershootInterpolator(2f).getInterpolation(it) }),
        )
        scale.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing))
        delay(150)
        settled = true
        onSettled()
    }

    Image(
        painter = painterResource(R.drawable.logo_vidyasaarthi_transparent),
        contentDescription = "VidyaSaarthi",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
    )
}
