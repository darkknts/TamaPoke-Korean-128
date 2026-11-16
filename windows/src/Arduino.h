#pragma once
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <algorithm>
using std::min;
using std::max;
uint32_t millis();
long random(long upper);
long random(long lower, long upper);
struct DesktopSerial { template<class... A> int printf(const char* f, A... a) { return ::printf(f, a...); } void begin(unsigned long) {} };
extern DesktopSerial Serial;
