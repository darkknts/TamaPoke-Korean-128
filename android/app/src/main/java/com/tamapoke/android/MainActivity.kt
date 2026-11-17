package com.tamapoke.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val PokePurple = Color(0xFF8A63C7)
private val PokeInk = Color(0xFF40354D)
private val PokeBackground = Color(0xFFFFFCF8)
private val PokeLavender = Color(0xFFF4EEFF)
private val PokeMint = Color(0xFFEAF8F0)

private fun habitatGradient(biome: Int): List<Color> = when (biome) {
    1 -> listOf(Color(0xFFFFF0D9), Color(0xFFDDF5F2)) // beach
    2 -> listOf(Color(0xFFE8F5E7), Color(0xFFD5EAD5)) // forest
    3 -> listOf(Color(0xFFFFE8D9), Color(0xFFFFD6C4)) // volcano
    4 -> listOf(Color(0xFFF0EAFE), Color(0xFFE0DDF2)) // mountain
    5 -> listOf(Color(0xFFF0F8FF), Color(0xFFDCEEFF)) // snow
    else -> listOf(Color(0xFFEAF7E6), Color(0xFFD9F0E4)) // meadow
}

private fun habitatGround(biome: Int): Color = when (biome) {
    1 -> Color(0xFFBDE9E5)
    2 -> Color(0xFFC8E3C3)
    3 -> Color(0xFFF2C2A8)
    4 -> Color(0xFFD2CBE8)
    5 -> Color(0xFFD2E8F8)
    else -> Color(0xFFCDE8C8)
}

private fun habitatAccent(biome: Int): Color = when (biome) {
    1 -> Color(0xFF69B9B5)
    2 -> Color(0xFF72AD78)
    3 -> Color(0xFFE99A67)
    4 -> Color(0xFF9B8BC8)
    5 -> Color(0xFF73B7D9)
    else -> Color(0xFF72AD78)
}

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
                    secondary = Color(0xFF62A985),
                    background = PokeBackground,
                    onBackground = PokeInk,
                    surface = Color.White,
                    onSurface = PokeInk,
                    surfaceVariant = PokeLavender,
                    onSurfaceVariant = Color(0xFF776A83)
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
    var spriteAction by remember { mutableIntStateOf(0) }
    var spriteActionToken by remember { mutableIntStateOf(0) }
    var showDex by remember { mutableStateOf(false) }
    var showGrowth by remember { mutableStateOf(false) }
    var showGame by remember { mutableStateOf(false) }
    var dexEntries by remember { mutableStateOf(emptyList<DexEntry>()) }

    fun performPetAction(animationId: Int, action: () -> Unit) {
        runCatching {
            action()
            pet = PetNative.state()
            spriteAction = animationId
            spriteActionToken++
        }.onFailure {
            status = "돌보기 동작 오류: ${it.message ?: "다시 시도해 주세요"}"
        }
    }

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
            .statusBarsPadding()
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
                Text("TamaPoke", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF49375F))
                Text("나만의 포켓몬 키우기", style = MaterialTheme.typography.labelMedium, color = Color(0xFF766B80))
            }
            Surface(color = Color(0xFFE5F7EC), shape = RoundedCornerShape(50)) {
                Text("● 함께 성장 중", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF438A63), fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { showGrowth = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) { Text("성장", style = MaterialTheme.typography.labelMedium) }
                OutlinedButton(
                    onClick = {
                        dexEntries = runCatching { PetNative.dexEntries() }.getOrDefault(emptyList())
                        showDex = true
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) { Text("도감", style = MaterialTheme.typography.labelMedium) }
            }
        }

        if (showGame) {
            BerryCatchGame(
                onBack = { showGame = false },
                onResult = { score ->
                    runCatching {
                        PetNative.playResult(score)
                        pet = PetNative.state()
                    }.onFailure { status = "보상 반영 오류: ${it.message ?: "다시 시도해 주세요"}" }
                }
            )
        } else if (showDex) {
            DexScreen(entries = dexEntries, onBack = { showDex = false })
        } else if (showGrowth && pet != null && !pet!!.egg && !pet!!.starterPick) {
            GrowthScreen(pet = pet!!, onBack = { showGrowth = false })
        } else if (pet == null) {
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F8FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("MY PARTNER", style = MaterialTheme.typography.labelMedium, color = PokePurple, fontWeight = FontWeight.ExtraBold)
                            Text("도감 번호 ${pet!!.speciesId}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PokeInk)
                        }
                        Surface(color = Color.White, shape = RoundedCornerShape(50)) {
                            Text("Lv. ${pet!!.level}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), color = PokePurple, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(174.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(habitatGradient(pet!!.biome))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 22.dp).size(34.dp),
                            color = Color(0xBFFFFFFF), shape = RoundedCornerShape(50)
                        ) {}
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(end = 34.dp, top = 35.dp).size(22.dp),
                            color = Color(0xBFFFFFFF), shape = RoundedCornerShape(50)
                        ) {}
                        Text("✦", modifier = Modifier.align(Alignment.CenterStart).padding(start = 55.dp), color = habitatAccent(pet!!.biome), style = MaterialTheme.typography.titleLarge)
                        Text("✧", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 52.dp), color = when (pet!!.speciesId) { 1 -> Color(0xFF72AD78); 4 -> Color(0xFFF0A45B); 7 -> Color(0xFF73B7D9); else -> Color(0xFFB29ADE) }, style = MaterialTheme.typography.headlineSmall)
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(42.dp),
                            color = habitatGround(pet!!.biome),
                            shape = RoundedCornerShape(topStart = 70.dp, topEnd = 70.dp)
                        ) {}
                        PetSprite(
                            speciesId = pet!!.speciesId,
                            shiny = pet!!.shiny,
                            actionId = if (pet!!.sleeping) 3 else spriteAction,
                            actionToken = spriteActionToken,
                            loopAction = pet!!.sleeping,
                            modifier = Modifier.size(138.dp)
                        )
                    }
                    Text(
                        if (pet!!.sleeping) "포근하게 잠들어 있어요" else "오늘도 즐거운 하루를 보내요!",
                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF655777),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
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
                        OutlinedButton(
                            onClick = { showGame = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                        ) { Text("🍓 미니게임", style = MaterialTheme.typography.labelSmall) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("🍓  먹이 주기", Modifier.weight(1f)) { performPetAction(4) { PetNative.feed() } }
                        ActionButton("🎾  놀아 주기", Modifier.weight(1f)) { performPetAction(8) { PetNative.play() } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("🧹  청소하기", Modifier.weight(1f)) { performPetAction(9) { PetNative.clean() } }
                        ActionButton("💗  쓰다듬기", Modifier.weight(1f)) { performPetAction(7) { PetNative.caress() } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton(if (pet!!.sleeping) "☀  깨우기" else "🌙  재우기", Modifier.weight(1f)) { performPetAction(if (pet!!.sleeping) 0 else 3) { PetNative.toggleSleep() } }
                        if (pet!!.canEvolve) {
                            ActionButton("✨  진화하기", Modifier.weight(1f)) { performPetAction(7) { PetNative.evolve() } }
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
private fun BerryCatchGame(onBack: () -> Unit, onResult: (Int) -> Unit) {
    var playing by remember { mutableStateOf(true) }
    var remaining by remember { mutableIntStateOf(20) }
    var score by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var basketLane by remember { mutableIntStateOf(1) }
    var berryLane by remember { mutableIntStateOf(Random.nextInt(3)) }
    var berryProgress by remember { mutableFloatStateOf(0f) }
    var resultSent by remember { mutableStateOf(false) }

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var ticks = 0
        while (playing && remaining > 0) {
            delay(100)
            ticks++
            berryProgress += 0.05f
            if (berryProgress >= 0.9f) {
                if (berryLane == basketLane) score++ else misses++
                berryProgress = 0f
                berryLane = Random.nextInt(3)
            }
            if (ticks % 10 == 0) remaining--
        }
        if (remaining <= 0 && !resultSent) {
            playing = false
            resultSent = true
            onResult(score)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("열매 받기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PokeInk)
                Text("바구니를 움직여 떨어지는 열매를 받아요", style = MaterialTheme.typography.labelSmall, color = Color(0xFF766B80))
            }
            OutlinedButton(onClick = onBack) { Text("나가기") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("남은 시간  $remaining초", color = PokePurple, fontWeight = FontWeight.Bold)
            Text("점수  $score  ·  놓침 $misses", color = PokeInk, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { remaining / 20f }, modifier = Modifier.fillMaxWidth(), color = PokePurple, trackColor = PokeLavender)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFEAF6FF))
        ) {
            val laneWidth = maxWidth / 3
            repeat(2) { lane ->
                Box(Modifier.offset(x = laneWidth * (lane + 1).toFloat()).fillMaxHeight().width(1.dp).background(Color(0xFFD5E6F3)))
            }
            Text("🍓", modifier = Modifier.offset(x = laneWidth * berryLane.toFloat() + laneWidth / 2 - 16.dp, y = maxHeight * berryProgress), style = MaterialTheme.typography.headlineMedium)
            Text("🧺", modifier = Modifier.align(Alignment.BottomStart).offset(x = laneWidth * basketLane.toFloat() + laneWidth / 2 - 22.dp, y = (-8).dp), style = MaterialTheme.typography.headlineLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { basketLane = (basketLane - 1).coerceAtLeast(0) }, enabled = playing, modifier = Modifier.weight(1f)) { Text("◀ 왼쪽") }
            Button(onClick = { basketLane = (basketLane + 1).coerceAtMost(2) }, enabled = playing, modifier = Modifier.weight(1f)) { Text("오른쪽 ▶") }
        }
        if (!playing && remaining == 0) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PokeMint), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("게임 종료!", style = MaterialTheme.typography.titleMedium, color = PokeInk, fontWeight = FontWeight.Bold)
                    Text("$score개 획득 · 속도 훈련과 행복도 보상이 적용됐어요", style = MaterialTheme.typography.bodySmall, color = Color(0xFF47775A))
                }
            }
            Button(onClick = {
                remaining = 20; score = 0; misses = 0; basketLane = 1
                berryLane = Random.nextInt(3); berryProgress = 0f; resultSent = false; playing = true
            }, modifier = Modifier.fillMaxWidth()) { Text("다시 도전하기") }
        }
    }
}
@Composable
private fun GrowthScreen(pet: PetState, onBack: () -> Unit) {
    val progress = if (pet.level >= 999) 1f else (60 - pet.nextLevelMinutes).coerceIn(0, 60) / 60f
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("성장 리포트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PokeInk)
                Text("도감 번호 ${pet.speciesId} · Lv. ${pet.level}", style = MaterialTheme.typography.labelMedium, color = PokePurple, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack) { Text("돌아가기") }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F8FF)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("다음 레벨까지", color = PokeInk, fontWeight = FontWeight.Bold)
                    Text(if (pet.level >= 999) "최고 레벨" else "${pet.nextLevelMinutes}분 남음", color = PokePurple, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = PokePurple, trackColor = PokeLavender)
                Text("성장은 실제 경과 시간에 따라 진행됩니다.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF766B80))
            }
        }
        Text("전투 능력치", style = MaterialTheme.typography.titleMedium, color = PokeInk, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GrowthStat("공격", pet.atk, Modifier.weight(1f))
            GrowthStat("방어", pet.defense, Modifier.weight(1f))
            GrowthStat("속도", pet.speed, Modifier.weight(1f))
        }
        Text("교감 기록", style = MaterialTheme.typography.titleMedium, color = PokeInk, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InfoChip("💗 친밀도", "${pet.bond} / 100", Modifier.weight(1f))
            InfoChip("🔥 연속 돌봄", "${pet.streak}일", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InfoChip("🏅 메달", "${Integer.bitCount(pet.medals)} / 8", Modifier.weight(1f))
            InfoChip("📖 도감 등록", "${pet.registeredCount} / 151", Modifier.weight(1f))
        }
    }
}

@Composable
private fun GrowthStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF766B80))
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = PokePurple, fontWeight = FontWeight.ExtraBold)
        }
    }
}
@Composable
private fun DexScreen(entries: List<DexEntry>, onBack: () -> Unit) {
    val registeredCount = entries.count { it.registered }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("포켓몬 도감", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PokeInk)
                Text("$registeredCount / 151종 발견", style = MaterialTheme.typography.labelMedium, color = PokePurple, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack) { Text("돌아가기") }
        }
        LinearProgressIndicator(
            progress = { registeredCount / 151f },
            modifier = Modifier.fillMaxWidth(),
            color = PokePurple,
            trackColor = PokeLavender
        )
        entries.chunked(3).forEach { rowEntries ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                rowEntries.forEach { entry ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.registered) Color.White else Color(0xFFF3F0F5)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("#%03d".format(entry.id), style = MaterialTheme.typography.labelSmall, color = PokePurple, fontWeight = FontWeight.Bold)
                            Text(
                                if (entry.registered) entry.name else "???",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (entry.registered) PokeInk else Color(0xFF9B93A1),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                when {
                                    entry.shiny -> "✨ 이로치"
                                    entry.registered -> "발견 완료"
                                    else -> "미발견"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.shiny) Color(0xFFB58A32) else Color(0xFF8B8191)
                            )
                        }
                    }
                }
                repeat(3 - rowEntries.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatTile(emoji: String, label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                color = if (value < 30) Color(0xFFEE8C7B) else Color(0xFF9A76D5),
                trackColor = Color(0xFFEDE5F7)
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFFF2F7FF), shape = RoundedCornerShape(12.dp)) {
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
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF936BCB), contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = FontWeight.Bold)
    }
}
