package com.tamapoke.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StartScreen()
                }
            }
        }
    }
}

@Composable
private fun StartScreen() {
    val engineMessage = runCatching { PetNative.engineStatus() }
        .getOrDefault("게임 엔진 연결 준비 중")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TamaPoke", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text("Android 버전 개발 초기 화면")
        Spacer(Modifier.height(8.dp))
        Text(engineMessage, style = MaterialTheme.typography.bodyMedium)
    }
}
