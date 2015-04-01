#include <jni.h>
#include <stdio.h>
#include "tweetnacl.c"
#include "org_peergos_crypto_NativeTweetNacl.h"

typedef unsigned char u8;
typedef unsigned long u32;
typedef unsigned long long u64;
typedef long long i64;
typedef i64 gf[16];

JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_ld32
(JNIEnv * env, jclass class, jbyteArray param){
        jsize length = (*env)->GetArrayLength(env, param);
        u8 array[length];
        (*env)->GetByteArrayRegion(env, param ,0, length, array);
        return ld32(array);
}
