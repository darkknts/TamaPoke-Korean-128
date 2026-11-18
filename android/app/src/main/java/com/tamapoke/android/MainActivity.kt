package com.tamapoke.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PokeInk = Color(0xFF40354D)
private val PokePurple = Color(0xFF8A63C7)
private val PokeBackground = Color(0xFFFFFCF8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(255, 252, 248)
        window.navigationBarColor = android.graphics.Color.rgb(255, 252, 248)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = PokePurple, background = PokeBackground,
                surface = Color.White, onSurface = PokeInk
            )) { NativeDiagnosticScreen() }
        }
    }
}

@Composable
private fun NativeDiagnosticScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("C++ 엔진 초기화 중...") }
    var state by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            PetNative.initialize(context.filesDir.absolutePath)
            status = PetNative.engineStatus()
            val pet = PetNative.state()
            state = "종족 ID: ${pet.speciesId}, 레벨: ${pet.level}, 알 상태: ${pet.egg}"
        } catch (error: Throwable) {
            status = "JNI 초기화 실패: ${error.javaClass.simpleName}"
            state = error.message ?: "상세 오류 메시지 없음"
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = PokeBackground) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("TamaPoke", color = PokeInk, fontSize = 30.sp, fontWeight = FontWeight.Medium)
            Text("C++ / JNI 진단 화면", modifier = Modifier.padding(top = 20.dp),
                color = PokePurple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(status, modifier = Modifier.padding(top = 28.dp), color = if (status.contains("완료")) Color(0xFF34865B) else Color(0xFFB3261E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(state, modifier = Modifier.padding(top = 16.dp), color = PokeInk, fontSize = 15.sp)
            Text("캐릭터 UI와 알림 기능은 아직 연결하지 않은 엔진 단독 테스트입니다.",
                modifier = Modifier.padding(top = 24.dp), color = PokeInk, fontSize = 13.sp)
        }
    }
}
