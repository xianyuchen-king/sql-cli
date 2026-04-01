package com.sqlcli.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    private ConfigManager cm;

    @BeforeEach
    void setUp() {
        cm = new ConfigManager(tempDir.toString());
    }

    @Test
    void load_returnsDefaultsWhenNoFile() {
        AppConfig config = cm.load();
        assertNotNull(config);
        assertNotNull(config.getDefaults());
        assertEquals(500, config.getDefaults().getMaxRows());
        assertTrue(config.getDefaults().isAutoLimit());
    }

    @Test
    void load_returnsDefaultsWhenEmptyFile() {
        cm.init(); // creates default config
        // Re-create ConfigManager to clear cache
        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        AppConfig config = cm2.load();
        assertNotNull(config);
        assertEquals(500, config.getDefaults().getMaxRows());
    }

    @Test
    void saveAndLoad_preservesConnections() {
        AppConfig config = new AppConfig();
        ConnectionConfig conn = new ConnectionConfig();
        conn.setName("testdb");
        conn.setType("mysql");
        conn.setHost("localhost");
        conn.setPort(3306);
        conn.setUser("root");
        conn.setDb("mydb");
        config.setConnections(java.util.List.of(conn));

        cm.save(config);

        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        AppConfig loaded = cm2.load();
        assertEquals(1, loaded.getConnections().size());
        assertEquals("testdb", loaded.getConnections().get(0).getName());
        assertEquals("mysql", loaded.getConnections().get(0).getType());
        assertEquals("localhost", loaded.getConnections().get(0).getHost());
    }

    @Test
    void saveAndLoad_preservesDefaults() {
        AppConfig config = new AppConfig();
        config.getDefaults().setMaxRows(1000);
        config.getDefaults().setAutoLimit(false);
        config.getDefaults().setOutputFormat("json");

        cm.save(config);

        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        AppConfig loaded = cm2.load();
        assertEquals(1000, loaded.getDefaults().getMaxRows());
        assertFalse(loaded.getDefaults().isAutoLimit());
        assertEquals("json", loaded.getDefaults().getOutputFormat());
    }

    @Test
    void saveAndLoad_preservesCustomTypes() {
        AppConfig config = new AppConfig();
        CustomTypeConfig ct = new CustomTypeConfig();
        ct.setName("dm");
        ct.setDriverClass("dm.jdbc.driver.DmDriver");
        ct.setDriver("DmJdbcDriver18.jar");
        ct.setUrlTemplate("jdbc:dm://{host}:{port}/{db}");
        ct.setDefaultPort(5236);
        config.setCustomTypes(java.util.List.of(ct));

        cm.save(config);

        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        AppConfig loaded = cm2.load();
        assertEquals(1, loaded.getCustomTypes().size());
        assertEquals("dm", loaded.getCustomTypes().get(0).getName());
        assertEquals(5236, loaded.getCustomTypes().get(0).getDefaultPort());
    }

    @Test
    void saveAndLoad_preservesGroups() {
        AppConfig config = new AppConfig();
        config.setGroups(java.util.List.of("prod", "staging", "dev"));

        cm.save(config);

        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        AppConfig loaded = cm2.load();
        assertEquals(3, loaded.getGroups().size());
        assertTrue(loaded.getGroups().contains("prod"));
    }

    @Test
    void init_createsConfigDirAndFile() {
        cm.init();
        assertTrue(tempDir.resolve("config.yml").toFile().exists());
        assertTrue(tempDir.resolve("drivers").toFile().isDirectory());
    }

    @Test
    void init_idempotent() {
        cm.init();
        cm.init(); // should not throw
        assertTrue(tempDir.resolve("config.yml").toFile().exists());
    }

    @Test
    void getDriverDir_returnsDefault() {
        cm.init();
        assertEquals(tempDir.resolve("drivers"), cm.getDriverDir());
    }

    @Test
    void getDriverDir_returnsCustom() {
        AppConfig config = new AppConfig();
        config.getDefaults().setDriverDir("/tmp/custom-drivers");
        cm.save(config);

        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        assertEquals(Path.of("/tmp/custom-drivers"), cm2.getDriverDir());
    }

    @Test
    void load_cachesResult() {
        AppConfig first = cm.load();
        AppConfig second = cm.load();
        assertSame(first, second);
    }

    @Test
    void saveAndLoad_preservesAllCustomTypeFields() {
        AppConfig config = new AppConfig();
        CustomTypeConfig ct = new CustomTypeConfig();
        ct.setName("dm");
        ct.setDriverClass("dm.jdbc.driver.DmDriver");
        ct.setDriver("DmJdbcDriver18.jar");
        ct.setUrlTemplate("jdbc:dm://{host}:{port}/{db}");
        ct.setDefaultPort(5236);
        ct.setLimitSuffix(" LIMIT ?");
        ct.setLimitPrefix("SELECT * FROM (");
        ct.setLimitPattern(".*LIMIT\\s+\\d+.*");
        ct.setDatabaseLabel("Schema");
        ct.setListDatabasesMethod("getSchemas");
        ct.setSystemSchemaFilter("^(SYS|DBA)");
        config.setCustomTypes(java.util.List.of(ct));

        cm.save(config);

        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        AppConfig loaded = cm2.load();
        assertEquals(1, loaded.getCustomTypes().size());
        CustomTypeConfig loadedCt = loaded.getCustomTypes().get(0);
        assertEquals("dm", loadedCt.getName());
        assertEquals("dm.jdbc.driver.DmDriver", loadedCt.getDriverClass());
        assertEquals("DmJdbcDriver18.jar", loadedCt.getDriver());
        assertEquals("jdbc:dm://{host}:{port}/{db}", loadedCt.getUrlTemplate());
        assertEquals(5236, loadedCt.getDefaultPort());
        assertEquals(" LIMIT ?", loadedCt.getLimitSuffix());
        assertEquals("SELECT * FROM (", loadedCt.getLimitPrefix());
        assertEquals(".*LIMIT\\s+\\d+.*", loadedCt.getLimitPattern());
        assertEquals("Schema", loadedCt.getDatabaseLabel());
        assertEquals("getSchemas", loadedCt.getListDatabasesMethod());
        assertEquals("^(SYS|DBA)", loadedCt.getSystemSchemaFilter());
    }

    @Test
    void load_backwardCompat_noDialectFields() {
        AppConfig config = new AppConfig();
        CustomTypeConfig ct = new CustomTypeConfig();
        ct.setName("dm");
        ct.setDriverClass("dm.jdbc.driver.DmDriver");
        ct.setDriver("DmJdbcDriver18.jar");
        ct.setUrlTemplate("jdbc:dm://{host}:{port}/{db}");
        ct.setDefaultPort(5236);
        config.setCustomTypes(java.util.List.of(ct));

        cm.save(config);

        ConfigManager cm2 = new ConfigManager(tempDir.toString());
        AppConfig loaded = cm2.load();
        assertEquals(1, loaded.getCustomTypes().size());
        CustomTypeConfig loadedCt = loaded.getCustomTypes().get(0);
        assertEquals("dm", loadedCt.getName());
        assertEquals(5236, loadedCt.getDefaultPort());
        assertNull(loadedCt.getLimitSuffix());
        assertNull(loadedCt.getLimitPrefix());
        assertNull(loadedCt.getLimitPattern());
        assertNull(loadedCt.getDatabaseLabel());
        assertNull(loadedCt.getListDatabasesMethod());
        assertNull(loadedCt.getSystemSchemaFilter());
    }
}
