cmake_minimum_required(VERSION 3.22.1)
project(tamapoke_android LANGUAGES CXX)

add_library(tamapoke SHARED
    jni_bridge.cpp
    platform/Preferences.cpp
    platform/audio_android.cpp
    ../../../../../pet.cpp
)

target_include_directories(tamapoke PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/platform
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../
)

target_compile_features(tamapoke PRIVATE cxx_std_17)
target_compile_options(tamapoke PRIVATE -Wall -Wextra -Wno-unused-parameter)
