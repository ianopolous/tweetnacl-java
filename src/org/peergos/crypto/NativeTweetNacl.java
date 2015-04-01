package org.peergos.crypto;

import java.util.Random;
import java.util.*;

public class NativeTweetNacl {
    static {
        System.loadLibrary("tweetnacl");
    }

    public static native int ld32(byte[] b);

    public static native int crypto_box_keypair(byte[] y, byte[] x);

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

//      System.out.println("java "+ javaResult +" jni "+ jniResult + "y "+ Arrays.equals(java_y, jni_y) +", x " + Arrays.equals(java_x, jni_x));

    }
}
