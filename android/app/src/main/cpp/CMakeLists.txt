cmake_minimum_required(VERSION 3.22.1)
project(tamapoke_android LANGUAGES CXX)

add_library(tamapoke SHARED
    jni_bridge.cpp
)

target_compile_features(tamapoke PRIVATE cxx_std_17)
