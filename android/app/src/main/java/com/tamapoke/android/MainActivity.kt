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
                primary = PokePurple,
                background = PokeBackground,
                surface = Color.White,
                onSurface = PokeInk
            )) {
                SpriteDiagnosticScreen()
            }
        }
    }
}

@Composable
private fun SpriteDiagnosticScreen() {
    val context = LocalContext.current
    val spriteLoaded = androidx.compose.runtime.remember(context) {
        loadPokemonSprite(context, 1, false) != null
    }
    Surface(modifier = Modifier.fillMaxSize(), color = PokeBackground) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("TamaPoke", color = PokeInk, fontSize = 30.sp, fontWeight = FontWeight.Medium)
            Text(
                "캐릭터 이미지 진단 화면",
                modifier = Modifier.padding(top = 20.dp),
                color = PokePurple,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            PetSprite(
                speciesId = 1,
                shiny = false,
                modifier = Modifier.padding(top = 20.dp),
                imageSize = 180.dp
            )
            Text(
                if (spriteLoaded) "캐릭터 리소스 로드 성공" else "캐릭터 리소스 로드 실패",
                modifier = Modifier.padding(top = 16.dp),
                color = if (spriteLoaded) Color(0xFF34865B) else Color(0xFFB3261E),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "이 화면은 게임 엔진이나 JNI를 사용하지 않습니다.",
                modifier = Modifier.padding(top = 10.dp),
                color = PokeInk,
                fontSize = 14.sp
            )
        }
    }
}
