package com.sqlcli.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerEncryptionWarningTest {

    @TempDir
    Path tempDir;

    private PrintStream originalStderr;
    private ByteArrayOutputStream stderrCapture;

    @BeforeEach
    void setUp() throws Exception {
        // Reset the static encryptionWarned flag before each test
        Field field = ConfigManager.class.getDeclaredField("encryptionWarned");
        field.setAccessible(true);
        field.set(null, false);

        // Capture stderr
        originalStderr = System.err;
        stderrCapture = new ByteArrayOutputStream();
        System.setErr(new PrintStream(stderrCapture, true));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalStderr);
    }

    private void resetEncryptionWarned() throws Exception {
        Field field = ConfigManager.class.getDeclaredField("encryptionWarned");
        field.setAccessible(true);
        field.set(null, false);
    }

    @Test
    void firstEncryptionFailure_outputsWarnToStderr() {
        ConfigManager cm = new ConfigManager(tempDir.toString());
        // No SQL_CLI_SECRET set, so encryption will fail
        String result = ConfigManager.tryEncryptWithWarning(cm, "mySecret");

        String stderr = stderrCapture.toString();
        assertTrue(stderr.contains("[WARN]"), "First failure should print WARN to stderr");
        assertTrue(stderr.contains("Password encryption failed"), "Should mention encryption failure");
        assertTrue(stderr.contains("SQL_CLI_SECRET"), "Should hint the env var name");
        assertEquals("mySecret", result, "Failure should return plaintext");
    }

    @Test
    void secondEncryptionFailure_outputsNothing() throws Exception {
        ConfigManager cm = new ConfigManager(tempDir.toString());

        // First call — triggers WARN
        ConfigManager.tryEncryptWithWarning(cm, "secret1");
        String firstStderr = stderrCapture.toString();
        assertTrue(firstStderr.contains("[WARN]"));

        // Clear capture to measure second call only
        stderrCapture.reset();

        // Second call — should be silent
        ConfigManager.tryEncryptWithWarning(cm, "secret2");
        String secondStderr = stderrCapture.toString();
        assertTrue(secondStderr.isEmpty(), "Second failure should output nothing");
    }

    @Test
    void encryptionSuccess_outputsNothing() throws Exception {
        // Set the env var so encryption succeeds
        String secret = EncryptionService.generateSecret();
        // ConfigManager reads from env var SQL_CLI_SECRET by default
        // We need to set it so getEncryptionService() works
        ConfigManager cm = new ConfigManager(tempDir.toString());
        // Create config that uses a system property instead
        // The default env var is SQL_CLI_SECRET
        // We set it as a system property (lowercase with dots) as fallback
        System.setProperty("sql.cli.secret", secret);

        try {
            String result = ConfigManager.tryEncryptWithWarning(cm, "myPassword");

            String stderr = stderrCapture.toString();
            assertTrue(stderr.isEmpty(), "Success should output nothing to stderr");
            assertNotNull(result);
            assertTrue(result.startsWith("ENC("), "Success should return encrypted string starting with ENC(");
        } finally {
            System.clearProperty("sql.cli.secret");
        }
    }

    @Test
    void failureReturnsPlaintext() {
        ConfigManager cm = new ConfigManager(tempDir.toString());
        String plaintext = "plainPassword123";
        String result = ConfigManager.tryEncryptWithWarning(cm, plaintext);

        assertEquals(plaintext, result, "Failure must return the original plaintext");
        assertFalse(result.startsWith("ENC("), "Plaintext should not be wrapped in ENC()");
    }

    @Test
    void successReturnsEncryptedString() throws Exception {
        String secret = EncryptionService.generateSecret();
        System.setProperty("sql.cli.secret", secret);

        ConfigManager cm = new ConfigManager(tempDir.toString());
        try {
            String result = ConfigManager.tryEncryptWithWarning(cm, "hello");

            assertTrue(result.startsWith("ENC("), "Encrypted string should start with ENC(");
            assertTrue(result.endsWith(")"), "Encrypted string should end with )");
            assertNotEquals("hello", result, "Encrypted string should differ from plaintext");
        } finally {
            System.clearProperty("sql.cli.secret");
        }
    }
}
