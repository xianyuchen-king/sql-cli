package com.sqlcli.config;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final byte[] keyBytes;

    public EncryptionService(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Encryption secret must not be blank");
        }
        // Derive a 32-byte (256-bit) key from the secret
        this.keyBytes = deriveKey(secret);
    }

    private static byte[] deriveKey(String secret) {
        byte[] input = secret.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (input[i % input.length] ^ (i * 31 + 17));
        }
        return key;
    }

    /**
     * Encrypt plaintext and return "ENC(Base64)" format.
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return "ENC(" + Base64.getEncoder().encodeToString(combined) + ")";
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt "ENC(Base64)" format string.
     */
    public String decrypt(String encrypted) {
        try {
            String base64 = encrypted;
            if (base64.startsWith("ENC(") && base64.endsWith(")")) {
                base64 = base64.substring(4, base64.length() - 1);
            }

            byte[] combined = Base64.getDecoder().decode(base64);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Mask password for display: "ENC(***)"
     */
    public static String maskPassword(String password) {
        if (password == null) return null;
        return "ENC(***)";
    }

    /**
     * Check if a value looks like an encrypted password.
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }

    /**
     * Generate a random secret key for the user.
     */
    public static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
