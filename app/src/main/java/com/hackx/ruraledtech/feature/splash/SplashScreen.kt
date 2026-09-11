package com.hackx.ruraledtech.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.hackx.ruraledtech.feature.common.AnimatedBrandLogo
import com.hackx.ruraledtech.ui.theme.BrandBlue

/**
 * The app's brand moment (logo pops in, zooms in, settles back out) plays every cold start
 * while [SplashViewModel] resolves where to route next. Navigation only fires once both the
 * animation has settled AND the destination is known, so a fast route resolution never cuts
 * the animation short.
 */
@Composable
fun SplashScreen(
    onDestinationReady: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.startDestination.collectAsState()
    var animationSettled by remember { mutableStateOf(false) }

    LaunchedEffect(destination, animationSettled) {
        if (animationSettled) {
            destination?.let(onDestinationReady)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BrandBlue),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedBrandLogo(onSettled = { animationSettled = true })
    }
}
