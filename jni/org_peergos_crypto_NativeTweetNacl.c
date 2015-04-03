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
(JNIEnv * env, jclass class , jbyteArray publicKey, jbyteArray secretKey) {

        u8 pk[crypto_box_PUBLICKEYBYTES];
        u8 sk[crypto_box_SECRETKEYBYTES];

        int rc = crypto_box_keypair(pk,sk);

        (*env)->SetByteArrayRegion(env,publicKey,0,crypto_box_PUBLICKEYBYTES,pk);
        (*env)->SetByteArrayRegion(env,secretKey,0,crypto_box_SECRETKEYBYTES,sk);
        return (jint) rc;
}


JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_crypto_1scalarmult_1base
(JNIEnv * env, jclass class , jbyteArray qin, jbyteArray nin) {

        u8 n[crypto_scalarmult_SCALARBYTES];
        u8 q[crypto_scalarmult_BYTES];
        (*env)->GetByteArrayRegion(env, nin, 0, crypto_scalarmult_SCALARBYTES, n);
        int rc = crypto_scalarmult_base(q,n);
        (*env)->SetByteArrayRegion(env, qin, 0, crypto_scalarmult_BYTES, q);
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_crypto_1sign_1open
(JNIEnv *, jclass, jbyteArray, jlongArray, jbyteArray, jlong, jbyteArray);

/*
 * Class:     org_peergos_crypto_NativeTweetNacl
 * Method:    crypto_sign
 * Signature: ([B[J[BJ[B)I
 */
JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_crypto_1sign
(JNIEnv *, jclass, jbyteArray, jlongArray, jbyteArray, jlong, jbyteArray);

/*
 * Class:     org_peergos_crypto_NativeTweetNacl
 * Method:    crypto_sign_keypair
 * Signature: ([B[B)I
 */
JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_crypto_1sign_1keypair
(JNIEnv *, jclass, jbyteArray, jbyteArray);

/*
 * Class:     org_peergos_crypto_NativeTweetNacl
 * Method:    crypto_box_open
 * Signature: ([B[BJ[B[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_crypto_1box_1open
(JNIEnv *, jclass, jbyteArray, jbyteArray, jlong, jbyteArray, jbyteArray, jbyteArray);

/*
 * Class:     org_peergos_crypto_NativeTweetNacl
 * Method:    crypto_box
 * Signature: ([B[BJ[B[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_peergos_crypto_NativeTweetNacl_crypto_1box
(JNIEnv *, jclass, jbyteArray, jbyteArray, jlong, jbyteArray, jbyteArray, jbyteArray);
