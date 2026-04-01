package com.sqlcli.cli;

import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConnRemoveTypeCommandTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream errOutput;
    private ByteArrayOutputStream outOutput;
    private String originalHome;

    @BeforeEach
    void setUp() {
        errOutput = new ByteArrayOutputStream();
        outOutput = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errOutput));
        System.setOut(new PrintStream(outOutput));
        originalHome = System.getProperty("user.home");
    }

    @org.junit.jupiter.api.AfterEach
    void restoreHome() {
        System.setProperty("user.home", originalHome);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private ConnRemoveTypeCommand createCommand(String name) throws Exception {
        ConnRemoveTypeCommand cmd = new ConnRemoveTypeCommand();
        setField(cmd, "name", name);
        return cmd;
    }

    private ConnectionConfig makeConn(String name, String type) {
        ConnectionConfig c = new ConnectionConfig();
        c.setName(name);
        c.setType(type);
        c.setHost("localhost");
        c.setPort(1234);
        c.setUser("user");
        c.setPassword("pass");
        c.setDriver("test.jar");
        return c;
    }

    private void writeTestConfig(String typeName, List<ConnectionConfig> connections) throws Exception {
        Path sqlCliDir = tempDir.resolve(".sql-cli");
        Files.createDirectories(sqlCliDir);
        StringBuilder yaml = new StringBuilder();
        yaml.append("defaults:\n  maxRows: 500\n  autoLimit: true\n  safetyLevel: normal\n  outputFormat: markdown\n  driverDir: /tmp/drivers\n");
        yaml.append("encryption:\n  keyEnv: SQL_CLI_SECRET\n");
        if (typeName != null) {
            yaml.append("customTypes:\n");
            yaml.append("- name: ").append(typeName).append("\n");
            yaml.append("  driverClass: com.example.Driver\n");
            yaml.append("  driver: test.jar\n");
            yaml.append("  urlTemplate: \"jdbc:test://{host}:{port}\"\n");
            yaml.append("  defaultPort: 1234\n");
        }
        if (connections != null && !connections.isEmpty()) {
            yaml.append("connections:\n");
            for (ConnectionConfig c : connections) {
                yaml.append("- name: ").append(c.getName()).append("\n");
                yaml.append("  type: ").append(c.getType()).append("\n");
                yaml.append("  host: ").append(c.getHost()).append("\n");
                yaml.append("  port: ").append(c.getPort()).append("\n");
                yaml.append("  user: ").append(c.getUser()).append("\n");
                yaml.append("  password: ").append(c.getPassword()).append("\n");
                yaml.append("  driver: ").append(c.getDriver()).append("\n");
            }
        }
        Files.writeString(sqlCliDir.resolve("config.yml"), yaml.toString());
    }

    @Test
    void removeBuiltinType_error() throws Exception {
        ConnRemoveTypeCommand cmd = createCommand("mysql");
        cmd.run();

        String err = errOutput.toString();
        assertTrue(err.contains("[ERROR]"), "Should print error for builtin type");
        assertTrue(err.contains("Cannot remove builtin type"), "Should say cannot remove builtin");
    }

    @Test
    void removeNonExistentType_error() throws Exception {
        ConnRemoveTypeCommand cmd = createCommand("nonexistent_type_xyz");
        cmd.run();

        String err = errOutput.toString();
        assertTrue(err.contains("[ERROR]"), "Should print error for non-existent type");
        assertTrue(err.contains("not found"), "Should say type not found");
    }

    @Test
    void removeType_referencedByConnection_error() throws Exception {
        writeTestConfig("mytype", List.of(makeConn("conn1", "mytype")));
        System.setProperty("user.home", tempDir.toString());

        ConnRemoveTypeCommand cmd = createCommand("mytype");
        cmd.run();

        String err = errOutput.toString();
        assertTrue(err.contains("[ERROR]"), "Should print error when type is referenced");
        assertTrue(err.contains("referenced by 1 connection"), "Should mention reference count");
    }

    @Test
    void removeType_referencedByMultipleConnections_error() throws Exception {
        writeTestConfig("mytype", List.of(
                makeConn("conn1", "mytype"),
                makeConn("conn2", "mytype")
        ));
        System.setProperty("user.home", tempDir.toString());

        ConnRemoveTypeCommand cmd = createCommand("mytype");
        cmd.run();

        String err = errOutput.toString();
        assertTrue(err.contains("[ERROR]"), "Should print error when type is referenced");
        assertTrue(err.contains("referenced by 2 connection"), "Should mention 2 connections");
    }

    @Test
    void removeType_notReferenced_success() throws Exception {
        writeTestConfig("mytype", List.of(makeConn("conn1", "mysql")));
        System.setProperty("user.home", tempDir.toString());

        ConnRemoveTypeCommand cmd = createCommand("mytype");
        cmd.run();

        String out = outOutput.toString();
        String err = errOutput.toString();
        assertTrue(out.contains("[DONE]"), "Should succeed when type is not referenced");
        assertTrue(err.isEmpty(), "Should not print error: " + err);
    }
}
