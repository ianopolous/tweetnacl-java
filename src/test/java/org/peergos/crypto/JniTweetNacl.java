package org.peergos.crypto;

public class JniTweetNacl {
    static {
        System.loadLibrary("tweetnacl");
    }

    public static native int crypto_box_keypair(byte[] y, byte[] x);

    public static native int crypto_box_open(byte[] m, byte[]c, long d, byte[] b, byte[] y, byte[] x);

    public static native int crypto_box(byte[] c, byte[] m, long d, byte[] n, byte[] y, byte[] x);


    public static native int crypto_sign_open(byte[] m, long mlen, byte[] sm, long n, byte[] pk);

    public static native int crypto_sign(byte[] sm, long smlen, byte[] m, long n, byte[] sk);

    public static native int crypto_sign_keypair(byte[] pk, byte[] sk);


    public static native int crypto_scalarmult_base(byte[] q, byte[] n);

    public static native int ld32(byte[] b);

}
