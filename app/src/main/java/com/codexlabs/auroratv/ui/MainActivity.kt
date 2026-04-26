package com.codexlabs.auroratv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.codexlabs.auroratv.app.configureTvWindow
import com.codexlabs.auroratv.ui.theme.AuroraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTvWindow()
        setContent {
            AuroraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuroraTvApp()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureTvWindow()
        }
    }
}
