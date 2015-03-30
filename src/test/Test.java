package test;

import org.peergos.crypto.TweetNaCl;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

public class Test
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
                    "    var jarr = Java.type('org.peergos.crypto.TweetNaCl').getRandomValues(arr.length);\n" +
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
                    "}");
            engine.eval(new InputStreamReader(Test.class.getClassLoader().getResourceAsStream("src/test/nacl.js")));
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
            return (byte[]) invocable.invokeFunction("toByteArray", res);
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    public static void main(String[] args) {
        byte[] publicBoxingKey = new byte[32];
        byte[] secretBoxingKey = new byte[32];
        TweetNaCl.crypto_box_keypair(publicBoxingKey, secretBoxingKey, true);

        byte[] message = "G'day mate!".getBytes();

        // box
        byte[] nonce = Test.createNonce();
        byte[] cipher = encryptMessageFor(message, nonce, publicBoxingKey, secretBoxingKey);
        byte[] cipher2 = TweetNaCl.crypto_box(message, nonce, publicBoxingKey, secretBoxingKey);
        if (!Arrays.equals(cipher, cipher2)) {
            throw new IllegalStateException("Different ciphertexts with same nonce!: "+bytesToHex(cipher) + " != "+bytesToHex(cipher2));
        }

        // unbox
        byte[] clear = TweetNaCl.crypto_box_open(cipher, nonce, publicBoxingKey, secretBoxingKey);
        if (!Arrays.equals(clear, message)) {
            throw new IllegalStateException("JS -> J, Decrypted message != original: "+new String(clear) + " != "+new String(message));
        }
        byte[] clear2 = decryptMessage(cipher2, nonce, publicBoxingKey, secretBoxingKey);
        if (!Arrays.equals(clear2, message))
            throw new IllegalStateException("J -> JS, Decrypted message != original: "+new String(clear2) + " != "+new String(message));

        // sign and unsign
        byte[] publicSigningKey = new byte[32];
        byte[] secretSigningKey = new byte[64];
        TweetNaCl.crypto_sign_keypair(publicSigningKey, secretSigningKey, true);
        byte[] sig = TweetNaCl.crypto_sign(message, secretSigningKey);
        byte[] sig2 = signMessage(message, secretSigningKey);
        if (!Arrays.equals(sig, sig2)) {
            System.out.println("J : "+bytesToHex(sig));
            System.out.println("JS: "+bytesToHex(sig2));
            throw new IllegalStateException("Signatures not equal! " + bytesToHex(sig) + " != " + bytesToHex(sig2));
        }

        byte[] unsigned = TweetNaCl.crypto_sign_open(sig, publicSigningKey);
        if (!Arrays.equals(unsigned, message))
            throw new IllegalStateException("J: Unsigned message != original! ");
        byte[] unsigned2 = unsignMessage(sig, publicSigningKey);
        if (!Arrays.equals(unsigned2, message))
            throw new IllegalStateException("JS: Unsigned message != original! ");
    }
}
