package com.sqlcli.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final String DEFAULT_DIR = ".sql-cli";
    private static final String CONFIG_FILE = "config.yml";

    private static volatile boolean encryptionWarned = false;

    private final Path configDir;
    private final Path configFile;
    private AppConfig config;

    public ConfigManager() {
        this(System.getProperty("user.home") + File.separator + DEFAULT_DIR);
    }

    public ConfigManager(String configDir) {
        this.configDir = Path.of(configDir);
        this.configFile = this.configDir.resolve(CONFIG_FILE);
    }

    public Path getConfigDir() { return configDir; }

    public Path getConfigFile() { return configFile; }

    public Path getDriverDir() {
        AppConfig appConfig = load();
        String driverDir = appConfig.getDefaults().getDriverDir();
        if (driverDir != null && !driverDir.isBlank()) {
            return Path.of(expandHome(driverDir));
        }
        return configDir.resolve("drivers");
    }

    /**
     * Load config from YAML file. If file doesn't exist, return defaults.
     */
    public AppConfig load() {
        if (config != null) return config;

        if (!Files.exists(configFile)) {
            config = new AppConfig();
            return config;
        }

        try (InputStream is = new FileInputStream(configFile.toFile())) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);
            if (data == null) {
                config = new AppConfig();
                return config;
            }
            config = parseConfig(data);
            return config;
        } catch (Exception e) {
            log.warn("Failed to load config from {}: {}", configFile, e.getMessage());
            config = new AppConfig();
            return config;
        }
    }

    /**
     * Save config to YAML file.
     */
    public void save(AppConfig config) {
        this.config = config;
        try {
            Files.createDirectories(configDir);
            Yaml yaml = new Yaml();
            String yamlStr = yaml.dumpAsMap(toMap(config));
            try (FileWriter writer = new FileWriter(configFile.toFile())) {
                writer.write(yamlStr);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config to " + configFile, e);
        }
    }

    /**
     * Get encryption service, loading secret from environment variable.
     */
    public EncryptionService getEncryptionService() {
        AppConfig cfg = load();
        String envVar = cfg.getEncryption().getKeyEnv();
        String secret = System.getenv(envVar);
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty(envVar.toLowerCase().replace('_', '.'));
        }
        if (secret == null || secret.isBlank()) {
            throw new RuntimeException(
                    "Encryption secret not set. Please set environment variable: " + envVar
                            + "\nYou can generate one with: export " + envVar + "=\"" + EncryptionService.generateSecret() + "\"");
        }
        return new EncryptionService(secret);
    }

    /**
     * Try to encrypt the plaintext password. On first failure, print a one-time WARN
     * with the env var hint and return plaintext. On subsequent failures, silently
     * return plaintext. On success, return the encrypted string.
     */
    public static String tryEncryptWithWarning(ConfigManager cm, String plaintext) {
        try {
            return cm.getEncryptionService().encrypt(plaintext);
        } catch (Exception e) {
            if (!encryptionWarned) {
                encryptionWarned = true;
                System.err.println("[WARN] Password encryption failed: " + e.getMessage()
                        + ". Password will be stored in plain text. "
                        + "Set environment variable SQL_CLI_SECRET to enable encryption.");
            }
            return plaintext;
        }
    }

    /**
     * Initialize config directory structure.
     */
    public void init() {
        try {
            Files.createDirectories(configDir);
            Files.createDirectories(getDriverDir());

            if (!Files.exists(configFile)) {
                AppConfig defaults = new AppConfig();
                defaults.getDefaults().setDriverDir(configDir.resolve("drivers").toString());
                save(defaults);
                log.info("Created default config at {}", configFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize config directory: " + configDir, e);
        }
    }

    @SuppressWarnings("unchecked")
    private AppConfig parseConfig(Map<String, Object> data) {
        AppConfig config = new AppConfig();

        if (data.containsKey("defaults")) {
            Map<String, Object> d = (Map<String, Object>) data.get("defaults");
            AppConfig.Defaults defaults = config.getDefaults();
            if (d.containsKey("maxRows")) defaults.setMaxRows(((Number) d.get("maxRows")).intValue());
            if (d.containsKey("autoLimit")) defaults.setAutoLimit((Boolean) d.get("autoLimit"));
            if (d.containsKey("safetyLevel")) defaults.setSafetyLevel((String) d.get("safetyLevel"));
            if (d.containsKey("outputFormat")) defaults.setOutputFormat((String) d.get("outputFormat"));
            if (d.containsKey("driverDir")) defaults.setDriverDir((String) d.get("driverDir"));
        }

        if (data.containsKey("encryption")) {
            Map<String, Object> e = (Map<String, Object>) data.get("encryption");
            if (e.containsKey("keyEnv")) config.getEncryption().setKeyEnv((String) e.get("keyEnv"));
        }

        if (data.containsKey("groups")) {
            config.setGroups((List<String>) data.get("groups"));
        }

        if (data.containsKey("customTypes")) {
            List<CustomTypeConfig> types = new ArrayList<>();
            for (Map<String, Object> m : (List<Map<String, Object>>) data.get("customTypes")) {
                CustomTypeConfig ct = new CustomTypeConfig();
                ct.setName((String) m.get("name"));
                ct.setDriverClass((String) m.get("driverClass"));
                ct.setDriver((String) m.get("driver"));
                ct.setUrlTemplate((String) m.get("urlTemplate"));
                if (m.containsKey("defaultPort")) ct.setDefaultPort(((Number) m.get("defaultPort")).intValue());
                if (m.containsKey("limitSuffix")) ct.setLimitSuffix((String) m.get("limitSuffix"));
                if (m.containsKey("limitPrefix")) ct.setLimitPrefix((String) m.get("limitPrefix"));
                if (m.containsKey("limitPattern")) ct.setLimitPattern((String) m.get("limitPattern"));
                if (m.containsKey("databaseLabel")) ct.setDatabaseLabel((String) m.get("databaseLabel"));
                if (m.containsKey("listDatabasesMethod")) ct.setListDatabasesMethod((String) m.get("listDatabasesMethod"));
                if (m.containsKey("systemSchemaFilter")) ct.setSystemSchemaFilter((String) m.get("systemSchemaFilter"));
                types.add(ct);
            }
            config.setCustomTypes(types);
        }

        if (data.containsKey("connections")) {
            List<ConnectionConfig> conns = new ArrayList<>();
            for (Map<String, Object> m : (List<Map<String, Object>>) data.get("connections")) {
                ConnectionConfig cc = new ConnectionConfig();
                cc.setName((String) m.get("name"));
                cc.setGroup((String) m.get("group"));
                cc.setTags((List<String>) m.get("tags"));
                cc.setType((String) m.get("type"));
                cc.setDriver((String) m.get("driver"));
                cc.setDriverClass((String) m.get("driverClass"));
                cc.setHost((String) m.get("host"));
                if (m.containsKey("port")) cc.setPort(((Number) m.get("port")).intValue());
                cc.setUser((String) m.get("user"));
                cc.setPassword((String) m.get("password"));
                cc.setDb((String) m.get("db"));
                cc.setUrl((String) m.get("url"));
                cc.setParams((Map<String, String>) m.get("params"));
                cc.setSafetyLevel((String) m.get("safetyLevel"));
                if (m.containsKey("defaultPort")) cc.setDefaultPort(((Number) m.get("defaultPort")).intValue());
                conns.add(cc);
            }
            config.setConnections(conns);
        }

        return config;
    }

    private Map<String, Object> toMap(AppConfig config) {
        Map<String, Object> map = new LinkedHashMap<>();

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("maxRows", config.getDefaults().getMaxRows());
        defaults.put("autoLimit", config.getDefaults().isAutoLimit());
        defaults.put("safetyLevel", config.getDefaults().getSafetyLevel());
        defaults.put("outputFormat", config.getDefaults().getOutputFormat());
        defaults.put("driverDir", config.getDefaults().getDriverDir());
        map.put("defaults", defaults);

        map.put("encryption", Map.of("keyEnv", config.getEncryption().getKeyEnv()));

        if (config.getGroups() != null && !config.getGroups().isEmpty()) {
            map.put("groups", config.getGroups());
        }

        if (config.getCustomTypes() != null && !config.getCustomTypes().isEmpty()) {
            List<Map<String, Object>> types = new ArrayList<>();
            for (CustomTypeConfig ct : config.getCustomTypes()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", ct.getName());
                m.put("driverClass", ct.getDriverClass());
                m.put("driver", ct.getDriver());
                m.put("urlTemplate", ct.getUrlTemplate());
                if (ct.getDefaultPort() != null) m.put("defaultPort", ct.getDefaultPort());
                if (ct.getLimitSuffix() != null) m.put("limitSuffix", ct.getLimitSuffix());
                if (ct.getLimitPrefix() != null) m.put("limitPrefix", ct.getLimitPrefix());
                if (ct.getLimitPattern() != null) m.put("limitPattern", ct.getLimitPattern());
                if (ct.getDatabaseLabel() != null) m.put("databaseLabel", ct.getDatabaseLabel());
                if (ct.getListDatabasesMethod() != null) m.put("listDatabasesMethod", ct.getListDatabasesMethod());
                if (ct.getSystemSchemaFilter() != null) m.put("systemSchemaFilter", ct.getSystemSchemaFilter());
                types.add(m);
            }
            map.put("customTypes", types);
        }

        if (config.getConnections() != null && !config.getConnections().isEmpty()) {
            List<Map<String, Object>> conns = new ArrayList<>();
            for (ConnectionConfig cc : config.getConnections()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", cc.getName());
                if (cc.getGroup() != null) m.put("group", cc.getGroup());
                if (cc.getTags() != null) m.put("tags", cc.getTags());
                m.put("type", cc.getType());
                if (cc.getDriver() != null) m.put("driver", cc.getDriver());
                if (cc.getDriverClass() != null) m.put("driverClass", cc.getDriverClass());
                if (cc.getHost() != null) m.put("host", cc.getHost());
                if (cc.getPort() != null) m.put("port", cc.getPort());
                m.put("user", cc.getUser());
                m.put("password", cc.getPassword());
                if (cc.getDb() != null) m.put("db", cc.getDb());
                if (cc.getUrl() != null) m.put("url", cc.getUrl());
                if (cc.getParams() != null) m.put("params", cc.getParams());
                if (cc.getSafetyLevel() != null) m.put("safetyLevel", cc.getSafetyLevel());
                if (cc.getDefaultPort() != null) m.put("defaultPort", cc.getDefaultPort());
                conns.add(m);
            }
            map.put("connections", conns);
        }

        return map;
    }

    private static String expandHome(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
