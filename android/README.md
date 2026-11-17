# TamaPoke Android

Android application project for the TamaPoke mobile port.

## Current stage

- Native Android app shell using Kotlin and Jetpack Compose.
- CMake/NDK shared-library target and JNI boundary.
- The existing pet engine is not linked yet; its Arduino and Preferences dependencies
  must first be provided through an Android platform adapter.
- Mobile mini-pet overlay and important-state notifications are planned for a later
  stage after the core pet engine and app lifecycle are connected.

## Build requirements

- Android Studio with Android SDK Platform 35
- Android NDK
- CMake 3.22.1
- JDK 17

Open the `android` directory in Android Studio and sync Gradle.
