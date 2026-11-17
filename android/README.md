# TamaPoke Android

Android application for the TamaPoke virtual-pet game.

## Implemented

- Kotlin + Jetpack Compose mobile UI.
- Shared C++ pet engine connected through JNI.
- Local device persistence through the Android Preferences adapter.
- Real-time pet status refresh and care actions (feed, play, clean, pet, sleep, evolve).
- Starter selection (Bulbasaur, Charmander, Squirtle).
- TPK2 sprite loading for all 151 species and shiny variants when available.
- Multi-action sprite playback: idle, eat, hop, sleep, pose and nod, with idle fallback when an action is absent.
- Species habitat backgrounds based on the original six biome classifications.
- Korean Pokédex screen showing all 151 species, discovery status and shiny registration.
- Growth report with level progress, battle stats, bond, streak, medals and collection count.
- Medal collection screen for all eight engine achievements, including level milestones, berry discovery, care streak, bond, final evolution and fitness.
- Berry-catching mini-game with three lanes, a 20-second round and score rewards connected to the C++ engine.

## Development sequence

1. Stabilize core care actions, lifecycle and local save/restore.
2. Complete sprite animations and species/biome habitat presentation.
3. Expand the Pokédex with detail pages and collection records.
4. Connect growth, evolution and battle-stat presentation.
5. Balance all three mini-games and verify repeated-play reward limits.
6. Add rewards, medals, streaks and daily participation screens.
7. Verify backup/restore, sound settings, notifications and power behavior.
8. Run integrated tests and create the release APK.

## APK build policy

Automatic APK builds are paused on the development branch to avoid producing intermediate APKs. The Android workflow remains available through `workflow_dispatch`; restore the release build trigger when development is complete.

## Build requirements

- Android Studio with Android SDK Platform 35
- Android NDK 27.2
- CMake 3.22.1
- JDK 17
- Gradle 8.9

Open the `android` directory in Android Studio and sync Gradle.
