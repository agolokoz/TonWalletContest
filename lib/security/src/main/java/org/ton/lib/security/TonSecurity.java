package org.ton.lib.security;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

public class TonSecurity {

    static {
        System.loadLibrary("tonsecurity");
    }

    @Keep
    @Nullable
    public static native byte[] nativeGetArgonHash(byte[] password, byte[] salt, int tCost, int mCost, int parallelism, int hashLen);

    @Keep
    @Nullable
    public static native byte[] nativeCryptoBoxInitKeys();

    @Keep
    @Nullable
    public static native byte[] nativeCryptoBox(byte[] message, byte[] publicKey, byte[] secretKey);

    @Keep
    @Nullable
    public static native byte[] nativeCryptoBoxOpen(byte[] cipher, byte[] publicKey, byte[] secretKey);

    @Keep
    public static native void nativeCryptoBoxTest(byte[] message, byte[] publicKey1, byte[] secretKey1, byte[] publicKey2, byte[] secretKey2);
}
