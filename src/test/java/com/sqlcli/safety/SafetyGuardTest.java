package com.sqlcli.safety;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConnectionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafetyGuardTest {

    private SafetyGuard guard;
    private AppConfig appConfig;
    private ConnectionConfig normalConfig;
    private ConnectionConfig strictConfig;
    private ConnectionConfig noneConfig;

    @BeforeEach
    void setUp() {
        guard = new SafetyGuard();
        appConfig = new AppConfig();

        normalConfig = new ConnectionConfig();
        normalConfig.setName("test");
        normalConfig.setType("mysql");
        normalConfig.setSafetyLevel("normal");

        strictConfig = new ConnectionConfig();
        strictConfig.setName("strict_conn");
        strictConfig.setType("mysql");
        strictConfig.setSafetyLevel("strict");

        noneConfig = new ConnectionConfig();
        noneConfig.setName("none_conn");
        noneConfig.setType("mysql");
        noneConfig.setSafetyLevel("none");
    }

    // --- Strict mode ---

    @Test
    void strictMode_allowsSelect() {
        String result = guard.validate("SELECT * FROM users", strictConfig, false, false, 0, appConfig);
        assertNotNull(result);
    }

    @Test
    void strictMode_blocksInsert() {
        assertThrows(RuntimeException.class, () ->
                guard.validate("INSERT INTO users VALUES (1, 'a')", strictConfig, false, false, 0, appConfig));
    }

    @Test
    void strictMode_blocksDelete() {
        assertThrows(RuntimeException.class, () ->
                guard.validate("DELETE FROM users", strictConfig, false, false, 0, appConfig));
    }

    @Test
    void strictMode_blocksUpdate() {
        assertThrows(RuntimeException.class, () ->
                guard.validate("UPDATE users SET name='x'", strictConfig, false, false, 0, appConfig));
    }

    @Test
    void strictMode_blocksDrop() {
        assertThrows(RuntimeException.class, () ->
                guard.validate("DROP TABLE users", strictConfig, false, false, 0, appConfig));
    }

    // --- Normal mode ---

    @Test
    void normalMode_allowsSelect() {
        String result = guard.validate("SELECT * FROM users", normalConfig, false, false, 0, appConfig);
        assertNotNull(result);
    }

    @Test
    void normalMode_blocksDropDatabase() {
        assertThrows(RuntimeException.class, () ->
                guard.validate("DROP DATABASE mydb", normalConfig, false, false, 0, appConfig));
    }

    @Test
    void normalMode_blocksDeleteWithoutWhere() {
        assertThrows(RuntimeException.class, () ->
                guard.validate("DELETE FROM users", normalConfig, false, false, 0, appConfig));
    }

    @Test
    void normalMode_blocksDropTableWithoutConfirm() {
        assertThrows(RuntimeException.class, () ->
                guard.validate("DROP TABLE users", normalConfig, false, false, 0, appConfig));
    }

    @Test
    void normalMode_allowsDropTableWithConfirm() {
        String result = guard.validate("DROP TABLE users", normalConfig, true, false, 0, appConfig);
        assertNotNull(result);
    }

    @Test
    void normalMode_allowsInsertWithWhere() {
        String result = guard.validate("INSERT INTO users VALUES (1, 'a')", normalConfig, false, false, 0, appConfig);
        assertNotNull(result);
    }

    @Test
    void normalMode_allowsCreateTable() {
        String result = guard.validate("CREATE TABLE test (id INT)", normalConfig, false, false, 0, appConfig);
        assertNotNull(result);
    }

    // --- None mode ---

    @Test
    void noneMode_allowsEverything() {
        String result = guard.validate("DROP TABLE users", noneConfig, false, false, 0, appConfig);
        assertNotNull(result);
    }

    @Test
    void noneMode_allowsDeleteWithoutWhere() {
        String result = guard.validate("DELETE FROM users", noneConfig, false, false, 0, appConfig);
        assertNotNull(result);
    }

    // --- Row limiting ---

    @Test
    void autoLimitAppliedToSelect() {
        appConfig.getDefaults().setAutoLimit(true);
        appConfig.getDefaults().setMaxRows(100);
        String result = guard.validate("SELECT * FROM users", normalConfig, false, false, 0, appConfig);
        assertTrue(result.contains("100"));
    }

    @Test
    void noLimitFlagDisablesLimit() {
        appConfig.getDefaults().setAutoLimit(true);
        String result = guard.validate("SELECT * FROM users", normalConfig, false, true, 0, appConfig);
        assertEquals("SELECT * FROM users", result);
    }

    @Test
    void userLimitOverridesDefault() {
        appConfig.getDefaults().setAutoLimit(true);
        String result = guard.validate("SELECT * FROM users", normalConfig, false, false, 50, appConfig);
        assertTrue(result.contains("50"));
    }
}
