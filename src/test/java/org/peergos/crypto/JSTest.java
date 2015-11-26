package org.peergos.crypto;

import org.junit.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

public class JSTest
{
    private static ScriptEngineManager engineManager = new ScriptEngineManager();
    public static final ScriptEngine engine = engineManager.getEngineByName("nashorn");
    public static final Invocable invocable = (Invocable) engine;

    public static Random prng = new Random(0); // only used in testing so let's make it deterministic
    public static byte[] getRandomValues(int len) {
        byte[] in = new byte[len];
        prng.nextBytes(in);
        return in;
    }

    static {
        try {
            engine.eval("var navigator = {}, window = {}; window.crypto = {};\n window.crypto.getRandomValues = " +
                    "function (arr){\n" +
                    "    var jarr = Java.type('org.peergos.crypto.JSTest').getRandomValues(arr.length);\n" +
                    "    for (var i=0; i < arr.length; i++) arr[i] = jarr[i];\n" +
                    "}\n" +
                    "" +
                    "function toByteArray(arr) {\n" +
                    "    var jarr = new (Java.type('byte[]'))(arr.length);" +
                    "    for (var i=0; i < jarr.length; i++) jarr[i] = arr[i];" +
                    "    return jarr;\n" +
                    "}\n" +
                    "" +
                    "function fromByteArray(arr) {\n" +
                    "    var res = new Uint8Array(arr.length);" +
                    "    for (var i=0; i < arr.length; i++) res[i] = arr[i];" +
                    "    return res;\n" +
                    "}\n" +
                    "" +
                    "function createNonce(){" +
                    "    return window.nacl.randomBytes(24);" +
                    "}" +
                    "" +
                    "function box(input, nonce, pubBox, secretBox) {" +
                    "    return window.nacl.box(input, nonce, pubBox, secretBox);" +
                    "}" +
                    "" +
                    "function unbox(cipher, nonce, pubBox, secretBox) {" +
                    "    return window.nacl.box.open(cipher, nonce, pubBox, secretBox);" +
                    "}" +
                    "" +
                    "function unsign(signature, publicSigningKey) {" +
                    "    return window.nacl.sign.open(signature, publicSigningKey);" +
                    "}" +
                    "" +
                    "function sign(message, secretSigningKey) {" +
                    "    return window.nacl.sign(message, secretSigningKey);" +
                    "}" +
                    "" +
                    "function sign_keypair(seed) {" +
                    "    var pk = new Uint8Array(32);\n" +
                    "    var sk = new Uint8Array(64);\n" +
                    "    for (var i = 0; i < 32; i++) sk[i] = seed[i];\n" +
                    "    window.nacl.lowlevel.crypto_sign_keypair(pk, sk, true);" +
                    "    var both = new Uint8Array(96);" +
                    "    for (var i = 0; i < 32; i++) both[i] = pk[i];" +
                    "    for (var i = 0; i < 64; i++) both[32+i] = sk[i];" +
                    "    return both;" +
                    "}");
            engine.eval(new InputStreamReader(JSTest.class.getClassLoader().getResourceAsStream("nacl.js")));
            engine.eval("Object.freeze(this);");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] signMessage(byte[] message, byte[] secretSigningKey)
    {
        byte[] res = null;
        try {
            res = (byte[]) invocable.invokeFunction("toByteArray", invocable.invokeFunction("sign",
                    invocable.invokeFunction("fromByteArray", message),
                    invocable.invokeFunction("fromByteArray", secretSigningKey)));
        } catch (Exception e) {e.printStackTrace();}
        return res;
    }

    public static byte[] decryptMessage(byte[] cipher, byte[] nonce, byte[] theirPublicBoxingKey, byte[] secretBoxingKey)
    {
        byte[] res = null;
        try {
            res = (byte[]) invocable.invokeFunction("toByteArray", invocable.invokeFunction("unbox",
                    invocable.invokeFunction("fromByteArray", cipher),
                    invocable.invokeFunction("fromByteArray", nonce),
                    invocable.invokeFunction("fromByteArray", theirPublicBoxingKey),
                    invocable.invokeFunction("fromByteArray", secretBoxingKey)));
        } catch (Exception e) {e.printStackTrace();}
        if (res.length == 0)
            throw new TweetNaCl.InvalidCipherTextException();
        return res;
    }

    public static byte[] encryptMessageFor(byte[] input, byte[] nonce, byte[] publicBoxingKey, byte[] ourSecretBoxingKey)
    {
        byte[] paddedMessage = new byte[32 + input.length];
        System.arraycopy(input, 0, paddedMessage, 32, input.length);
        try {
            return (byte[]) invocable.invokeFunction("toByteArray", invocable.invokeFunction("box",
                    invocable.invokeFunction("fromByteArray", input),
                    invocable.invokeFunction("fromByteArray", nonce),
                    invocable.invokeFunction("fromByteArray", publicBoxingKey),
                    invocable.invokeFunction("fromByteArray", ourSecretBoxingKey)));
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    public static byte[] createNonce() {
        try {
            Object nonce = invocable.invokeFunction("createNonce");
            return (byte[]) invocable.invokeFunction("toByteArray", nonce);
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    public static String bytesToHex(byte[] data)
    {
        StringBuilder s = new StringBuilder();
        for (byte b : data)
            s.append(String.format("%02x", b & 0xFF));
        return s.toString();
    }

    public static byte[] unsignMessage(byte[] signed, byte[] publicSigningKey)
    {
        try {
            Object res = invocable.invokeFunction("unsign",
                    invocable.invokeFunction("fromByteArray", signed),
                    invocable.invokeFunction("fromByteArray", publicSigningKey));
            if (res == null)
                throw new TweetNaCl.InvalidSignatureException();
            return (byte[]) invocable.invokeFunction("toByteArray", res);
        } catch (ScriptException | NoSuchMethodException e) {throw new RuntimeException(e);}
    }

    public static final int NUMBER_OF_RANDOM_KEYPAIRS = 10;
    public static final int MESSAGE_SIZE = 128;
    @Test
    public  void testAll() throws Exception
    {

        prng.setSeed(System.currentTimeMillis());

        for (int i = 0; i < NUMBER_OF_RANDOM_KEYPAIRS; i++)
        {
            byte[] privateBoxingKey = new byte[32];
            byte[] secretBoxingKey = new byte[32];
            prng.nextBytes(secretBoxingKey);
            TweetNaCl.crypto_box_keypair(privateBoxingKey, secretBoxingKey, true);

            byte[] message = new byte[MESSAGE_SIZE];
            prng.nextBytes(message);

            // box
            byte[] nonce = JSTest.createNonce();
            byte[] cipher = encryptMessageFor(message, nonce, privateBoxingKey, secretBoxingKey);
            byte[] cipher2 = TweetNaCl.crypto_box(message, nonce, privateBoxingKey, secretBoxingKey);

            assertArrayEquals("Different ciphertexts with same nonce!: " + bytesToHex(cipher) + " != " + bytesToHex(cipher2),
                    cipher, cipher2);

            // unbox
            byte[] clear = TweetNaCl.crypto_box_open(cipher, nonce, privateBoxingKey, secretBoxingKey);

            assertArrayEquals("JS -> J, Decrypted message != original: " + new String(clear) + " != " + new String(message),
                    clear, message);

            byte[] clear2 = decryptMessage(cipher2, nonce, privateBoxingKey, secretBoxingKey);

            assertArrayEquals("J -> JS, Decrypted message != original: " + new String(clear2) + " != " + new String(message),
                    clear2, message);

            // unbox with error
            try
            {
                byte[] ciphererr = Arrays.copyOf(cipher, cipher.length);
                ciphererr[0] = (byte) ~ciphererr[0];
                TweetNaCl.crypto_box_open(ciphererr, nonce, privateBoxingKey, secretBoxingKey);
                fail("J, Decrypting bad cipher text didn't fail!");

            }
            catch (TweetNaCl.InvalidCipherTextException ignored)
            {

            }

            try
            {
                byte[] cipher2err = Arrays.copyOf(cipher2, cipher2.length);
                cipher2err[0] = (byte) ~cipher2err[0];
                decryptMessage(cipher2err, nonce, privateBoxingKey, secretBoxingKey);
                fail("JS, Decrypting bad cipher text didn't fail!");

            }
            catch (TweetNaCl.InvalidCipherTextException ignored)
            {
            }

            // sign keygen
            byte[] privateSigningKey = new byte[32];
            byte[] secretSigningKey = new byte[64];
            byte[] signSeed = new byte[32];
            prng.nextBytes(signSeed);
            System.arraycopy(signSeed, 0, secretSigningKey, 0, 32);
            TweetNaCl.crypto_sign_keypair(privateSigningKey, secretSigningKey, true);
            byte[] jsSignPair = (byte[]) invocable.invokeFunction("toByteArray",
                    invocable.invokeFunction("sign_keypair", invocable.invokeFunction("fromByteArray", signSeed)));
            byte[] jsSecretSignKey = Arrays.copyOfRange(jsSignPair, 32, 96);
            byte[] jsprivateSignKey = Arrays.copyOfRange(jsSignPair, 0, 32);

            assertArrayEquals("Signing key generation invalid, different secret keys!", secretSigningKey, jsSecretSignKey);

            assertArrayEquals("Signing key generation invalid, different private keys!", privateSigningKey, jsprivateSignKey);

            assertArrayEquals("Signing private key != second half of secret key!", privateSigningKey,
                    Arrays.copyOfRange(secretSigningKey, 32, 64));

            // sign
            byte[] sig = TweetNaCl.crypto_sign(message, secretSigningKey);
            byte[] sig2 = signMessage(message, secretSigningKey);

            assertArrayEquals("Signatures not equal! " + bytesToHex(sig) + " != " + bytesToHex(sig2), sig, sig2);

            // unsign
            {
                byte[] unsigned = TweetNaCl.crypto_sign_open(sig, privateSigningKey);
                assertArrayEquals("J (J sig): Unsigned message != original! " + bytesToHex(sig) + " != " + bytesToHex(sig2),
                        unsigned, message);
            }

            {
                byte[] unsigned = TweetNaCl.crypto_sign_open(sig2, privateSigningKey);
                assertArrayEquals("J (JS sig): Unsigned message != original! " + bytesToHex(sig) + " != " + bytesToHex(sig2),
                        unsigned, message);

            }
            {
                byte[] unsigned2 = unsignMessage(sig, privateSigningKey);
                assertArrayEquals("J (JS sig): Unsigned message != original! " + bytesToHex(sig) + " != " + bytesToHex(sig2),
                        unsigned2, message);
            }
            {
                byte[] unsigned2 = unsignMessage(sig2, privateSigningKey);
                assertArrayEquals("J (JS sig): Unsigned message != original! " + bytesToHex(sig) + " != " + bytesToHex(sig2),
                        unsigned2, message);
            }

            // unsign with error
            byte[] sigerr = Arrays.copyOf(sig, sig.length);
            sigerr[0] = (byte) (~sigerr[0]);
            try
            {
                byte[] unsignederr = TweetNaCl.crypto_sign_open(sigerr, privateSigningKey);
                fail("J: invalid unsign didn't fail!");

            }
            catch (TweetNaCl.InvalidSignatureException ignored)
            {
            }
            try
            {
                byte[] unsigned2err = unsignMessage(sigerr, privateSigningKey);
                fail("J: invalid unsign didn't fail!");

            }
            catch (TweetNaCl.InvalidSignatureException ignored)
            {
            }

        }
    }
}
