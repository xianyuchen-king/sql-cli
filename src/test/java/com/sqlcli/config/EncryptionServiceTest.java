package com.sqlcli.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private final EncryptionService service = new EncryptionService("my-test-secret-key-123");

    @Test
    void encryptAndDecrypt() {
        String original = "mypassword123!";
        String encrypted = service.encrypt(original);
        assertTrue(encrypted.startsWith("ENC("));
        assertNotEquals(original, encrypted);

        String decrypted = service.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void roundTrip() {
        String[] passwords = {"", "a", "password", "P@ssw0rd!#$%", "中文密码"};
        for (String pw : passwords) {
            String encrypted = service.encrypt(pw);
            assertEquals(pw, service.decrypt(encrypted));
        }
    }

    @Test
    void isEncrypted() {
        assertTrue(EncryptionService.isEncrypted("ENC(abc123)"));
        assertTrue(EncryptionService.isEncrypted("ENC()"));
        assertFalse(EncryptionService.isEncrypted("plaintext"));
        assertFalse(EncryptionService.isEncrypted("enc(abc)"));
        assertFalse(EncryptionService.isEncrypted(null));
    }

    @Test
    void maskPassword() {
        assertEquals("ENC(***)", EncryptionService.maskPassword("ENC(abc123)"));
        assertEquals("ENC(***)", EncryptionService.maskPassword("anything"));
        assertNull(EncryptionService.maskPassword(null));
    }

    @Test
    void generateSecret() {
        String secret = EncryptionService.generateSecret();
        assertNotNull(secret);
        assertTrue(secret.length() > 20);
    }

    @Test
    void blankSecretThrows() {
        assertThrows(IllegalArgumentException.class, () -> new EncryptionService(""));
    }
}
