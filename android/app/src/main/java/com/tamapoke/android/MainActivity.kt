package com.tamapoke.android

import android.os.Bundle
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
        // Item 1 only: keep time, battery, and notification icons black on a white status bar.
        window.statusBarColor = android.graphics.Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
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

    override fun onResume() {
        super.onResume()
        runCatching { MiniPetWidgetRenderer.updateAll(this) }
    }

    override fun onDestroy() {
        SoundFeedback.release()
        super.onDestroy()
    }
}

@Composable
private fun PetHome(engineStatus: String, initialState: PetState?) {
    var pet by remember { mutableStateOf(initialState) }
    var status by remember { mutableStateOf(engineStatus) }
    var spriteAction by remember { mutableIntStateOf(0) }
    var spriteActionToken by remember { mutableIntStateOf(0) }
    var showDex by remember { mutableStateOf(false) }
    var selectedDexEntry by remember { mutableStateOf<DexEntry?>(null) }
    var showGrowth by remember { mutableStateOf(false) }
    var showAchievements by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var showGame by remember { mutableStateOf(false) }
    var showReleaseConfirm by remember { mutableStateOf(false) }
    var showWildEncounter by remember { mutableStateOf(false) }
    var showFriendVisit by remember { mutableStateOf(false) }
    var showFriendPlay by remember { mutableStateOf(false) }
    var friendVisitDex by remember { mutableIntStateOf(0) }
    var gameMode by remember { mutableStateOf("menu") }
    val context = LocalContext.current
    var dexEntries by remember { mutableStateOf(runCatching { PetNative.dexEntries() }.getOrDefault(emptyList())) }
    fun openMainScreen(screen: String) {
        showGame = false
        showManual = screen == "manual"
        showSettings = screen == "settings"
        showGrowth = screen == "growth"
        showDex = screen == "dex"
        showAchievements = false
        selectedDexEntry = null
        if (screen == "dex") {
            dexEntries = runCatching { PetNative.dexEntries() }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(pet?.speciesId, pet?.sleeping) {
        if (pet != null && !pet!!.egg && !pet!!.sleeping) {
            while (true) {
                delay(15 * 60 * 1000L)
                val current = runCatching { PetNative.state() }.getOrNull()
                if (current != null && !current.egg && !current.sleeping && !showWildEncounter && !showFriendVisit && !showFriendPlay) {
                    val knownFriends = dexEntries.filter { it.registered && it.id != current.speciesId }
                    if (knownFriends.isNotEmpty() && Random.nextInt(100) < 7) {
                        friendVisitDex = knownFriends.random().id
                        showFriendVisit = true
                    } else if (Random.nextInt(100) < 7) {
                        showWildEncounter = true
                    }
                }
            }
        }
    }
    LaunchedEffect(showFriendPlay) {
        if (showFriendPlay) {
            delay(10_000L)
            runCatching {
                PetNative.playWithFriend()
                pet = PetNative.state()
                MiniPetWidgetRenderer.updateAll(context)
                status = "친구 포켓몬과 즐겁게 놀았어요! 행복도와 친밀도가 올랐어요."
            }.onFailure { status = "친구 방문 보상 오류: ${it.message ?: "다시 시도해 주세요"}" }
            showFriendPlay = false
        }
    }
    var soundEnabled by remember { mutableStateOf(context.getSharedPreferences("tamapoke_settings", android.content.Context.MODE_PRIVATE).getBoolean("sound_enabled", true)) }
    var musicEnabled by remember { mutableStateOf(context.getSharedPreferences("tamapoke_settings", android.content.Context.MODE_PRIVATE).getBoolean("music_enabled", true)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(showGame, musicEnabled, lifecycleOwner) {
        var musicPlayer: MediaPlayer? = null
        val musicAsset = if (showGame) "audio/battle.m4a" else "audio/background.m4a"
        fun startMusic() {
            if (!musicEnabled || musicPlayer?.isPlaying == true) return
            runCatching {
                if (musicPlayer == null) {
                    val descriptor = context.assets.openFd(musicAsset)
                    musicPlayer = MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                        setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                        isLooping = true
                        setVolume(0.72f, 0.72f)
                        prepare()
                    }
                    descriptor.close()
                }
                musicPlayer?.start()
            }.onFailure {
                musicPlayer?.release()
                musicPlayer = null
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startMusic()
                Lifecycle.Event.ON_STOP -> runCatching { musicPlayer?.pause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startMusic()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { musicPlayer?.stop() }
            musicPlayer?.release()
            musicPlayer = null
        }
    }
    var hapticsEnabled by remember { mutableStateOf(context.getSharedPreferences("tamapoke_settings", android.content.Context.MODE_PRIVATE).getBoolean("haptics_enabled", true)) }
    var backupMessage by remember { mutableStateOf("") }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                val saveFile = java.io.File(context.filesDir, "tamapoke_tamapoke.prefs")
                check(saveFile.exists()) { "저장 데이터가 아직 없습니다." }
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    ZipOutputStream(output).use { zip ->
                        zip.putNextEntry(ZipEntry("tamapoke_tamapoke.prefs"))
                        saveFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                } ?: error("백업 파일을 만들 수 없습니다.")
                "백업 파일을 저장했습니다."
            }.getOrElse { "백업 실패: ${it.message}" }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            backupMessage = runCatching {
                var restored = false
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == "tamapoke_tamapoke.prefs") {
                                val target = java.io.File(context.filesDir, "tamapoke_tamapoke.prefs")
                                val temp = java.io.File(context.filesDir, "tamapoke_restore.tmp")
                                val bytes = java.io.ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    val read = zip.read(buffer)
                                    if (read < 0) break
                                    total += read
                                    check(total <= 2 * 1024 * 1024) { "백업 파일이 너무 큽니다." }
                                    bytes.write(buffer, 0, read)
                                }
                                // Finish the ZIP entry before accepting its contents so the
                                // ZIP CRC is checked by ZipInputStream.
                                zip.closeEntry()
                                val data = bytes.toByteArray()
                                check(data.isNotEmpty()) { "백업 파일이 비어 있습니다." }
                                val parsed = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                                check(parsed.remaining() >= 4) { "저장 데이터 헤더가 손상되었습니다." }
                                val count = parsed.int
                                check(count in 0..4096) { "저장 데이터 항목 수가 올바르지 않습니다." }
                                repeat(count) {
                                    check(parsed.remaining() >= 2) { "저장 데이터 키가 손상되었습니다." }
                                    val keySize = parsed.short.toInt() and 0xffff
                                    check(keySize in 1..1024 && parsed.remaining() >= keySize + 4) { "저장 데이터 키 길이가 올바르지 않습니다." }
                                    parsed.position(parsed.position() + keySize)
                                    val valueSize = parsed.int
                                    check(valueSize in 0..1_048_576 && parsed.remaining() >= valueSize) { "저장 데이터 값 길이가 올바르지 않습니다." }
                                    parsed.position(parsed.position() + valueSize)
                                }
                                check(!parsed.hasRemaining()) { "저장 데이터 뒤에 알 수 없는 내용이 있습니다." }
                                try {
                                    temp.outputStream().use { it.write(data) }
                                    try {
                                        java.nio.file.Files.move(
                                            temp.toPath(), target.toPath(),
                                            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                        )
                                    } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                                        java.nio.file.Files.move(
                                            temp.toPath(), target.toPath(),
                                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                        )
                                    }
                                    restored = true
                                } finally {
                                    if (temp.exists()) temp.delete()
                                }
                                break
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                } ?: error("백업 파일을 읽을 수 없습니다.")
                check(restored) { "TamaPoke 백업 파일을 찾을 수 없습니다." }
                PetNative.initialize(context.filesDir.absolutePath)
                pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context)
                "백업 데이터를 복원했습니다."
            }.getOrElse { "복원 실패: ${it.message}" }
        }
    }

    fun performPetAction(animationId: Int, action: () -> Unit) {
        runCatching {
            action()
            pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context)
            if (soundEnabled) SoundFeedback.play(context, 0)
            if (hapticsEnabled) SoundFeedback.vibrate(context)
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
            }.onSuccess { updated ->
                val previous = pet
                pet = updated
                // Refresh home-screen widgets only when a displayed pet value changes.
                if (
                    previous == null ||
                    previous.ageMinutes != updated.ageMinutes ||
                    previous.speciesId != updated.speciesId ||
                    previous.egg != updated.egg ||
                    previous.shiny != updated.shiny ||
                    previous.fullness != updated.fullness ||
                    previous.energy != updated.energy ||
                    previous.hygiene != updated.hygiene ||
                    previous.joy != updated.joy
                ) {
                    MiniPetWidgetRenderer.updateAll(context)
                }
            }.onFailure { status = "게임 상태 갱신 오류: ${it.message ?: "확인 필요"}" }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactLayout = maxWidth < 360.dp || maxHeight < 900.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compactLayout) 10.dp else 14.dp, vertical = if (compactLayout) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactLayout) 7.dp else 10.dp)
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text("TamaPoke", style = if (compactLayout) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF49375F))
                    Text("나만의 포켓몬 키우기", style = MaterialTheme.typography.labelMedium, color = Color(0xFF766B80))
                }
                Surface(color = Color(0xFFE5F7EC), shape = RoundedCornerShape(50)) {
                    Text("● 함께 성장 중", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF438A63), fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f).height(if (compactLayout) 38.dp else 42.dp), onClick = { openMainScreen("settings") }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("설정", maxLines = 1) }
                OutlinedButton(modifier = Modifier.weight(1f).height(if (compactLayout) 38.dp else 42.dp), onClick = { openMainScreen("growth") }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("성장", maxLines = 1) }
                OutlinedButton(modifier = Modifier.weight(1f).height(if (compactLayout) 38.dp else 42.dp), onClick = { openMainScreen("dex") }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("도감", maxLines = 1) }
                OutlinedButton(modifier = Modifier.weight(1f).height(if (compactLayout) 38.dp else 42.dp), onClick = { openMainScreen("manual") }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("매뉴얼", maxLines = 1) }
            }
        }

        if (showGame) {
            when (gameMode) {
                "berry" -> BerryCatchGame(
                    compact = compactLayout,
                    onBack = { gameMode = "menu" },
                    onResult = { score ->
                        runCatching {
                            PetNative.playResult(score)
                            pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context)
                            if (soundEnabled) SoundFeedback.play(context, 1)
                            if (hapticsEnabled) SoundFeedback.vibrate(context)
                        }
                            .onFailure { status = "보상 반영 오류: ${it.message ?: "다시 시도해 주세요"}" }
                    }
                )
                "timing" -> TimingGame(
                    compact = compactLayout,
                    onBack = { gameMode = "menu" },
                    onResult = { score ->
                        runCatching {
                            PetNative.playResult(score)
                            pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context)
                            if (soundEnabled) SoundFeedback.play(context, 1)
                            if (hapticsEnabled) SoundFeedback.vibrate(context)
                        }
                            .onFailure { status = "보상 반영 오류: ${it.message ?: "다시 시도해 주세요"}" }
                    }
                )
                "battle" -> WildBattleScreen(
                    compact = compactLayout,
                    pet = pet,
                    dexEntries = dexEntries,
                    onBack = { gameMode = "menu" },
                    onBattleWin = { dex, closeWin ->
                        runCatching { PetNative.applyBattleWin(dex, closeWin); pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context) }
                    },
                    onCapture = { dex ->
                        runCatching { PetNative.registerCaught(dex); pet = PetNative.state(); dexEntries = PetNative.dexEntries(); MiniPetWidgetRenderer.updateAll(context) }
                            .onFailure { status = "포획 저장 오류: ${it.message ?: "다시 시도해 주세요"}" }
                    }
                )
                "memory" -> MemoryGame(
                    compact = compactLayout,
                    onBack = { gameMode = "menu" },
                    onResult = { score ->
                        runCatching { PetNative.playResult(score); pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context) }
                            .onFailure { status = "보상 반영 오류: ${it.message ?: "다시 시도해 주세요"}" }
                    }
                )
                else -> MiniGameMenu(
                    compact = compactLayout,
                    onSelect = { gameMode = it },
                    onBack = { showGame = false }
                )
            }
        } else if (showManual) {
            GameManualScreen(compact = compactLayout, onBack = { showManual = false })
        } else if (showSettings) {
            SettingsScreen(
                compact = compactLayout,
                soundEnabled = soundEnabled,
                musicEnabled = musicEnabled,
                hapticsEnabled = hapticsEnabled,
                backupMessage = backupMessage,
                onBackup = { backupLauncher.launch("tamapoke-backup.zip") },
                onRestore = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                onSoundChange = {
                    soundEnabled = it
                    context.getSharedPreferences("tamapoke_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("sound_enabled", it).apply()
                    if (it) SoundFeedback.play(context, 1)
                },
                onMusicChange = {
                    musicEnabled = it
                    context.getSharedPreferences("tamapoke_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("music_enabled", it).apply()
                },
                onHapticsChange = {
                    hapticsEnabled = it
                    context.getSharedPreferences("tamapoke_settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("haptics_enabled", it).apply()
                    if (it) SoundFeedback.vibrate(context)
                },
                onBack = { showSettings = false }
            )
        } else if (showAchievements && pet != null && !pet!!.egg && !pet!!.starterPick) {
            AchievementScreen(pet = pet!!, compact = compactLayout, onBack = { showAchievements = false })
        } else if (showDex) {
            val selected = selectedDexEntry
            if (selected == null) {
                DexScreen(
                    entries = dexEntries,
                    compact = compactLayout,
                    onSelect = { selectedDexEntry = it },
                    onBack = { showDex = false }
                )
            } else {
                DexDetailScreen(
                    entry = selected,
                    compact = compactLayout,
                    onBack = { selectedDexEntry = null }
                )
            }
        } else if (showGrowth && pet != null && !pet!!.egg && !pet!!.starterPick) {
            GrowthScreen(pet = pet!!, compact = compactLayout, onBack = { showGrowth = false }, onAchievements = { showAchievements = true })
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
                        StarterButton("이상해씨", 1) { choose(1) { pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context) } }
                        StarterButton("파이리", 4) { choose(4) { pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context) } }
                        StarterButton("꼬부기", 7) { choose(7) { pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context) } }
                    }
                }
            }
        } else if (pet!!.egg) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PokeLavender)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("새로운 생명이 기다리고 있어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("포켓몬 알", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { runCatching { PetNative.eggTap(); pet = PetNative.state(); MiniPetWidgetRenderer.updateAll(context) } }) { Text("알 톡톡 터치") }
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (compactLayout) 8.dp else 14.dp, vertical = if (compactLayout) 2.dp else 3.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compactLayout) 1.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(if (compactLayout) 64.dp else 70.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.width(if (compactLayout) 104.dp else 118.dp).padding(start = 8.dp, top = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("나의 파트너", style = MaterialTheme.typography.labelMedium, color = PokePurple, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "No. ${pet!!.speciesId.toString().padStart(3, '0')}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PokePurple
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Text(
                                dexEntries.firstOrNull { it.id == pet!!.speciesId }?.name ?: "포켓몬",
                                modifier = Modifier.align(Alignment.TopCenter).offset(x = (-12).dp).padding(top = 0.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PokeInk,
                                maxLines = 1
                            )
                            EvolutionTrack(
                                entries = dexEntries,
                                currentId = pet!!.speciesId,
                                compact = compactLayout,
                                inline = true,
                                modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-12).dp).padding(bottom = 1.dp)
                            )
                            if (pet!!.shiny) {
                                Text(
                                    "✨ 이로치",
                                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp, top = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB58A32)
                                )
                            }
                        }
                        Surface(color = Color.White, shape = RoundedCornerShape(50)) {
                            Text("Lv. ${pet!!.level}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = PokePurple, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(if (compactLayout) 104.dp else 132.dp)
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
                            modifier = Modifier.size(if (compactLayout) 112.dp else 138.dp).offset(y = if (compactLayout) 5.dp else 6.dp)
                        )
                    }
                    Text(
                        if (pet!!.sleeping) "포근하게 잠들어 있어요" else "오늘도 즐거운 하루를 보내요!",
                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                        style = if (compactLayout) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                        color = Color(0xFF655777),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("오늘의 컨디션", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PokeInk)
                    Text("실시간 상태", style = MaterialTheme.typography.labelSmall, color = Color(0xFF857991))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("🍚", "포만도", pet!!.fullness, Modifier.weight(1f), compactLayout)
                    StatTile("💗", "행복도", pet!!.joy, Modifier.weight(1f), compactLayout)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile("⚡", "에너지", pet!!.energy, Modifier.weight(1f), compactLayout)
                    StatTile("🫧", "위생", pet!!.hygiene, Modifier.weight(1f), compactLayout)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(if (compactLayout) 8.dp else 10.dp), verticalArrangement = Arrangement.spacedBy(if (compactLayout) 5.dp else 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("포켓몬 돌보기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            // 버튼 순서를 고정: 작별 → 진화 → 미니게임
                            if (pet!!.canFarewell) {
                                OutlinedButton(
                                    onClick = { showReleaseConfirm = true },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text("💗 작별", color = Color(0xFF9B5360), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                            }
                            if (pet!!.canEvolve) {
                                OutlinedButton(
                                    onClick = { performPetAction(7) { PetNative.evolve() } },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text("✨ 진화", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                            }
                            OutlinedButton(
                                onClick = { showGame = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                            ) { Text("🍓 미니게임", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("🍓  먹이 주기", Modifier.weight(1f), compact = compactLayout) { performPetAction(4) { PetNative.feed() } }
                        ActionButton("🎾  놀아 주기", Modifier.weight(1f), compact = compactLayout) { performPetAction(8) { PetNative.play() } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("🧹  청소하기", Modifier.weight(1f), compact = compactLayout) { performPetAction(9) { PetNative.clean() } }
                        ActionButton("💗  쓰다듬기", Modifier.weight(1f), compact = compactLayout) { performPetAction(7) { PetNative.caress() } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton(if (pet!!.sleeping) "☀  깨우기" else "🌙  재우기", Modifier.weight(1f), compact = compactLayout) { performPetAction(if (pet!!.sleeping) 0 else 3) { PetNative.toggleSleep() } }
                        Surface(modifier = Modifier.weight(1f).height(if (compactLayout) 38.dp else 44.dp), color = PokeMint, shape = RoundedCornerShape(14.dp)) {
                            BoxCenterText("건강하게 성장 중", Color(0xFF47775A))
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
    if (showFriendVisit) {
        val visitorName = dexEntries.firstOrNull { it.id == friendVisitDex }?.name ?: "친구 포켓몬"
        AlertDialog(
            onDismissRequest = { showFriendVisit = false },
            title = { Text("💚 친구 포켓몬이 놀러왔어요!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${visitorName}가 함께 놀고 싶어 해요.")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        PetSprite(speciesId = pet?.speciesId ?: 1, shiny = pet?.shiny ?: false, actionId = 0, actionToken = 0, loopAction = false, modifier = Modifier.size(76.dp))
                        Text("💗", style = MaterialTheme.typography.titleLarge)
                        PetSprite(speciesId = friendVisitDex, shiny = dexEntries.firstOrNull { it.id == friendVisitDex }?.shiny ?: false, actionId = 0, actionToken = 0, loopAction = false, modifier = Modifier.size(76.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFriendVisit = false; showFriendPlay = true }) { Text("함께 놀기") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFriendVisit = false }) { Text("다음에") }
            }
        )
    }
    if (showFriendPlay) {
        val visitorName = dexEntries.firstOrNull { it.id == friendVisitDex }?.name ?: "친구 포켓몬"
        AlertDialog(
            onDismissRequest = { },
            title = { Text("🎉 친구와 즐거운 시간!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("내 포켓몬과 ${visitorName}가 함께 놀고 있어요.")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        PetSprite(speciesId = pet?.speciesId ?: 1, shiny = pet?.shiny ?: false, actionId = 1, actionToken = 1, loopAction = true, modifier = Modifier.size(84.dp))
                        Text("💞", style = MaterialTheme.typography.headlineMedium)
                        PetSprite(speciesId = friendVisitDex, shiny = dexEntries.firstOrNull { it.id == friendVisitDex }?.shiny ?: false, actionId = 1, actionToken = 1, loopAction = true, modifier = Modifier.size(84.dp))
                    }
                    Text("10초 후 행복도와 친밀도 보상을 받을 수 있어요.")
                }
            },
            confirmButton = { Text("함께 노는 중…", color = PokePurple, fontWeight = FontWeight.Bold) }
        )
    }
    if (showWildEncounter) {
        AlertDialog(
            onDismissRequest = { showWildEncounter = false },
            title = { Text("🌿 야생 포켓몬 출현!") },
            text = { Text("주변에서 야생 포켓몬의 기척이 느껴져요. 지금 배틀을 시작할까요?") },
            confirmButton = {
                Button(onClick = {
                    showWildEncounter = false
                    showGame = true
                    gameMode = "battle"
                }) { Text("싸운다") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWildEncounter = false }) { Text("나중에") }
            }
        )
    }
    if (pet?.ceremony == 1) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("💗 작별의 시간") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PetSprite(
                        speciesId = pet?.speciesId ?: 1,
                        shiny = pet?.shiny ?: false,
                        actionId = 1,
                        actionToken = 9901,
                        loopAction = true,
                        modifier = Modifier.size(132.dp)
                    )
                    Text("함께한 추억을 안고 새로운 모험을 떠나요", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    LinearProgressIndicator(
                        progress = { pet?.ceremonyT ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = PokePurple,
                        trackColor = PokeLavender
                    )
                    Text("새 알을 준비하고 있어요…", style = MaterialTheme.typography.labelSmall, color = PokePurple)
                }
            },
            confirmButton = { Text("작별 중…", color = PokePurple, fontWeight = FontWeight.Bold) }
        )
    }
    if (showReleaseConfirm) {
        AlertDialog(
            onDismissRequest = { showReleaseConfirm = false },
            title = { Text("포켓몬과 작별할까요?") },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { PetSprite(speciesId = pet?.speciesId ?: 1, shiny = pet?.shiny ?: false, actionId = 1, actionToken = 9001, loopAction = true, modifier = Modifier.size(112.dp)); Text("💞   ✨   🥚", style = MaterialTheme.typography.headlineMedium); Text("소중한 시간을 함께한 포켓몬과 작별합니다. 작별 의식이 끝나면 새 알을 받으며, 다음 알에 희귀도와 이로치 확률 보너스가 적용됩니다.", textAlign = androidx.compose.ui.text.style.TextAlign.Center) } },
            confirmButton = {
                Button(onClick = {
                    showReleaseConfirm = false
                    performPetAction(7) { PetNative.farewell() }
                }) { Text("작별하기") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReleaseConfirm = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun MiniGameMenu(compact: Boolean, onSelect: (String) -> Unit, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("미니게임 놀이터", style = MaterialTheme.typography.titleLarge, color = PokeInk, fontWeight = FontWeight.ExtraBold)
            OutlinedButton(onClick = onBack) { Text("닫기") }
        }
        Text("게임을 완료하면 점수에 따라 포켓몬 훈련 보상을 받아요.", color = Color(0xFF766B80), style = MaterialTheme.typography.bodySmall)
        Button(onClick = { onSelect("battle") }, modifier = Modifier.fillMaxWidth()) { Text("⚔️ 야생 포켓몬 배틀 · 승리하면 포획 도전") }
        Button(onClick = { onSelect("berry") }, modifier = Modifier.fillMaxWidth()) { Text("🍓 열매 받기  ·  좌우 이동") }
        Button(onClick = { onSelect("timing") }, modifier = Modifier.fillMaxWidth()) { Text("⏱ 타이밍 맞추기  ·  정확한 순간에 누르기") }
        Button(onClick = { onSelect("memory") }, modifier = Modifier.fillMaxWidth()) { Text("🧠 카드 짝 맞추기  ·  기억력 도전") }
    }
}

@Composable
private fun TimingGame(compact: Boolean, onBack: () -> Unit, onResult: (Int) -> Unit) {
    var round by remember { mutableIntStateOf(1) }
    var phase by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableFloatStateOf(1f) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    LaunchedEffect(finished) {
        while (!finished) {
            delay(24)
            phase += direction * 0.018f
            if (phase >= 1f) { phase = 1f; direction = -1f }
            if (phase <= 0f) { phase = 0f; direction = 1f }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("타이밍 맞추기", style = MaterialTheme.typography.titleLarge, color = PokeInk, fontWeight = FontWeight.ExtraBold)
            OutlinedButton(onClick = onBack) { Text("나가기") }
        }
        Text(if (finished) "도전 완료" else "$round / 10 라운드 · 초록 구간에 맞춰 누르세요", color = PokePurple, fontWeight = FontWeight.Bold)
        BoxWithConstraints(Modifier.fillMaxWidth().height(if (compact) 42.dp else 54.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFEDE8F5))) {
            val targetStart = maxWidth * 0.42f
            Box(Modifier.offset(x = targetStart).width(maxWidth * 0.16f).fillMaxHeight().background(Color(0xFFBFE8CD)))
            Box(Modifier.offset(x = maxWidth * phase - 5.dp).width(10.dp).fillMaxHeight().padding(vertical = 5.dp).background(PokePurple, RoundedCornerShape(8.dp)))
        }
        Button(onClick = {
            if (!finished) {
                if (phase in 0.42f..0.58f) score += 2 else if (phase in 0.34f..0.66f) score += 1
                if (round >= 10) { finished = true; if (!sent) { sent = true; onResult(score) } }
                else round++
            }
        }, enabled = !finished, modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 54.dp)) { Text("지금 누르기") }
        if (finished) {
            Card(colors = CardDefaults.cardColors(containerColor = PokeMint), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("최종 점수  $score / 20", color = PokeInk, fontWeight = FontWeight.Bold)
                    Text("점수에 따라 속도 훈련 보상이 반영됐어요.", color = Color(0xFF47775A), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("미니게임으로") }
        }
    }
}

@Composable
private fun MemoryGame(compact: Boolean, onBack: () -> Unit, onResult: (Int) -> Unit) {
    val icons = listOf("🍓", "🍋", "🍇", "🍎", "🍓", "🍋", "🍇", "🍎")
    var order by remember { mutableStateOf((0..7).shuffled()) }
    var opened by remember { mutableStateOf(emptyList<Int>()) }
    var matched by remember { mutableStateOf(emptySet<Int>()) }
    var turns by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var lock by remember { mutableStateOf(false) }
    LaunchedEffect(opened, matched) {
        if (opened.size == 2) {
            lock = true
            delay(650)
            val a = opened[0]; val b = opened[1]
            if (icons[order[a]] == icons[order[b]]) matched = matched + a + b
            opened = emptyList()
            lock = false
            if (matched.size == 8) {
                finished = true
                if (!sent) { sent = true; onResult((24 - turns).coerceAtLeast(0)) }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("카드 짝 맞추기", style = MaterialTheme.typography.titleLarge, color = PokeInk, fontWeight = FontWeight.ExtraBold)
            OutlinedButton(onClick = onBack) { Text("나가기") }
        }
        Text("같은 열매 카드 4쌍을 찾아보세요. 시도 횟수 ${turns}회", color = PokePurple, fontWeight = FontWeight.Bold)
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                for (col in 0..3) {
                    val index = row * 4 + col
                    val visible = index in opened || index in matched
                    Button(
                        onClick = { if (!lock && !finished && !visible && opened.size < 2) { opened = opened + index; if (opened.size == 1) turns++ } },
                        enabled = !lock && !finished && !visible,
                        modifier = Modifier.weight(1f).height(if (compact) 54.dp else 76.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (visible) icons[order[index]] else "❔", style = MaterialTheme.typography.headlineSmall) }
                }
            }
        }
        if (finished) {
            Card(colors = CardDefaults.cardColors(containerColor = PokeMint), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("모든 짝을 찾았어요!", color = PokeInk, fontWeight = FontWeight.Bold)
                    Text("시도 ${turns}회 · 훈련 점수 ${(24 - turns).coerceAtLeast(0)}점", color = Color(0xFF47775A), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = { order = (0..7).shuffled(); opened = emptyList(); matched = emptySet(); turns = 0; finished = false; sent = false }, modifier = Modifier.fillMaxWidth()) { Text("다시 도전하기") }
        }
    }
}
@Composable
private fun BerryCatchGame(compact: Boolean, onBack: () -> Unit, onResult: (Int) -> Unit) {
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

    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("열매 받기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PokeInk)
                Text("바구니를 움직여 떨어지는 열매를 받아요", style = MaterialTheme.typography.labelSmall, color = Color(0xFF766B80))
            }
            OutlinedButton(onClick = onBack) { Text("나가기") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("남은 시간  ${remaining}초", color = PokePurple, fontWeight = FontWeight.Bold)
            Text("점수  $score  ·  놓침 $misses", color = PokeInk, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { remaining / 20f }, modifier = Modifier.fillMaxWidth(), color = PokePurple, trackColor = PokeLavender)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(if (compact) 190.dp else 280.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFEAF6FF))
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
                    Text("${score}개 획득 · 속도 훈련과 행복도 보상이 적용됐어요", style = MaterialTheme.typography.bodySmall, color = Color(0xFF47775A))
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
private fun GameManualScreen(compact: Boolean, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TamaPoke 매뉴얼", style = MaterialTheme.typography.titleLarge, color = PokeInk, fontWeight = FontWeight.ExtraBold)
                Text("게임 이용 안내", style = MaterialTheme.typography.labelSmall, color = Color(0xFF766B80))
            }
            OutlinedButton(onClick = onBack) { Text("돌아가기") }
        }
        Text("처음 시작하기", style = MaterialTheme.typography.titleMedium, color = PokePurple, fontWeight = FontWeight.Bold)
        ManualCard("첫 포켓몬 선택", "처음 게임을 시작하면 이상해씨, 파이리, 꼬부기 중 한 마리를 선택합니다. 이후 새 포켓몬은 알을 통해 만날 수 있습니다.")
        ManualCard("알 부화", "알을 세 번 터치하면 부화합니다. 기다리면 자동으로 부화할 수도 있습니다.")
        Text("포켓몬 돌보기", style = MaterialTheme.typography.titleMedium, color = PokePurple, fontWeight = FontWeight.Bold)
        ManualCard("먹이 주기", "먹이 주기 버튼에서 열매 또는 사탕을 선택해 포만도를 관리합니다. 포켓몬마다 선호하는 열매가 있으며, 좋아하는 열매를 발견하면 추가 효과를 얻습니다.")
        ManualCard("놀아 주기", "놀이를 통해 행복도를 높일 수 있습니다. 놀이와 미니게임은 에너지를 사용하므로 상태를 확인해 주세요.")
        ManualCard("청소하기", "청소하기를 눌러 위생 상태를 회복합니다.")
        ManualCard("쓰다듬기", "포켓몬을 쓰다듬어 행복도와 유대감을 높입니다.")
        ManualCard("수면", "재우기/깨우기로 휴식 상태를 전환합니다. 잠자는 동안 에너지가 회복되고 돌봄 수치 감소가 느려집니다.")
        Text("성장과 진화", style = MaterialTheme.typography.titleMedium, color = PokePurple, fontWeight = FontWeight.Bold)
        ManualCard("레벨", "포켓몬은 실제 경과 시간에 따라 성장합니다. 성장 화면에서 다음 레벨까지 남은 시간을 확인할 수 있습니다.")
        ManualCard("진화", "진화 가능 레벨에 도달하고 네 가지 돌봄 수치 중 가장 낮은 값이 40 이상이어야 합니다. 조건을 충족해도 자동 진화하지 않으며, 홈 화면에 표시되는 진화하기 버튼을 직접 눌러야 합니다.")
        Text("미니게임", style = MaterialTheme.typography.titleMedium, color = PokePurple, fontWeight = FontWeight.Bold)
        ManualCard("열매 받기", "20초 동안 진행합니다. 왼쪽/오른쪽 버튼으로 바구니를 세 개 레인 사이에서 움직여 떨어지는 열매를 받으세요. 받은 개수가 점수로 반영됩니다.")
        ManualCard("타이밍 맞추기", "총 10라운드입니다. 움직이는 표시가 초록색 구간에 들어왔을 때 '지금 누르기'를 누르세요. 정확도에 따라 점수가 달라집니다.")
        ManualCard("카드 짝 맞추기", "열매 카드 8장에 숨겨진 같은 그림 4쌍을 찾으세요. 카드를 두 장씩 열어 짝을 맞추며, 시도 횟수에 따라 훈련 점수가 정해집니다.")
        Text("도감과 기록", style = MaterialTheme.typography.titleMedium, color = PokePurple, fontWeight = FontWeight.Bold)
        ManualCard("도감", "도감에서 151종의 발견 여부를 확인할 수 있습니다. 발견한 포켓몬을 선택하면 상세 정보와 이로치 발견 여부를 확인할 수 있습니다.")
        ManualCard("성장 리포트와 메달", "성장 화면에서 전투 능력치, 친밀도, 연속 돌봄, 메달 및 도감 등록 수를 확인할 수 있습니다. 메달 화면에서 8종의 업적 달성 여부를 볼 수 있습니다.")
        Text("작별과 새 포켓몬", style = MaterialTheme.typography.titleMedium, color = PokePurple, fontWeight = FontWeight.Bold)
        ManualCard("작별", "처음 만난 파트너는 마지막 진화까지 성장한 뒤 게임 시간 3일이 지나면 작별할 수 있습니다. 이후 만나는 포켓몬은 1차·2차 등 현재 진화 단계에서 3일이 지나면 작별할 수 있습니다. 작별 의식이 끝나면 새 알을 받으며, 다음 알의 희귀도와 이로치 확률에 보너스가 적용됩니다.")
        ManualCard("도주", "돌봄 수치가 모두 0인 상태가 14일 지속되면 도주할 수 있으며, 도주 후 새 알은 일반 등급으로 제한됩니다.")
        Text("설정과 저장 데이터", style = MaterialTheme.typography.titleMedium, color = PokePurple, fontWeight = FontWeight.Bold)
        ManualCard("효과음과 진동", "설정 화면에서 효과음과 진동 피드백을 각각 켜거나 끌 수 있습니다.")
        ManualCard("백업과 복원", "설정 화면에서 저장 데이터를 ZIP 파일로 내보내거나 복원할 수 있습니다. 복원하면 현재 기기의 저장 데이터가 백업 시점의 데이터로 교체되므로, 필요한 경우 먼저 백업해 주세요.")
        Text("안내 내용은 현재 Android 앱에 구현된 기능을 기준으로 작성되었습니다.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8B8191))
    }
}

@Composable
private fun ManualCard(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = PokeInk, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF766B80))
        }
    }
}

@Composable
private fun SettingsScreen(
    compact: Boolean,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    hapticsEnabled: Boolean,
    backupMessage: String,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onMusicChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("게임 설정", style = MaterialTheme.typography.titleLarge, color = PokeInk, fontWeight = FontWeight.ExtraBold)
            OutlinedButton(onClick = onBack) { Text("돌아가기") }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(if (compact) 10.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("효과음", color = PokeInk, fontWeight = FontWeight.Bold); Text("돌봄과 게임 동작의 짧은 알림음", color = Color(0xFF766B80), style = MaterialTheme.typography.bodySmall) }
                    androidx.compose.material3.Switch(checked = soundEnabled, onCheckedChange = onSoundChange)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("배경음악", color = PokeInk, fontWeight = FontWeight.Bold); Text("일반 화면은 배경음, 미니게임은 배틀음 반복 재생", color = Color(0xFF766B80), style = MaterialTheme.typography.bodySmall) }
                    androidx.compose.material3.Switch(checked = musicEnabled, onCheckedChange = onMusicChange)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("진동 피드백", color = PokeInk, fontWeight = FontWeight.Bold); Text("버튼을 누를 때 가벼운 진동", color = Color(0xFF766B80), style = MaterialTheme.typography.bodySmall) }
                    androidx.compose.material3.Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChange)
                }
            }
        }
        Text("저장 데이터 백업", style = MaterialTheme.typography.titleMedium, color = PokeInk, fontWeight = FontWeight.Bold)
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Text("백업 내보내기") }
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("백업 복원") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBackup, modifier = Modifier.weight(1f)) { Text("백업 내보내기") }
                OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) { Text("백업 복원") }
            }
        }
        if (backupMessage.isNotBlank()) Text(backupMessage, color = PokePurple, style = MaterialTheme.typography.bodySmall)
        Text("백업 파일은 ZIP 형식입니다. 복원하면 현재 기기의 저장 데이터가 백업 시점의 데이터로 교체됩니다.", color = Color(0xFF9A7650), style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun GrowthScreen(pet: PetState, compact: Boolean, onBack: () -> Unit, onAchievements: () -> Unit) {
    val progress = if (pet.level >= 999) 1f else (60 - pet.nextLevelMinutes).coerceIn(0, 60) / 60f
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("성장 리포트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PokeInk)
                Text("도감 번호 ${pet.speciesId} · Lv. ${pet.level}", style = MaterialTheme.typography.labelMedium, color = PokePurple, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack) { Text("돌아가기") }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F8FF)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(if (compact) 10.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        OutlinedButton(onClick = onAchievements, modifier = Modifier.fillMaxWidth()) { Text("🏅 메달 및 업적 확인") }
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
private fun AchievementScreen(pet: PetState, compact: Boolean, onBack: () -> Unit) {
    val medals = listOf(
        Triple(1, "🌱 첫 성장", "레벨 10 달성"),
        Triple(2, "🌿 꾸준한 성장", "레벨 25 달성"),
        Triple(4, "🌳 베테랑", "레벨 50 달성"),
        Triple(8, "🍓 취향 발견", "좋아하는 열매 발견"),
        Triple(16, "🔥 7일 연속 돌봄", "돌봄 연속 기록 7일 달성"),
        Triple(32, "💗 최고의 유대", "친밀도 100 달성"),
        Triple(64, "✨ 최종 진화", "최종 진화 단계 도달"),
        Triple(128, "💪 건강한 몸", "레벨 5 달성")
    )
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("메달 컬렉션", style = MaterialTheme.typography.titleLarge, color = PokeInk, fontWeight = FontWeight.ExtraBold)
                Text("${Integer.bitCount(pet.medals)} / 8 획득", color = PokePurple, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack) { Text("돌아가기") }
        }
        LinearProgressIndicator(progress = { Integer.bitCount(pet.medals) / 8f }, modifier = Modifier.fillMaxWidth(), color = PokePurple, trackColor = PokeLavender)
        medals.forEach { medal ->
            val earned = (pet.medals and medal.first) != 0
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (earned) Color(0xFFFFF6DF) else Color(0xFFF3F1F6)), shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.fillMaxWidth().padding(if (compact) 8.dp else 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (earned) medal.second.substringBefore(" ") else "🔒", style = MaterialTheme.typography.headlineSmall)
                    Column(Modifier.weight(1f)) {
                        Text(if (earned) medal.second.substringAfter(" ") else medal.second.substringAfter(" "), color = PokeInk, fontWeight = FontWeight.Bold)
                        Text(medal.third, color = Color(0xFF766B80), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (earned) "획득" else "미획득", color = if (earned) Color(0xFF9A6B1C) else Color(0xFF918A99), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
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
private fun EvolutionTrack(
    entries: List<DexEntry>,
    currentId: Int,
    compact: Boolean,
    inline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val path = remember(entries, currentId) {
        val byId = entries.associateBy { it.id }
        val result = mutableListOf<Int>()
        var cursor = currentId
        repeat(2) {
            val next = byId[cursor]?.evolvesTo ?: 0
            if (next > 0 && next !in result) {
                result.add(next)
                cursor = next
            }
        }
        result
    }
    if (inline && path.isEmpty()) return
    Row(
        modifier = if (inline) modifier else Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (inline) Arrangement.Start else Arrangement.Center
    ) {
        Text("진화", style = MaterialTheme.typography.labelSmall, color = PokePurple, fontWeight = FontWeight.Bold)
        if (!inline) Spacer(Modifier.width(7.dp))
        path.forEach { id ->
            Text("›", modifier = Modifier.padding(horizontal = if (compact) 1.dp else 2.dp), color = Color(0xFFAAA1B0), fontWeight = FontWeight.Bold)
            val evolutionEntry = entries.firstOrNull { it.id == id }
            if (evolutionEntry?.registered == true) {
                PetSprite(
                    speciesId = id,
                    shiny = evolutionEntry.shiny,
                    modifier = Modifier.size(if (inline) { if (compact) 30.dp else 36.dp } else { if (compact) 32.dp else 38.dp }),
                    imageSize = if (inline) { if (compact) 29.dp else 35.dp } else { if (compact) 31.dp else 37.dp }
                )
            } else {
                Box(
                    modifier = Modifier.size(if (inline) { if (compact) 30.dp else 36.dp } else { if (compact) 32.dp else 38.dp }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("??", color = Color(0xFF8F8795), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun DexScreen(entries: List<DexEntry>, compact: Boolean, onSelect: (DexEntry) -> Unit, onBack: () -> Unit) {
    val registeredCount = entries.count { it.registered }
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
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
        val columnCount = if (compact) 2 else 3
        entries.chunked(columnCount).forEach { rowEntries ->
            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp), modifier = Modifier.fillMaxWidth()) {
                rowEntries.forEach { entry ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(entry) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.registered) Color.White else Color(0xFFF3F0F5)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 7.dp else 9.dp, horizontal = 4.dp),
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
                            if (entry.registered) {
                                PetSprite(
                                    speciesId = entry.id,
                                    shiny = entry.shiny,
                                    modifier = Modifier.size(if (compact) 58.dp else 66.dp),
                                    imageSize = if (compact) 56.dp else 64.dp
                                )
                            } else {
                                Box(Modifier.size(if (compact) 58.dp else 66.dp), contentAlignment = Alignment.Center) {
                                    Text("?", color = Color(0xFFB8AFBE), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                when {
                                    entry.normal && entry.shiny -> "일반 ✓  ·  ✨ 이로치 ✓"
                                    entry.normal -> "일반 ✓  ·  이로치 미발견"
                                    entry.shiny -> "일반 미발견  ·  ✨ 이로치 ✓"
                                    else -> "일반 미발견  ·  이로치 미발견"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.shiny) Color(0xFFB58A32) else Color(0xFF8B8191)
                            )
                        }
                    }
                }
                repeat(columnCount - rowEntries.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}


@Composable
private fun DexDetailScreen(entry: DexEntry, compact: Boolean, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("포켓몬 상세", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PokeInk)
                Text("도감 번호 #${"%03d".format(entry.id)}", style = MaterialTheme.typography.labelMedium, color = PokePurple, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack) { Text("도감으로") }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F8FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 18.dp else 26.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (entry.registered) {
                    PetSprite(
                        speciesId = entry.id,
                        shiny = entry.shiny,
                        modifier = Modifier.size(if (compact) 156.dp else 190.dp),
                        imageSize = if (compact) 148.dp else 180.dp
                    )
                } else {
                    Box(
                        modifier = Modifier.size(if (compact) 156.dp else 190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", style = MaterialTheme.typography.displayLarge, color = Color(0xFFB8AFBE), fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    if (entry.registered) entry.name else "???",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (entry.registered) PokeInk else Color(0xFF9B93A1),
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    color = if (entry.registered) Color(0xFFE5F7EC) else Color(0xFFF0EDF2),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        if (entry.registered) "발견 완료" else "아직 발견하지 못했어요",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (entry.registered) Color(0xFF438A63) else Color(0xFF8B8191),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (entry.shiny) {
                    Text("✨ 이로치 포켓몬 발견!", color = Color(0xFFB58A32), fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("발견 기록", style = MaterialTheme.typography.titleMedium, color = PokeInk, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("일반 포켓몬", color = Color(0xFF766B80), style = MaterialTheme.typography.bodyMedium)
                    Text(if (entry.normal) "수집 완료" else "미수집", color = if (entry.normal) Color(0xFF438A63) else PokeInk, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("이로치", color = Color(0xFF766B80), style = MaterialTheme.typography.bodyMedium)
                    Text(if (entry.shiny) "수집 완료" else "미수집", color = if (entry.shiny) Color(0xFFB58A32) else PokeInk, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "현재 저장된 도감 기록을 기준으로 표시합니다.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8B8191)
        )
    }
}

@Composable
private fun StatTile(emoji: String, label: String, value: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 8.dp else 11.dp, vertical = if (compact) 7.dp else 9.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 5.dp)) {
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
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(modifier = modifier, color = Color(0xFFF2F7FF), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(horizontal = if (compact) 8.dp else 11.dp, vertical = if (compact) 6.dp else 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
private fun ActionButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, compact: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = { runCatching(onClick) },
        modifier = modifier.height(if (compact) 42.dp else 44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF936BCB), contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun WildBattleScreen(
    compact: Boolean,
    pet: PetState?,
    dexEntries: List<DexEntry>,
    onBack: () -> Unit,
    onBattleWin: (Int, Boolean) -> Unit,
    onCapture: (Int) -> Unit
) {
    val player = pet
    val eligible = remember(dexEntries) { dexEntries.filter { it.rarity == 1 || it.rarity == 2 } }
    var enemy by remember { mutableStateOf((if (Random.nextInt(100) < 25) eligible.filter { it.rarity == 2 } else eligible.filter { it.rarity == 1 }).randomOrNull() ?: eligible.randomOrNull()) }
    var enemyLevel by remember {
        val roll = Random.nextInt(100)
        val delta = when { roll < 55 -> roll % 3 - 1; roll < 85 -> -2 - roll % 3; else -> 2 + roll % 2 }
        mutableIntStateOf(((player?.level ?: 1) + delta).coerceIn(1, 100))
    }
    var playerHp by remember { mutableIntStateOf(0) }
    var enemyHp by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(1) }
    var skill1Uses by remember { mutableIntStateOf(3) }
    var skill2Uses by remember { mutableIntStateOf(2) }
    var recoverUses by remember { mutableIntStateOf(2) }
    var result by remember { mutableStateOf<String?>(null) }
    var captureResult by remember { mutableStateOf<String?>(null) }
    var captured by remember { mutableStateOf(false) }
    var closeWin by remember { mutableStateOf(false) }
    var rewardApplied by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("야생 포켓몬이 나타났어요!") }
    var impactToken by remember { mutableIntStateOf(0) }
    var impactVisible by remember { mutableStateOf(false) }
    LaunchedEffect(impactToken) {
        if (impactToken > 0) {
            impactVisible = true
            delay(520)
            impactVisible = false
        }
    }
    val pMax = remember(player) { (30 + (player?.level ?: 1) * 5 + (player?.defense ?: 50)).coerceAtLeast(1) }
    val eMax = remember(enemy, enemyLevel) { (30 + enemyLevel * 5 + (enemy?.defense ?: 40) + enemyLevel).coerceAtLeast(1) }
    LaunchedEffect(enemy?.id, enemyLevel) { playerHp = pMax; enemyHp = eMax }
    fun finishIfNeeded() {
        if (enemyHp <= 0) {
            result = "승리!"
            closeWin = playerHp <= pMax / 3
            if (!rewardApplied) { rewardApplied = true; onBattleWin(enemy!!.id, closeWin) }
            log = "야생 ${enemy?.name ?: "포켓몬"}에게 승리했어요!"
        } else if (playerHp <= 0) {
            result = "패배"
            log = "포켓몬이 쓰러졌어요."
        } else if (round > 20) {
            if (playerHp >= enemyHp) { result = "승리!"; closeWin = playerHp <= pMax / 3; if (!rewardApplied) { rewardApplied = true; onBattleWin(enemy!!.id, closeWin) } }
            else result = "패배"
        }
    }
    fun attack(power: Int) {
        val pAtk = player?.atk ?: 50
        val pDef = player?.defense ?: 50
        val pSpe = player?.speed ?: 50
        val eAtk = (enemy?.atk ?: 50) + enemyLevel
        val eDef = (enemy?.defense ?: 50) + enemyLevel
        val eSpe = (enemy?.speed ?: 50) + enemyLevel
        val first = pSpe >= eSpe
        val pDamage = ((eMax * (16 + (pAtk - eDef) / 12).coerceIn(11, 23) / 100.0) * power / 100.0 * Random.nextInt(90, 111) / 100.0).toInt().coerceAtLeast(1)
        val enemySkillPower = when {
            enemyLevel >= 20 && Random.nextInt(100) < 35 -> 135
            enemyLevel >= 10 && Random.nextInt(100) < 50 -> 120
            else -> 100
        }
        val eDamage = ((pMax * (16 + (eAtk - pDef) / 12).coerceIn(11, 23) / 100.0) * enemySkillPower / 100.0 * Random.nextInt(90, 111) / 100.0).toInt().coerceAtLeast(1)
        if (first) {
            enemyHp = (enemyHp - pDamage).coerceAtLeast(0)
            if (enemyHp > 0) playerHp = (playerHp - eDamage).coerceAtLeast(0)
        } else {
            playerHp = (playerHp - eDamage).coerceAtLeast(0)
            if (playerHp > 0) enemyHp = (enemyHp - pDamage).coerceAtLeast(0)
        }
        log = "내 공격 $pDamage · 상대 공격 $eDamage"
        round++
        finishIfNeeded()
    }
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("⚔️ 야생 배틀", style = MaterialTheme.typography.titleLarge, color = PokeInk, fontWeight = FontWeight.ExtraBold)
            OutlinedButton(onClick = onBack) { Text("나가기") }
        }
        if (player == null || player.egg || player.sleeping) {
            Text(if (player?.sleeping == true) "잠든 포켓몬은 배틀할 수 없어요. 먼저 깨워 주세요." else "부화한 포켓몬이 있어야 배틀할 수 있어요.")
            Button(onClick = onBack) { Text("돌아가기") }
            return@Column
        }
        if (enemy == null) {
            Text("배틀 가능한 야생 포켓몬이 없습니다.")
            Button(onClick = onBack) { Text("돌아가기") }
            return@Column
        }
        // Battle stage: animated Pokémon sprites on a habitat-inspired pixel arena.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7E6))
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("야생 포켓몬 · Lv.$enemyLevel", color = PokeInk, fontWeight = FontWeight.Bold)
                            Text(enemy?.name ?: "야생 포켓몬", style = MaterialTheme.typography.titleMedium, color = PokeInk)
                        }
                        Text("HP $enemyHp / $eMax", color = PokeInk, style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(progress = { enemyHp.toFloat() / eMax }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF5BAE77))
                }
            }
            Box(
                Modifier.fillMaxWidth().height(if (compact) 218.dp else 252.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFDDF0FF), Color(0xFFEAF6DF), Color(0xFFD2EAC8))))
            ) {
                Box(Modifier.fillMaxWidth().height(74.dp).align(Alignment.BottomCenter).background(Color(0xFFB9DFAE)))
                Box(Modifier.size(116.dp).align(Alignment.BottomCenter).offset(y = 26.dp).background(Color(0xFFA5D49A), RoundedCornerShape(50)))
                Box(Modifier.size(66.dp).align(Alignment.TopStart).offset(x = 22.dp, y = 28.dp).background(Color(0x66FFFFFF), RoundedCornerShape(50)))
                Box(Modifier.size(42.dp).align(Alignment.TopEnd).offset(x = (-28).dp, y = 48.dp).background(Color(0x77FFFFFF), RoundedCornerShape(50)))
                Text("WILD", Modifier.align(Alignment.TopCenter).padding(top = 10.dp), color = Color(0xFF6D8B78), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                PetSprite(
                    speciesId = enemy!!.id,
                    shiny = false,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 26.dp, top = 44.dp),
                    imageSize = if (compact) 104.dp else 122.dp
                )
                PetSprite(
                    speciesId = player.speciesId,
                    shiny = player.shiny,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 8.dp),
                    imageSize = if (compact) 122.dp else 144.dp
                )
                Text("내 포켓몬", Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 2.dp), color = PokeInk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                if (impactVisible) {
                    Text(
                        if (impactToken % 2 == 0) "💥 타격!" else "✨ 기술 발동!",
                        Modifier.align(Alignment.Center).background(Color(0xDDFFFFFF), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                        color = PokePurple,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PokeLavender)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("내 포켓몬 · Lv.${player.level}", color = PokeInk, fontWeight = FontWeight.Bold)
                            Text("No. ${player.speciesId}", color = PokeInk, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("HP $playerHp / $pMax", color = PokeInk, style = MaterialTheme.typography.labelMedium)
                    }
                    LinearProgressIndicator(progress = { playerHp.toFloat() / pMax }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF8A63C7))
                    Text("공격 ${player.atk} · 방어 ${player.defense} · 속도 ${player.speed}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF766B80))
                }
            }
        }
        Text("R${(round.coerceAtMost(21))} · $log", color = PokePurple, fontWeight = FontWeight.SemiBold)
        if (result == null) {
            val canSkill1 = player.level >= 10 && skill1Uses > 0
            val canSkill2 = player.level >= 20 && skill2Uses > 0
            Button(onClick = { attack(100) }, modifier = Modifier.fillMaxWidth()) { Text("기본 공격") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { skill1Uses--; attack(120) }, enabled = canSkill1, modifier = Modifier.weight(1f)) { Text("기술 1 ($skill1Uses)") }
                OutlinedButton(onClick = { skill2Uses--; attack(135) }, enabled = canSkill2, modifier = Modifier.weight(1f)) { Text("기술 2 ($skill2Uses)") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    if (recoverUses > 0 && (playerHp < pMax || skill1Uses < 3 || skill2Uses < 2)) {
                        recoverUses--; playerHp = (playerHp + (pMax * 0.28f).toInt()).coerceAtMost(pMax)
                        skill1Uses = (skill1Uses + 1).coerceAtMost(3); skill2Uses = (skill2Uses + 1).coerceAtMost(2)
                        val eDamage = ((enemy?.atk ?: 50) + enemyLevel - (player.defense)).coerceAtLeast(8)
                        playerHp = (playerHp - eDamage).coerceAtLeast(0); round++; log = "회복했지만 상대 공격으로 $eDamage 피해를 받았어요."; finishIfNeeded()
                    } else log = "지금은 회복할 수 없어요."
                }, enabled = recoverUses > 0, modifier = Modifier.weight(1f)) { Text("회복 ($recoverUses)") }
                OutlinedButton(onClick = {
                    val chance = (40 + (player.speed - ((enemy?.speed ?: 50) + enemyLevel)) / 2).coerceIn(25, 90)
                    if (Random.nextInt(100) < chance) { result = "도망 성공"; log = "무사히 도망쳤어요." }
                    else { playerHp = (playerHp - ((enemy?.atk ?: 50) + enemyLevel) / 2).coerceAtLeast(0); round++; log = "도망에 실패했어요."; finishIfNeeded() }
                }, modifier = Modifier.weight(1f)) { Text("도망") }
            }
        } else {
            Text(result!!, style = MaterialTheme.typography.headlineMedium, color = if (result!!.startsWith("승리")) Color(0xFF438A63) else PokePurple, fontWeight = FontWeight.ExtraBold)
            if (result!!.startsWith("승리") && captureResult == null) {
                val gap = enemyLevel - player.level
                val baseChance = if (enemy?.rarity == 2) 28 else 55
                val levelAdjust = if (gap > 0) -gap * 4 else -gap * 2
                val chance = if (enemy?.rarity == 3) 0 else (baseChance + levelAdjust + (if (closeWin) 8 else 0) + player.bond / 20).coerceIn(10, if (enemy?.rarity == 2) 60 else 75)
                Text("포획 확률 $chance%")
                Button(onClick = {
                    if (chance > 0 && Random.nextInt(100) < chance) {
                        onCapture(enemy!!.id); captured = true; captureResult = "포획 성공! 도감에 등록했어요."
                    } else captureResult = if (chance == 0) "이 포켓몬은 포획할 수 없어요." else "포획에 실패했어요. 야생 포켓몬이 도망갔어요."
                }, modifier = Modifier.fillMaxWidth(), enabled = !captured) { Text("🎯 포획하기") }
                OutlinedButton(onClick = { captureResult = "야생 포켓몬을 보내줬어요." }, modifier = Modifier.fillMaxWidth()) { Text("보내주기") }
            }
            captureResult?.let { Text(it, color = PokePurple, fontWeight = FontWeight.Bold) }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("미니게임으로 돌아가기") }
        }
    }
}
