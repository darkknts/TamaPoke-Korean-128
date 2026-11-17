package com.tamapoke.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialStatus = runCatching {
            PetNative.initialize(filesDir.absolutePath)
            PetNative.update()
            PetNative.engineStatus()
        }.getOrElse { "엔진 초기화 오류: ${it.message ?: "알 수 없는 오류"}" }
        val initialState = runCatching { PetNative.state() }.getOrNull()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PetHome(initialStatus, initialState)
                }
            }
        }
    }
}

@Composable
private fun PetHome(engineStatus: String, initialState: PetState?) {
    var pet by remember { mutableStateOf(initialState) }
    var status by remember { mutableStateOf(engineStatus) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            runCatching {
                PetNative.update()
                PetNative.state()
            }.onSuccess { pet = it }
                .onFailure { status = "게임 상태 갱신 오류: ${it.message ?: "확인 필요"}" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("TamaPoke", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("나만의 포켓몬 키우기", style = MaterialTheme.typography.bodyMedium)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    when {
                        pet == null -> "게임 데이터를 불러오는 중입니다"
                        pet!!.starterPick -> "첫 포켓몬을 선택해 주세요"
                        pet!!.egg -> "포켓몬 알"
                        else -> if (pet!!.shiny) "✨ 이로치 포켓몬" else "함께 성장하는 포켓몬"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when {
                        pet == null -> "잠시만 기다려 주세요"
                        pet!!.starterPick -> "이상해씨 · 파이리 · 꼬부기"
                        pet!!.egg -> "알을 터치해 부화시켜 보세요"
                        else -> "도감 번호 ${pet!!.speciesId}"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                if (pet != null && !pet!!.starterPick) {
                    Text("레벨 ${pet!!.level}", style = MaterialTheme.typography.titleLarge)
                }
                if (pet?.starterPick == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StarterButton("이상해씨", 1) { choose(1) { pet = PetNative.state() } }
                        StarterButton("파이리", 4) { choose(4) { pet = PetNative.state() } }
                        StarterButton("꼬부기", 7) { choose(7) { pet = PetNative.state() } }
                    }
                } else if (pet?.egg == true) {
                    Button(onClick = { runCatching { PetNative.eggTap(); pet = PetNative.state() } }) {
                        Text("알 톡톡 터치")
                    }
                } else if (pet != null) {
                    Text(if (pet!!.sleeping) "Zzz… 쉬는 중" else "오늘도 건강하게 돌봐 주세요")
                }
            }
        }

        if (pet != null && !pet!!.egg && !pet!!.starterPick) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("포켓몬 상태", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatBar("포만도", pet!!.fullness)
                    StatBar("행복도", pet!!.joy)
                    StatBar("에너지", pet!!.energy)
                    StatBar("위생", pet!!.hygiene)
                    Text("몸무게 ${pet!!.weight} / 100", style = MaterialTheme.typography.bodySmall)
                    Text("배설물 ${pet!!.poops} / 3", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("돌보기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("먹이 주기", Modifier.weight(1f)) { PetNative.feed() }
                        ActionButton("놀아 주기", Modifier.weight(1f)) { PetNative.play() }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("청소하기", Modifier.weight(1f)) { PetNative.clean() }
                        ActionButton("쓰다듬기", Modifier.weight(1f)) { PetNative.caress() }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton(if (pet!!.sleeping) "깨우기" else "재우기", Modifier.weight(1f)) { PetNative.toggleSleep() }
                        if (pet!!.canEvolve) ActionButton("진화하기", Modifier.weight(1f)) { PetNative.evolve() }
                    }
                }
            }
        }

        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("게임 데이터는 기기 내부에 저장됩니다.", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StarterButton(label: String, dex: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(label) }
}

private inline fun choose(dex: Int, refresh: () -> Unit) {
    runCatching { PetNative.chooseStarter(dex) }
    refresh()
}

@Composable
private fun StatBar(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = { runCatching(onClick) }, modifier = modifier, enabled = enabled) {
        Text(label)
    }
}
