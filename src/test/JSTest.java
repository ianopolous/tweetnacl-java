package test;

import peergos.server.crypto.TweetNaCl;

import javax.script.*;
import java.io.InputStreamReader;
import java.util.*;

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
                    "    var jarr = Java.type('test.JSTest').getRandomValues(arr.length);\n" +
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
            engine.eval(new InputStreamReader(JSTest.class.getClassLoader().getResourceAsStream("lib/nacl.js")));
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

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Run with:");
            System.out.println("         java -jar Test.jar $k $n [-random]");
            System.out.println("Where $k is the size in KiB of the message, and $n is the number of random keypairs to try. -random randomises the PRNG");
            return;
        }
        if (args.length > 2 && args[2].equals("-random"))
            prng.setSeed(System.currentTimeMillis());
        int n = Integer.parseInt(args[1]);
        int max = Integer.parseInt(args[0])*1024;
        for (int i=0; i < n; i++) {
            byte[] publicBoxingKey = new byte[32];
            byte[] secretBoxingKey = new byte[32];
            prng.nextBytes(secretBoxingKey);
            TweetNaCl.crypto_box_keypair(publicBoxingKey, secretBoxingKey, true);

            byte[] message = new byte[max];
            prng.nextBytes(message);

            // box
            byte[] nonce = JSTest.createNonce();
            byte[] cipher = encryptMessageFor(message, nonce, publicBoxingKey, secretBoxingKey);
            byte[] cipher2 = TweetNaCl.crypto_box(message, nonce, publicBoxingKey, secretBoxingKey);
            if (!Arrays.equals(cipher, cipher2)) {
                throw new IllegalStateException("Different ciphertexts with same nonce!: " + bytesToHex(cipher) + " != " + bytesToHex(cipher2));
            }
            if (n == 1)
                System.out.println("Passed box test.");

            // unbox
            byte[] clear = TweetNaCl.crypto_box_open(cipher, nonce, publicBoxingKey, secretBoxingKey);
            if (!Arrays.equals(clear, message)) {
                throw new IllegalStateException("JS -> J, Decrypted message != original: " + new String(clear) + " != " + new String(message));
            }
            byte[] clear2 = decryptMessage(cipher2, nonce, publicBoxingKey, secretBoxingKey);
            if (!Arrays.equals(clear2, message))
                throw new IllegalStateException("J -> JS, Decrypted message != original: " + new String(clear2) + " != " + new String(message));
            if (n == 1)
                System.out.println("Passed unbox test.");

            // unbox with error
            try {
                byte[] ciphererr = Arrays.copyOf(cipher, cipher.length);
                ciphererr[0] = (byte)~ciphererr[0];
                byte[] clearerr = TweetNaCl.crypto_box_open(ciphererr, nonce, publicBoxingKey, secretBoxingKey);
                throw new IllegalStateException("J, Decrypting bad cipher text didn't fail!");
            } catch (TweetNaCl.InvalidCipherTextException e) {}
            try {
                byte[] cipher2err = Arrays.copyOf(cipher2, cipher2.length);
                cipher2err[0] = (byte)~cipher2err[0];
                byte[] clear2err = decryptMessage(cipher2err, nonce, publicBoxingKey, secretBoxingKey);
                throw new IllegalStateException("JS, Decrypting bad cipher text didn't fail!");
            } catch (TweetNaCl.InvalidCipherTextException e) {}
            if (n == 1)
                System.out.println("Passed unbox with error test.");

            // sign keygen
            byte[] publicSigningKey = new byte[32];
            byte[] secretSigningKey = new byte[64];
            byte[] signSeed = new byte[32];
            prng.nextBytes(signSeed);
            System.arraycopy(signSeed, 0, secretSigningKey, 0, 32);
            TweetNaCl.crypto_sign_keypair(publicSigningKey, secretSigningKey, true);
            byte[] jsSignPair = (byte[]) invocable.invokeFunction("toByteArray", invocable.invokeFunction("sign_keypair",
                    invocable.invokeFunction("fromByteArray", signSeed)));
            byte[] jsSecretSignKey = Arrays.copyOfRange(jsSignPair, 32, 96);
            byte[] jsPublicSignKey = Arrays.copyOfRange(jsSignPair, 0, 32);
            if (!Arrays.equals(secretSigningKey, jsSecretSignKey))
                throw new IllegalStateException("Signing key generation invalid, different secret keys!");
            if (!Arrays.equals(publicSigningKey, jsPublicSignKey))
                throw new IllegalStateException("Signing key generation invalid, different public keys!");
            if (!Arrays.equals(publicSigningKey, Arrays.copyOfRange(secretSigningKey, 32, 64)))
                throw new IllegalStateException("Signing public key != second half of secret key!");
            if (n == 1)
                System.out.println("Passed sign keygen tests.");

            // sign
            byte[] sig = TweetNaCl.crypto_sign(message, secretSigningKey);
            byte[] sig2 = signMessage(message, secretSigningKey);
            if (!Arrays.equals(sig, sig2)) {
                System.out.println("J : " + bytesToHex(sig));
                System.out.println("JS: " + bytesToHex(sig2));
                throw new IllegalStateException("Signatures not equal! " + bytesToHex(sig) + " != " + bytesToHex(sig2));
            }
            if (n == 1)
                System.out.println("Passed sign tests.");

            // unsign
            {
                byte[] unsigned = TweetNaCl.crypto_sign_open(sig, publicSigningKey);
                if (!Arrays.equals(unsigned, message))
                    throw new IllegalStateException("J (J sig): Unsigned message != original! ");
            }
            {
                byte[] unsigned = TweetNaCl.crypto_sign_open(sig2, publicSigningKey);
                if (!Arrays.equals(unsigned, message))
                    throw new IllegalStateException("J (JS sig): Unsigned message != original! ");
            }
            {
                byte[] unsigned2 = unsignMessage(sig, publicSigningKey);
                if (!Arrays.equals(unsigned2, message))
                    throw new IllegalStateException("JS (J sig): Unsigned message != original! ");
            }
            {
                byte[] unsigned2 = unsignMessage(sig2, publicSigningKey);
                if (!Arrays.equals(unsigned2, message))
                    throw new IllegalStateException("JS (JS sig): Unsigned message != original! ");
            }
            if (n == 1)
                System.out.println("Passed unsign tests.");

            // unsign with error
            byte[] sigerr = Arrays.copyOf(sig, sig.length);
            sigerr[0] = (byte)(~sigerr[0]);
            try {
                byte[] unsignederr = TweetNaCl.crypto_sign_open(sigerr, publicSigningKey);
                throw new IllegalStateException("J: invalid unsign didn't fail! ");
            } catch (TweetNaCl.InvalidSignatureException e) {}
            try {
                byte[] unsigned2err = unsignMessage(sigerr, publicSigningKey);
                throw new IllegalStateException("JS: invalid unsign didn't fail! ");
            } catch (TweetNaCl.InvalidSignatureException e) {}
            if (n == 1)
                System.out.println("Passed unsign with error tests.");
        }
        System.out.println("Passed all tests for "+n +" sets of random key pairs and random messages "+max+" bytes long!");
    }
}
