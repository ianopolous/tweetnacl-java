package org.peergos.crypto;

import java.util.Random;
import java.util.*;

public class NativeTweetNacl {
    static {
        System.loadLibrary("tweetnacl");
    }

//  int crypto_box(u8 *c,const u8 *m,u64 d,const u8 *n,const u8 *y,const u8 *x)
//  int crypto_box_open(u8 *m,const u8 *c,u64 d,const u8 *n,const u8 *y,const u8 *x)
//  int crypto_sign_keypair(u8 *pk, u8 *sk)
//  int crypto_sign(u8 *sm,u64 *smlen,const u8 *m,u64 n,const u8 *sk)
//  int crypto_sign_open(u8 *m,u64 *mlen,const u8 *sm,u64 n,const u8 *pk)
//  int crypto_scalarmult_base(u8 *q,const u8 *n)

    public static native int ld32(byte[] b);

    public static native int crypto_box_keypair(byte[] y, byte[] x);

    public static native int crypto_scalarmult_base(byte[] q, byte[] n);

    public static native int crypto_sign_open(byte[] m, long[] mlen, byte[] sm, long n, byte[] pk);

    public static native int crypto_sign(byte[] sm, long[] smlen, byte[] m, long n, byte[] sk);

    public static native int crypto_sign_keypair(byte[] pk, byte[] sk);

    public static native int crypto_box_open(byte[] m, byte[]c, long d, byte[] b, byte[] y, byte[] x);

    public static native int crypto_box(byte[] c, byte[] m, long d, byte[] n, byte[] y, byte[] x);


    public static void main(String[] args) {
        NativeTweetNacl jni = new NativeTweetNacl();
        Random random = new Random();

        byte[] b = new byte[16];
        random.nextBytes(b);

//        int jni = jni.ld32(b);
//        int java = TweetNaCl.ld32(b, 0);
//        System.out.println(jni + ", "+ java);

        int length = 32;

        byte[] java_y = new byte[length], java_x = new byte[length];
        boolean isSeeded = true;
        int javaResult = TweetNaCl.crypto_box_keypair(java_y, java_x, isSeeded);

        byte[] jni_y = new byte[length], jni_x = new byte[length];
        int jniResult = NativeTweetNacl.crypto_box_keypair(jni_y, jni_x);

      System.out.println("java "+ javaResult +" jni "+ jniResult + "y "+ Arrays.equals(java_y, jni_y) +", x " + Arrays.equals(java_x, jni_x));

    }
}
