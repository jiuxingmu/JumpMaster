package com.jumpmaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jumpmaster.app.ui.navigation.JumpMasterNavHost
import com.jumpmaster.app.ui.theme.JumpMasterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JumpMasterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JumpMasterNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
