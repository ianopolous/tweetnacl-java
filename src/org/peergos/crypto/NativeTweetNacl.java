package org.peergos.crypto;

import java.util.Random;
import java.util.*;

public class NativeTweetNacl {
    static {
        System.loadLibrary("tweetnacl");
    }

    public static native int crypto_box_keypair(byte[] y, byte[] x);

    public static native int crypto_box_open(byte[] m, byte[]c, long d, byte[] b, byte[] y, byte[] x);

    public static native int crypto_box(byte[] c, byte[] m, long d, byte[] n, byte[] y, byte[] x);


    public static native int crypto_sign_open(byte[] message, long[] mlen, byte[] secretKey, long n, byte[] publicKey);

    public static native int crypto_sign(byte[] sm, long[] smlen, byte[] m, long n, byte[] sk);

    public static native int crypto_sign_keypair(byte[] pk, byte[] sk);


    public static native int crypto_scalarmult_base(byte[] q, byte[] n);

    public static native int ld32(byte[] b);


    public static void main(String[] args) {
        NativeTweetNacl jni = new NativeTweetNacl();

    }
}
