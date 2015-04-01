package org.peergos.crypto;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.*;

public class NativeTweetNacl {
    static {
        System.loadLibrary("tweetnacl");
    }

    public static native int ld32(byte[] b);


    public static void main(String[] args) {
        NativeTweetNacl nacl = new NativeTweetNacl();
        Random random = new Random();

        byte[] b = new byte[16];
        random.nextBytes(b);

        int jni = nacl.ld32(b);
        int java = TweetNaCl.ld32(b, 0);
        
        System.out.println(jni + ", "+ java);
    }
}
