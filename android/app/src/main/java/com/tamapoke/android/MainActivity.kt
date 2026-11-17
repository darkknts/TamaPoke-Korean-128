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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val PokePurple = Color(0xFF7653A6)
private val PokeInk = Color(0xFF30263D)
private val PokeBackground = Color(0xFFFFF9FF)
private val PokeLavender = Color(0xFFF0EAF7)
private val PokeMint = Color(0xFFE8F4EC)

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
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = PokePurple,
                    onPrimary = Color.White,
                    secondary = Color(0xFF54856B),
                    background = PokeBackground,
                    onBackground = PokeInk,
                    surface = Color.White,
                    onSurface = PokeInk,
                    surfaceVariant = PokeLavender,
                    onSurfaceVariant = Color(0xFF665B70)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = PokeBackground) {
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text("TamaPoke", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = PokeInk)
                Text("나만의 포켓몬 키우기", style = MaterialTheme.typography.labelMedium, color = Color(0xFF766B80))
            }
            Surface(color = PokeMint, shape = RoundedCornerShape(50)) {
                Text("● 함께 성장 중", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF47775A), fontWeight = FontWeight.Bold)
            }
        }

        if (pet == null) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PokeLavender)) {
                Text("포켓몬 정보를 불러오는 중입니다", modifier = Modifier.padding(20.dp))
            }
        } else if (pet!!.starterPick) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PokeLavender)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("첫 번째 파트너를 선택해 주세요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("함께 모험을 시작할 포켓몬", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        StarterButton("이상해씨", 1) { choose(1) { pet = PetNative.state() } }
                        StarterButton("파이리", 4) { choose(4) { pet = PetNative.state() } }
                        StarterButton("꼬부기", 7) { choose(7) { pet = PetNative.state() } }
                    }
                }
            }
        } else if (pet!!.egg) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PokeLavender)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("새로운 생명이 기다리고 있어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("포켓몬 알", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { runCatching { PetNative.eggTap(); pet = PetNative.state() } }) { Text("알 톡톡 터치") }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = PokeLavender),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (pet!!.shiny) "SPECIAL PARTNER ✨" else "MY PARTNER", style = MaterialTheme.typography.labelSmall, color = PokePurple, fontWeight = FontWeight.ExtraBold)
                        Text("도감 번호 ${pet!!.speciesId}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PokeInk)
                        Surface(color = Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(50)) {
                            Text("Lv. ${pet!!.level}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = PokePurple, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                        Text(if (pet!!.sleeping) "포근하게 잠들어 있어요" else "오늘도 즐거운 하루를 보내요!", style = MaterialTheme.typography.bodySmall, color = Color(0xFF62566D))
                    }
                    PetSprite(speciesId = pet!!.speciesId, shiny = pet!!.shiny, modifier = Modifier.size(126.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("오늘의 컨디션", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PokeInk)
                    Text("실시간 상태", style = MaterialTheme.typography.labelSmall, color = Color(0xFF857991))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("🍚", "포만도", pet!!.fullness, Modifier.weight(1f))
                    StatTile("💗", "행복도", pet!!.joy, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("⚡", "에너지", pet!!.energy, Modifier.weight(1f))
                    StatTile("🫧", "위생", pet!!.hygiene, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    InfoChip("⚖ 몸무게", "${pet!!.weight} / 100", Modifier.weight(1f))
                    InfoChip("🧹 배설물", "${pet!!.poops} / 3", Modifier.weight(1f))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("포켓몬 돌보기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("매일매일 교감해요", style = MaterialTheme.typography.labelSmall, color = Color(0xFF857991))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("🍓  먹이 주기", Modifier.weight(1f)) { PetNative.feed() }
                        ActionButton("🎾  놀아 주기", Modifier.weight(1f)) { PetNative.play() }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("🧹  청소하기", Modifier.weight(1f)) { PetNative.clean() }
                        ActionButton("💗  쓰다듬기", Modifier.weight(1f)) { PetNative.caress() }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton(if (pet!!.sleeping) "☀  깨우기" else "🌙  재우기", Modifier.weight(1f)) { PetNative.toggleSleep() }
                        if (pet!!.canEvolve) {
                            ActionButton("✨  진화하기", Modifier.weight(1f)) { PetNative.evolve() }
                        } else {
                            Surface(modifier = Modifier.weight(1f).height(44.dp), color = PokeMint, shape = RoundedCornerShape(14.dp)) {
                                BoxCenterText("건강하게 성장 중", Color(0xFF47775A))
                            }
                        }
                    }
                }
            }
        }

        Text(
            if (status.contains("오류")) status else "● 자동 저장 중 · 기기 내부 저장",
            style = MaterialTheme.typography.labelSmall,
            color = if (status.contains("오류")) MaterialTheme.colorScheme.error else Color(0xFF8B8191),
            maxLines = 1
        )
    }
}

@Composable
private fun StatTile(emoji: String, label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(emoji, style = MaterialTheme.typography.titleSmall)
                Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6E6378), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("$value%", style = MaterialTheme.typography.titleSmall, color = PokePurple, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { value.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (value < 30) Color(0xFFD97767) else PokePurple,
                trackColor = PokeLavender
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFFF5F0F8), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF746980))
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PokeInk)
        }
    }
}

@Composable
private fun BoxCenterText(value: String, color: Color) {
    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
        Text(value, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
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
private fun ActionButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = { runCatching(onClick) },
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = FontWeight.Bold)
    }
}
