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
        val engineStatus = runCatching {
            PetNative.initialize(filesDir.absolutePath)
            PetNative.update()
            PetNative.engineStatus()
        }.getOrElse { "엔진 초기화 오류: ${it.message ?: "알 수 없는 오류"}" }
        val petState = runCatching { PetNative.state() }.getOrNull()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StartScreen(engineStatus, petState)
                }
            }
        }
    }
}

@Composable
private fun StartScreen(engineStatus: String, pet: PetState?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TamaPoke", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text("Android 공통 엔진 연결 확인")
        Spacer(Modifier.height(8.dp))
        Text(engineStatus, style = MaterialTheme.typography.bodyMedium)
        if (pet != null) {
            Spacer(Modifier.height(16.dp))
            Text(if (pet.egg) "알 상태" else "도감 번호: ${pet.speciesId}")
            Text("레벨 ${pet.level}")
            Text("포만도 ${pet.fullness} · 행복도 ${pet.joy}")
            Text("에너지 ${pet.energy} · 위생 ${pet.hygiene}")
            if (pet.starterPick) Text("첫 시작: 초기 포켓몬 선택 필요")
        }
    }
}
