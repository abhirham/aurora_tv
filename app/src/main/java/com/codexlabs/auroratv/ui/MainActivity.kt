package com.codexlabs.auroratv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.codexlabs.auroratv.ui.theme.AuroraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuroraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuroraTvApp()
                }
            }
        }
    }
}
