package com.hackx.ruraledtech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hackx.ruraledtech.feature.navigation.RuralEdTechNavGraph
import com.hackx.ruraledtech.ui.theme.RuralEdTechTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuralEdTechTheme {
                RuralEdTechNavGraph()
            }
        }
    }
}
