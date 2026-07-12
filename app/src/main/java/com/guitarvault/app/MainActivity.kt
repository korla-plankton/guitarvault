package com.guitarvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guitarvault.app.navigation.AppNavigation
import com.guitarvault.app.ui.theme.GuitarVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuitarVaultTheme {
                AppNavigation()
            }
        }
    }
}
