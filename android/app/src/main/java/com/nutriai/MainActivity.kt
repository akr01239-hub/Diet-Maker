package com.nutriai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.nutriai.ui.health.HealthScreen
import com.nutriai.ui.theme.NutriAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriAiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HealthScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
