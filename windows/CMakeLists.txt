cmake_minimum_required(VERSION 3.20)
project(TamaPokeDesktop LANGUAGES CXX)

if(NOT WIN32)
  message(FATAL_ERROR "TamaPokeDesktop currently targets Windows.")
endif()

add_executable(TamaPoke WIN32
  src/main.cpp
  src/runtime.cpp
  ../pet.cpp
  ../i18n.cpp
)

target_include_directories(TamaPoke PRIVATE src ..)
target_compile_features(TamaPoke PRIVATE cxx_std_17)
target_compile_definitions(TamaPoke PRIVATE
  UNICODE
  _UNICODE
  WIN32_LEAN_AND_MEAN
  NOMINMAX
)
if(MSVC)
  target_compile_options(TamaPoke PRIVATE /utf-8 /W4)
endif()
target_link_libraries(TamaPoke PRIVATE shell32 msimg32)

# Bundle the existing 151-species PMD sprite archive beside the executable.
add_custom_command(TARGET TamaPoke POST_BUILD
  COMMAND ${CMAKE_COMMAND} -E copy_if_different
    "${CMAKE_CURRENT_SOURCE_DIR}/../web/sprites.pak"
    "$<TARGET_FILE_DIR:TamaPoke>/sprites.pak"
)
