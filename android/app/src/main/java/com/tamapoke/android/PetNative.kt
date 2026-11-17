package com.tamapoke.android

/**
 * JNI boundary for the shared C++ pet engine.
 * The engine is connected in the next implementation stage.
 */
object PetNative {
    init {
        System.loadLibrary("tamapoke")
    }

    external fun engineStatus(): String
}
