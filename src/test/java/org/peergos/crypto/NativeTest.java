package org.peergos.crypto;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class NativeTest {
    private static final int SECRETBOX_INTERNAL_OVERHEAD_BYTES = 32;
    private static Random prng = new Random(0);

    public NativeTest(){}

    private static byte[] nonce() {
        byte[] nonce = new byte[TweetNaCl.BOX_NONCE_BYTES];
        prng.nextBytes(nonce);
        return nonce;
    }
    private static byte[] copy(byte[] of) {
        if (of == null)
            throw new IllegalStateException("Array to be copied must not be null.");
        return Arrays.copyOf(of, of.length);
    }

    private static void cryptoSignOpenTest(int nRound, int messageLength) {
        byte[] publicKey = new byte[32];
        byte[] secretKey = new byte[64];

        byte[] message = new byte[messageLength];

        for (int iRound = 0; iRound < nRound; iRound++) {
            prng.nextBytes(message);

            boolean isSeeded = true;
            prng.nextBytes(secretKey);
            TweetNaCl.crypto_sign_keypair(publicKey, secretKey, isSeeded);
//                JniTweetNacl.crypto_sign_keypair(publicKey, secretKey);

            byte[] signedText = TweetNaCl.crypto_sign(message, secretKey);

            int signedLength = signedText.length;
            byte[] jniMessage = new byte[signedLength];

            byte[] javaMessage = TweetNaCl.crypto_sign_open(copy(signedText), publicKey);

            int jniRc = JniTweetNacl.crypto_sign_open(jniMessage, message.length, copy(signedText), signedLength, copy(publicKey));
            jniMessage = Arrays.copyOfRange(jniMessage, 0, jniMessage.length - 64);

            if (jniRc != 0)
                throw new IllegalStateException("non-zero jni return code " + jniRc);

            if (!Arrays.equals(javaMessage, message))
                throw new IllegalStateException("Java unsign != original!");

            if (!Arrays.equals(jniMessage, message))
                throw new IllegalStateException("JNI unsign != original!");

            boolean test = Arrays.equals(jniMessage, javaMessage);
            for (int i=0; i < javaMessage.length; i++)
                if (javaMessage[i] != jniMessage[i])
                    System.out.println("pos " + i + " : java " + javaMessage[i] + " vs jni " + jniMessage[i]);
            assertTrue("crypto sign-open round " + iRound +", message length "+ messageLength, test);
        }
    }


    private static void cryptoSignTest(int nRound, int messageLength) {
        byte[] publicSigningKey = new byte[32];
        byte[] secretSigningKey = new byte[64];

        byte[] message = new byte[messageLength];

        int signedLength = message.length + TweetNaCl.CRYPTO_SIGN_BYTES;

        byte[] jniSignedMessage = new byte[signedLength];

        for (int iRound =0; iRound < nRound; iRound++) {
            boolean isSeeded = true;
            prng.nextBytes(secretSigningKey);
            TweetNaCl.crypto_sign_keypair(publicSigningKey, secretSigningKey, isSeeded);

            prng.nextBytes(message);
            byte[] javaSignedMessage = TweetNaCl.crypto_sign(message, secretSigningKey);
            int jniRc = JniTweetNacl.crypto_sign(jniSignedMessage, signedLength, copy(message), message.length, secretSigningKey);

            if (jniRc != 0)
                throw new IllegalStateException("non-zero jni return code " + jniRc);


            boolean test = Arrays.equals(javaSignedMessage, jniSignedMessage);
            assertTrue("crypto-sign round " + iRound +" message length ", test);
        }
    }



    private static boolean cryptoBoxOpenTest(byte[] publicKey, byte[] secretKey, byte[] cipher, byte[] nonce) {

        byte[] paddedCipher = new byte[cipher.length + 16];
        System.arraycopy(cipher, 0, paddedCipher, 16, cipher.length);

        byte[] javaRawText = TweetNaCl.crypto_box_open(cipher, nonce, publicKey, secretKey);

        byte[] jniRawText = new byte[paddedCipher.length];
        int jniRc = JniTweetNacl.crypto_box_open(jniRawText, copy(paddedCipher), paddedCipher.length, copy(nonce), copy(publicKey), copy(secretKey));

        if (jniRc != 0)
            throw new IllegalStateException("non-zero jni return code " + jniRc);

        jniRawText = Arrays.copyOfRange(jniRawText, 32, jniRawText.length);

        return Arrays.equals(javaRawText, jniRawText);
    }

    private static boolean cryptoBoxTest(byte[] publicKey, byte[] secretKey, byte[] message) {
        byte[] nonce = nonce();
        byte[] cipherText = new byte[SECRETBOX_INTERNAL_OVERHEAD_BYTES + message.length];
        byte[] paddedMessage = new byte[SECRETBOX_INTERNAL_OVERHEAD_BYTES + message.length];
        System.arraycopy(message, 0, paddedMessage, SECRETBOX_INTERNAL_OVERHEAD_BYTES, message.length);

        return cryptoBoxTest(cipherText, paddedMessage, paddedMessage.length, nonce, publicKey, secretKey);
    }

    private static boolean cryptoBoxTest(byte[] cipherText, byte[] paddedMessage, long paddedMessageLength, byte[] nonce, byte[] theirPublicBoxingKey, byte[] ourSecretBoxingKey) {

        byte[] javaCipherText = TweetNaCl.crypto_box(Arrays.copyOfRange(paddedMessage, SECRETBOX_INTERNAL_OVERHEAD_BYTES, paddedMessage.length),
                nonce, theirPublicBoxingKey, ourSecretBoxingKey);

        byte[] jniCipherText = copy(cipherText);
        long jniMessageLength = paddedMessageLength;

        int jniRc = JniTweetNacl.crypto_box(jniCipherText, copy(paddedMessage), jniMessageLength, copy(nonce), copy(theirPublicBoxingKey), copy(ourSecretBoxingKey));

        if (jniRc != 0)
            throw new IllegalStateException("non-zero jni return code");

        jniCipherText = Arrays.copyOfRange(jniCipherText, 16, jniCipherText.length);

        return Arrays.equals(javaCipherText, jniCipherText);
    }

    private static void cryptoBoxTest(int nRound, int messageLength) {
        byte[] message = new byte[messageLength];
        byte[] publicBoxingKey = new byte[TweetNaCl.BOX_PUBLIC_KEY_BYTES];
        byte[] secretBoxingKey = new byte[TweetNaCl.BOX_SECRET_KEY_BYTES];

        for (int iRound=0; iRound < nRound; iRound++) {
            boolean isSeeded = true;
            prng.nextBytes(secretBoxingKey);
            TweetNaCl.crypto_box_keypair(publicBoxingKey, secretBoxingKey, isSeeded);
            prng.nextBytes(message);
            boolean test = cryptoBoxTest(publicBoxingKey, secretBoxingKey, message);
            assertTrue("crypto-box round "+ iRound +" with message length "+ messageLength, test);
        }
    }

    private static void cryptoBoxOpenTest(int nRound, int messageLength) {
        byte[] message = new byte[messageLength];
        byte[] publicBoxingKey = new byte[TweetNaCl.BOX_PUBLIC_KEY_BYTES];
        byte[] secretBoxingKey = new byte[TweetNaCl.BOX_SECRET_KEY_BYTES];

        for (int iRound=0; iRound < nRound; iRound++) {
            boolean isSeeded = true;
            prng.nextBytes(secretBoxingKey);
            TweetNaCl.crypto_box_keypair(publicBoxingKey, secretBoxingKey, isSeeded);
            byte[] nonce = nonce();
            byte[] cipher = TweetNaCl.crypto_box(message, copy(nonce), copy(publicBoxingKey), copy(secretBoxingKey));

            boolean test = cryptoBoxOpenTest(publicBoxingKey, secretBoxingKey, cipher, nonce);
            assertTrue("crypto-box-open round "+ iRound +" with message length "+ messageLength, test);
        }
    }


    public static int N_ROUND = 10;
    public static int MIN_SIZE_EXP = 5;
    public static int MAX_SIZE_EXP = 24;

    @org.junit.Test public void all() {

        for (int exp = MIN_SIZE_EXP; exp < MAX_SIZE_EXP; exp++) {
            int size = (int) Math.pow(2, exp);
            size += prng.nextInt(size);

            cryptoBoxTest(N_ROUND, size);
            cryptoBoxOpenTest(N_ROUND, size);
            cryptoSignTest(N_ROUND, size);
            cryptoSignOpenTest(N_ROUND, size);
        }
    }
}
