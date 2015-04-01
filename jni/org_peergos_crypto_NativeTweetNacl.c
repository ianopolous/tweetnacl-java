#include <jni.h>
#include <stdio.h>
#include "org_peergos_crypto_NativeTweetNacl.h"

JNIEXPORT jlong JNICALL Java_org_peergos_crypto_NativeTweetNacl_ld32
  (JNIEnv * env, jclass class, jintArray param){
         long x = 12;
         return x; 
  }
