#include <jni.h>
#include <stdio.h>
#include "devurandom.c"
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


JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_crypto_1box_1keypair
  (JNIEnv * env, jclass class , jbyteArray jy, jbyteArray jx) {

        jsize jy_length = (*env)->GetArrayLength(env, jy);
        u8 y[jy_length];
        (*env)->GetByteArrayRegion(env, jy ,0, jy_length, y);

        jsize jx_length = (*env)->GetArrayLength(env, jx);
        u8 x[jx_length];
        (*env)->GetByteArrayRegion(env, jx ,0, jx_length, x);

        return crypto_box_keypair(y,x); 
  }

