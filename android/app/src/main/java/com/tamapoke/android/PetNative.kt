package com.tamapoke.android

import org.json.JSONObject

data class DexEntry(
    val id: Int,
    val name: String,
    val registered: Boolean,
    val shiny: Boolean
)

data class PetState(
    val egg: Boolean,
    val speciesId: Int,
    val biome: Int,
    val level: Int,
    val fullness: Int,
    val joy: Int,
    val energy: Int,
    val hygiene: Int,
    val ageMinutes: Int,
    val canEvolve: Boolean,
    val sleeping: Boolean,
    val shiny: Boolean,
    val starterPick: Boolean,
    val atk: Int,
    val defense: Int,
    val speed: Int,
    val bond: Int,
    val streak: Int,
    val medals: Int,
    val registeredCount: Int,
    val nextLevelMinutes: Int
)

object PetNative {
    init { System.loadLibrary("tamapoke") }

    external fun nativeInit(filesDir: String, nowEpoch: Long)
    external fun nativeEngineStatus(): String
    external fun nativeGetState(): String
    external fun nativeGetDexState(): String
    external fun nativeUpdate(nowEpoch: Long)
    external fun nativeFeed()
    external fun nativePlay()
    external fun nativePlayResult(score: Int)
    external fun nativeClean()
    external fun nativeCaress()
    external fun nativeEggTap()
    external fun nativeToggleSleep()
    external fun nativeEvolve()
    external fun nativeChooseStarter(dex: Int)

    @Synchronized
    fun initialize(filesDir: String) = nativeInit(filesDir, System.currentTimeMillis() / 1000)

    @Synchronized
    fun engineStatus(): String = nativeEngineStatus()

    private fun readState(): PetState {
        val json = JSONObject(nativeGetState())
        return PetState(
            egg = json.optBoolean("egg"),
            speciesId = json.optInt("speciesId", -1),
            biome = json.optInt("biome", 0),
            level = json.optInt("level", 1),
            fullness = json.optInt("fullness", 0),
            joy = json.optInt("joy", 0),
            energy = json.optInt("energy", 0),
            hygiene = json.optInt("hygiene", 0),
            ageMinutes = json.optInt("ageMinutes", 0),
            canEvolve = json.optBoolean("canEvolve"),
            sleeping = json.optBoolean("sleeping"),
            shiny = json.optBoolean("shiny"),
            starterPick = json.optBoolean("starterPick"),
            atk = json.optInt("atk"),
            defense = json.optInt("def"),
            speed = json.optInt("speed"),
            bond = json.optInt("bond"),
            streak = json.optInt("streak"),
            medals = json.optInt("medals"),
            registeredCount = json.optInt("registeredCount"),
            nextLevelMinutes = json.optInt("nextLevelMinutes")
        )
    }

    @Synchronized
    fun state(): PetState = readState()

    /** Atomically advances the shared native engine and snapshots it for widgets. */
    @Synchronized
    fun refreshForWidget(filesDir: String): PetState {
        if (nativeEngineStatus() != "공통 C++ 엔진 연결 완료") {
            nativeInit(filesDir, System.currentTimeMillis() / 1000)
        }
        nativeUpdate(System.currentTimeMillis() / 1000)
        return readState()
    }

    @Synchronized
    fun dexEntries(): List<DexEntry> {
        val array = org.json.JSONArray(nativeGetDexState())
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            DexEntry(
                id = item.optInt("id"),
                name = item.optString("name"),
                registered = item.optBoolean("registered"),
                shiny = item.optBoolean("shiny")
            )
        }
    }

    @Synchronized fun update() = nativeUpdate(System.currentTimeMillis() / 1000)
    @Synchronized fun feed() = nativeFeed()
    @Synchronized fun play() = nativePlay()
    @Synchronized fun playResult(score: Int) = nativePlayResult(score)
    @Synchronized fun clean() = nativeClean()
    @Synchronized fun caress() = nativeCaress()
    @Synchronized fun eggTap() = nativeEggTap()
    @Synchronized fun toggleSleep() = nativeToggleSleep()
    @Synchronized fun evolve() = nativeEvolve()
    @Synchronized fun chooseStarter(dex: Int) = nativeChooseStarter(dex)
}
