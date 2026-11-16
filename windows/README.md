# TamaPoke for Windows

Native Windows desktop application target. This is kept separate from the
ESP32 Arduino firmware, so existing hardware builds are unaffected.

## Build with Visual Studio 2022

Install the Desktop development with C++ workload and CMake support, then run:

    cmake -S windows -B windows/build -A x64
    cmake --build windows/build --config Release

The executable is created at windows/build/Release/TamaPoke.exe.

## Porting status

- [x] Native Windows application target and initial desktop window
- [x] Initial round-screen-inspired layout
- [ ] Connect existing Pet game logic and persistent save data
- [ ] Port game screens, sprite animation, and care interactions
- [ ] Add settings, Pokedex, training, evolution, and minigames
- [ ] Package installer and Windows taskbar shortcut

IMPORTANT: The current executable is a UI shell, not yet a playable game.
The next stage connects pet.cpp and i18n.cpp to a Windows runtime and replaces
the placeholder preview with actual sprites, animations, and game screens.
