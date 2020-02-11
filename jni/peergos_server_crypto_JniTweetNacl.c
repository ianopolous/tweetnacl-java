#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include "devurandom.c"
#include "tweetnacl.c"
#include "peergos_server_crypto_JniTweetNacl.h"

typedef unsigned char u8;
typedef unsigned long u32;
typedef unsigned long long u64;
typedef long long i64;
typedef i64 gf[16];

int length(JNIEnv * env, jbyteArray array) {
        return (int) (*env)->GetArrayLength(env, array);
}

u8* toArray(JNIEnv * env, jbyteArray array) {
        int len = length(env,array);
        u8 *array_c = (u8*) malloc(len * 8);
        (*env)->GetByteArrayRegion(env, array, 0, len, array_c);
        return array_c;

}

void copy(JNIEnv * env, u8* from, jbyteArray to, int offset, int length) {
    (*env)->SetByteArrayRegion(env, to, offset, length, from);
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_ld32
(JNIEnv * env, jclass class, jbyteArray param){
        jsize length = (*env)->GetArrayLength(env, param);
        u8 array[length];
        (*env)->GetByteArrayRegion(env, param ,0, length, array);

        return ld32(array);
}


JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1box_1keypair
(JNIEnv * env, jclass class , jbyteArray publicKey, jbyteArray secretKey) {

        u8 pk[crypto_box_PUBLICKEYBYTES];
        u8 sk[crypto_box_SECRETKEYBYTES];

        int rc = crypto_box_keypair(pk,sk);

        (*env)->SetByteArrayRegion(env,publicKey,0,crypto_box_PUBLICKEYBYTES,pk);
        (*env)->SetByteArrayRegion(env,secretKey,0,crypto_box_SECRETKEYBYTES,sk);
        return (jint) rc;
}


JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1scalarmult_1base
(JNIEnv * env, jclass class , jbyteArray qin, jbyteArray nin) {

        u8 n[crypto_scalarmult_SCALARBYTES];
        u8 q[crypto_scalarmult_BYTES];
        (*env)->GetByteArrayRegion(env, nin, 0, crypto_scalarmult_SCALARBYTES, n);
        int rc = crypto_scalarmult_base(q,n);
        (*env)->SetByteArrayRegion(env, qin, 0, crypto_scalarmult_BYTES, q);
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1sign_1open
(JNIEnv * env, jclass class, jbyteArray message, jlong mlen,  jbyteArray sm, jlong n, jbyteArray publicKey) {
        
        u8* message_c = toArray(env, message); 
        u64 mlen_c= (u64) mlen;
        u8* sm_c = toArray(env, sm); 
        u8* publicKey_c = toArray(env, publicKey); 
        int rc = crypto_sign_open(message_c, &mlen_c, sm_c, n, publicKey_c);
        
        copy(env, message_c, message, 0, length(env, message));

        free(message_c);
        free(sm_c);
        free(publicKey_c);
        
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1sign
(JNIEnv * env, jclass class, jbyteArray sm, jlong smlen, jbyteArray m, jlong n, jbyteArray sk) {
        u8* sm_c = toArray(env, sm);
        u64 smlen_c= (u64) smlen;
        u8* m_c = toArray(env, m);
        u8* sk_c = toArray(env, sk);

        int rc = crypto_sign(sm_c, &smlen_c, m_c, (long) n, sk_c);
        copy(env, sm_c, sm, 0, length(env, sm));

        free(sm_c);
        free(m_c);
        free(sk_c);
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1sign_1keypair
(JNIEnv * env, jclass class, jbyteArray publicKey, jbyteArray signingKey) {

        u8* publicKey_c = toArray(env,publicKey);
        u8* signingKey_c = toArray(env,signingKey);

        int rc = crypto_sign_keypair(publicKey_c, signingKey_c);
        copy(env, signingKey_c, signingKey, 0, length(env, signingKey));
        copy(env, publicKey_c, publicKey, 0, length(env, publicKey));

        free(publicKey_c);
        free(signingKey_c);
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1box_1open
(JNIEnv * env, jclass class, jbyteArray message, jbyteArray cipher, jlong d, jbyteArray n, jbyteArray y, jbyteArray x) {

        u8* message_c = toArray(env,message);
        u8* cipher_c = toArray(env,cipher);
        u8* n_c = toArray(env,n);
        u8* y_c = toArray(env,y);
        u8* x_c = toArray(env,x);
        
        int rc = crypto_box_open(message_c, cipher_c, d, n_c, y_c, x_c);
        copy(env, message_c, message, 0, length(env, message)); 
        free(message_c);
        free(cipher_c);
        free(n_c);
        free(y_c);
        free(x_c);
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1secretbox
(JNIEnv * env, jclass class, jbyteArray c, jbyteArray m, jlong d, jbyteArray n, jbyteArray k) {
        u8* c_c = toArray(env, c);
        u8* m_c = toArray(env, m);
        u8* n_c = toArray(env, n);
	u8* k_c = toArray(env, k);

        int rc = crypto_secretbox(c_c, m_c, (long) d, n_c, k_c);
        copy(env, c_c, c, 0, length(env, c));

        free(c_c);
        free(m_c);
	free(n_c);
        free(k_c);
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1secretbox_1open
(JNIEnv * env, jclass class, jbyteArray m, jbyteArray c, jlong d, jbyteArray n, jbyteArray k) {
        u8* m_c = toArray(env, m);
        u8* c_c = toArray(env, c);
        u8* n_c = toArray(env, n);
	u8* k_c = toArray(env, k);

        int rc = crypto_secretbox_open(m_c, c_c, (long) d, n_c, k_c);
        copy(env, m_c, m, 0, length(env, m));

        free(c_c);
        free(m_c);
	free(n_c);
        free(k_c);
        return (jint) rc;
}

JNIEXPORT jint JNICALL Java_peergos_server_crypto_JniTweetNacl_crypto_1box
(JNIEnv * env, jclass class, jbyteArray c, jbyteArray m, jlong d, jbyteArray n, jbyteArray y, jbyteArray x) {

        u8* c_c = toArray(env,c);
        u8* m_c = toArray(env,m);
        u8* n_c = toArray(env,n);
        u8* y_c = toArray(env,y);
        u8* x_c = toArray(env,x);
          
        int rc = crypto_box(c_c, m_c, d, n_c, y_c, x_c);
        copy(env, c_c, c, 0, length(env, c)); 
        
        free(m_c);
        free(c_c);
        free(n_c);
        free(y_c);
        free(x_c);

        return (jint) rc;
}
