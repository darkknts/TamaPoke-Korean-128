# TamaPoke Android

Android application for the TamaPoke virtual-pet game.

## Implemented

- Kotlin + Jetpack Compose mobile UI.
- Shared C++ pet engine connected through JNI.
- Local device persistence through the Android Preferences adapter, with periodic engine flushes.
- Persistent Android settings for optional sound effects and haptic feedback.
- ZIP backup export and restore for the C++ engine save file, with save reload after restore.
- Two Android home-screen mini-pet widgets: compact 1×1 sprite/level and detailed 2×2 sprite/care meters. Tap either widget to open the game.
- Important care notifications for critical hunger, energy, hygiene and happiness states, with a six-hour repeat cooldown.
- Periodic widget refresh scheduled with WorkManager (15-minute minimum interval; Android may defer execution under battery/background limits).
- Real-time pet status refresh and care actions (feed, play, clean, pet, sleep, evolve).
- Starter selection (Bulbasaur, Charmander, Squirtle).
- TPK2 sprite loading for all 151 species and shiny variants when available.
- Multi-action sprite playback: idle, eat, hop, sleep, pose and nod, with idle fallback when an action is absent.
- Species habitat backgrounds based on the original six biome classifications.
- Korean Pokédex screen showing all 151 species, discovery status and shiny registration.
- Growth report with level progress, battle stats, bond, streak, medals and collection count.
- Medal collection screen for all eight engine achievements, including level milestones, berry discovery, care streak, bond, final evolution and fitness.
- Berry-catching mini-game with three lanes, a 20-second round and score rewards connected to the C++ engine.
- Both widget sizes render from one synchronized native-engine snapshot; refresh is skipped when neither widget is installed.
- Offline elapsed-time progression is applied by the shared C++ engine when it synchronizes the saved clock (up to 14 days); care stats have a minimum floor and evolution remains a user-triggered action.
- Widget sprite selection follows the current species and shiny state, so a completed evolution is reflected on the next widget refresh.

## Development sequence

1. Stabilize core care actions, lifecycle and local save/restore.
2. Complete sprite animations and species/biome habitat presentation.
3. Expand the Pokédex with detail pages and collection records.
4. Connect growth, evolution and battle-stat presentation.
5. Balance all three mini-games and verify repeated-play reward limits.
6. Add rewards, medals, streaks and daily participation screens.
7. Implement and verify 1×1 and 2×2 mini-pet home screen widgets using the current species sprite and live care values.
8. Verify ZIP backup/restore on multiple devices, sound and haptic settings, widget refresh and background lifecycle behavior.
9. Run integrated tests and create the release APK.

## APK build policy

Automatic APK builds are paused on the development branch to avoid producing intermediate APKs. Keep the final APK build for the release validation stage; do not treat the firmware CI as Android build verification.

## Build requirements

- Android Studio with Android SDK Platform 35
- Android NDK 27.2
- CMake 3.22.1
- JDK 17
- Gradle 8.9

Open the `android` directory in Android Studio and sync Gradle.
