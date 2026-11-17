#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tamapoke_android_PetNative_engineStatus(JNIEnv* env, jobject /* thiz */) {
    return env->NewStringUTF("공통 C++ 엔진 연결 전");
}
