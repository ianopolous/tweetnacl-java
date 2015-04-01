package org.peergos.crypto;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.*;

public class NativeTweetNacl {
    static {
        System.loadLibrary("tweetnacl.2");
    }

    public static native long ld32(int[] b);


    public static void main(String[] args) {
        NativeTweetNacl nacl = new NativeTweetNacl();
        Random random = new Random();

//        char[] b = new char[8];
        int[] b = new int[8];

        for (int i=0; i < b.length; i++) {
            b[i] = (char) random.nextInt();
            System.out.print(b[i] +",");
        }

        System.out.println();

        long x = nacl.ld32(b);
        System.out.println(x);


    }
}
