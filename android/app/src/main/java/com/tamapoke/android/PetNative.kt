package com.tamapoke.android

import org.json.JSONObject

data class PetState(
    val egg: Boolean,
    val speciesId: Int,
    val level: Int,
    val fullness: Int,
    val joy: Int,
    val energy: Int,
    val hygiene: Int,
    val poops: Int,
    val sleeping: Boolean,
    val shiny: Boolean,
    val starterPick: Boolean
)

object PetNative {
    init { System.loadLibrary("tamapoke") }

    external fun nativeInit(filesDir: String, nowEpoch: Long)
    external fun nativeEngineStatus(): String
    external fun nativeGetState(): String
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
            level = json.optInt("level", 1),
            fullness = json.optInt("fullness", 0),
            joy = json.optInt("joy", 0),
            energy = json.optInt("energy", 0),
            hygiene = json.optInt("hygiene", 0),
            poops = json.optInt("poops", 0),
            sleeping = json.optBoolean("sleeping"),
            shiny = json.optBoolean("shiny"),
            starterPick = json.optBoolean("starterPick")
        )
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
