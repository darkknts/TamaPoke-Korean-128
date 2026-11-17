#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <memory>
#include <string>

#include "Arduino.h"
#include "pet.h"
#include "dex.h"
#include "Preferences.h"

namespace {
std::unique_ptr<Pet> g_pet;
uint32_t g_lastClockMinute = 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tamapoke_android_PetNative_nativeInit(JNIEnv* env, jobject, jstring filesDir, jlong nowEpoch) {
    const char* path = env->GetStringUTFChars(filesDir, nullptr);
    Preferences::setStorageDirectory(path);
    env->ReleaseStringUTFChars(filesDir, path);
    std::srand(static_cast<unsigned int>(std::time(nullptr)));
    g_pet = std::make_unique<Pet>();
    g_pet->begin();
    g_pet->syncClock(static_cast<uint32_t>(nowEpoch));
    g_lastClockMinute = static_cast<uint32_t>(nowEpoch) / 60;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tamapoke_android_PetNative_nativeEngineStatus(JNIEnv* env, jobject) {
    return env->NewStringUTF(g_pet ? "공통 C++ 엔진 연결 완료" : "엔진 초기화 필요");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tamapoke_android_PetNative_nativeGetState(JNIEnv* env, jobject) {
    if (!g_pet) return env->NewStringUTF("{}");
    char json[640];
    std::snprintf(json, sizeof(json),
        "{\"egg\":%s,\"speciesId\":%d,\"level\":%u,\"fullness\":%u,\"joy\":%u,\"energy\":%u,\"hygiene\":%u,\"poops\":%u,\"weight\":%u,\"ageMinutes\":%u,\"canEvolve\":%s,\"sleeping\":%s,\"shiny\":%s,\"starterPick\":%s}",
        g_pet->isEgg() ? "true" : "false", g_pet->speciesId, g_pet->level(),
        g_pet->fullness, g_pet->joy, g_pet->energy, g_pet->hygiene, g_pet->poops, g_pet->weight, g_pet->ageMinutes,
        g_pet->canEvolveNow() ? "true" : "false", g_pet->sleeping ? "true" : "false", g_pet->shiny ? "true" : "false",
        g_pet->awaitingStarter() ? "true" : "false");
    return env->NewStringUTF(json);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tamapoke_android_PetNative_nativeUpdate(JNIEnv*, jobject, jlong nowEpoch) {
    if (!g_pet) return;
    g_pet->update(millis());
    const uint32_t epoch = static_cast<uint32_t>(nowEpoch);
    const uint32_t minute = epoch / 60;
    if (epoch && minute != g_lastClockMinute) {
        g_pet->setClock(epoch);
        g_lastClockMinute = minute;
    }
    g_pet->flushSave();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tamapoke_android_PetNative_nativeGetDexState(JNIEnv* env, jobject) {
    if (!g_pet) return env->NewStringUTF("[]");
    std::string json;
    json.reserve(12000);
    json += "[";
    for (int dex = 1; dex <= DEX_COUNT; ++dex) {
        if (dex > 1) json += ",";
        char item[192];
        const char* name = DEX_NAME_KO[dex] ? DEX_NAME_KO[dex] : DEX_TBL[dex].name;
        std::snprintf(item, sizeof(item),
            "{\\"id\\":%d,\\"name\\":\\"%s\\",\\"registered\\":%s,\\"shiny\\":%s}",
            dex, name, g_pet->isRegistered(dex) ? "true" : "false",
            g_pet->isShinyRegistered(dex) ? "true" : "false");
        json += item;
    }
    json += "]";
    return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativeFeed(JNIEnv*, jobject) { if (g_pet) g_pet->feed(); }
extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativePlay(JNIEnv*, jobject) { if (g_pet) g_pet->play(); }
extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativeClean(JNIEnv*, jobject) { if (g_pet) g_pet->clean(); }
extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativeCaress(JNIEnv*, jobject) { if (g_pet) g_pet->caress(); }
extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativeEggTap(JNIEnv*, jobject) { if (g_pet) g_pet->eggTap(); }
extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativeToggleSleep(JNIEnv*, jobject) { if (g_pet) g_pet->toggleLight(); }
extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativeEvolve(JNIEnv*, jobject) { if (g_pet) g_pet->evolve(); }
extern "C" JNIEXPORT void JNICALL Java_com_tamapoke_android_PetNative_nativeChooseStarter(JNIEnv*, jobject, jint dex) { if (g_pet) g_pet->chooseStarter(static_cast<int16_t>(dex)); }
