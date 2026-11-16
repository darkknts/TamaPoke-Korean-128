# TamaPoke for Windows

A native Windows x64 desktop port, separate from the ESP32 firmware.

## Build

Install Visual Studio 2022 with **Desktop development with C++** and CMake
support. From PowerShell at the repository root:

    cmake -S windows -B build/windows -A x64
    cmake --build build/windows --config Release

Run `build/windows/Release/TamaPoke.exe`.

The GitHub Actions workflow also builds the Windows target and uploads a
`TamaPoke-Windows-x64` artifact when the branch is pushed. Open the repository's
Actions tab, select the successful CI run, and download the artifact.

## Current playable scope

- Uses the original `Pet` game logic from `pet.cpp` and the original
  `DEX_TBL` data, not a separate mock pet simulator.
- First-run starter selection (Bulbasaur, Charmander, Squirtle) and egg hatching.
- Feeding, play/care, bathing, sleep toggle, strength training and evolution.
- Home/status/Pokedex screens with the original 151-species registration data.
- Original pixel sprites for egg and the three starter evolution lines.
- Persistent save data under `%LOCALAPPDATA%\\TamaPoke\\save.dat`.
- Game time continues while the app is closed, using the same offline catch-up
  rules as the firmware.

## Porting limitations

- The original SD-card sprite pack contains the full 151-species artwork. The
  Windows port currently renders the built-in sprites for the egg and the
  starter evolution lines; other species use a simple placeholder until the
  sprite pack reader is ported.
- Minigame, rename keyboard, settings, sound and some animated dialogs remain
  to be ported.
- This is an early playable port; it has not yet been verified on a Windows PC.
