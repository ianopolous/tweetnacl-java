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
(JNIEnv * env, jclass class , jbyteArray publicKey, jbyteArray privateKey) {

        u8 pk[crypto_box_PUBLICKEYBYTES];
        u8 sk[crypto_box_SECRETKEYBYTES];

        int rc = crypto_box_keypair(pk,sk);

        (*env)->SetByteArrayRegion(env,publicKey,0,crypto_box_PUBLICKEYBYTES,pk);
        (*env)->SetByteArrayRegion(env,secretKey,0,crypto_box_SECRETKEYBYTES,sk);
        return rc;
}
}

