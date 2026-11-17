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
    val poops: Int,
    val weight: Int,
    val ageMinutes: Int,
    val canEvolve: Boolean,
    val sleeping: Boolean,
    val shiny: Boolean,
    val starterPick: Boolean
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
    external fun nativeClean()
    external fun nativeCaress()
    external fun nativeEggTap()
    external fun nativeToggleSleep()
    external fun nativeEvolve()
    external fun nativeChooseStarter(dex: Int)

    fun initialize(filesDir: String) = nativeInit(filesDir, System.currentTimeMillis() / 1000)
    fun engineStatus(): String = nativeEngineStatus()

    fun state(): PetState {
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
            poops = json.optInt("poops", 0),
            weight = json.optInt("weight", 0),
            ageMinutes = json.optInt("ageMinutes", 0),
            canEvolve = json.optBoolean("canEvolve"),
            sleeping = json.optBoolean("sleeping"),
            shiny = json.optBoolean("shiny"),
            starterPick = json.optBoolean("starterPick")
        )
    }

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

    fun update() = nativeUpdate(System.currentTimeMillis() / 1000)
    fun feed() = nativeFeed()
    fun play() = nativePlay()
    fun clean() = nativeClean()
    fun caress() = nativeCaress()
    fun eggTap() = nativeEggTap()
    fun toggleSleep() = nativeToggleSleep()
    fun evolve() = nativeEvolve()
    fun chooseStarter(dex: Int) = nativeChooseStarter(dex)
}
