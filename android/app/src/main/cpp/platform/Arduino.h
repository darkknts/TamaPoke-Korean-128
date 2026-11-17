#pragma once
#include <algorithm>
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
using std::max;
using std::min;
inline uint32_t millis() {
    using namespace std::chrono;
    static const auto start = steady_clock::now();
    return static_cast<uint32_t>(duration_cast<milliseconds>(steady_clock::now() - start).count());
}
inline long random(long upper) { return upper <= 0 ? 0 : std::rand() % upper; }
inline long random(long lower, long upper) { return upper <= lower ? lower : lower + random(upper - lower); }
struct AndroidSerial {
    template<class... A> int printf(const char* f, A... a) { return std::printf(f, a...); }
    void begin(unsigned long) {}
};
inline AndroidSerial Serial;
