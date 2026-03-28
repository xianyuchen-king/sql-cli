package com.sqlcli.connection;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.EncryptionService;
import com.sqlcli.dialect.Dialect;
import com.sqlcli.dialect.DialectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final ConfigManager configManager;
    private final DriverLoader driverLoader;

    public ConnectionManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.driverLoader = new DriverLoader(configManager.getDriverDir().toFile());
    }

    /**
     * Resolve a ConnectionConfig from either a saved connection name or inline parameters.
     */
    public ConnectionConfig resolveConnection(String connectionName, ConnectionConfig inline) {
        if (connectionName != null && !connectionName.isBlank()) {
            AppConfig config = configManager.load();
            ConnectionConfig saved = config.getConnection(connectionName);
            if (saved == null) {
                throw new RuntimeException("Connection not found: " + connectionName);
            }
            // Decrypt password
            if (saved.getPassword() != null && EncryptionService.isEncrypted(saved.getPassword())) {
                try {
                    String decrypted = configManager.getEncryptionService().decrypt(saved.getPassword());
                    ConnectionConfig resolved = new ConnectionConfig();
                    copyFields(saved, resolved);
                    resolved.setPassword(decrypted);
                    return resolved;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decrypt password for connection: " + connectionName, e);
                }
            }
            return saved;
        }
        if (inline != null && (inline.getUrl() != null || inline.getHost() != null || inline.getType() != null)) {
            return inline;
        }
        throw new RuntimeException("Either -c <connection-name> or connection parameters are required");
    }

    /**
     * Establish a JDBC connection using the resolved config.
     */
    public Connection connect(ConnectionConfig config) {
        try {
            String url;
            String driverClassName;
            String driverJar;
            Dialect dialect;

            if (config.isDirectUrl()) {
                url = config.getUrl();
                driverClassName = config.getDriverClass();
                driverJar = config.getDriver();
                dialect = new com.sqlcli.dialect.GenericDialect();
            } else {
                AppConfig appConfig = configManager.load();
                dialect = DialectFactory.getDialect(config.getType(), appConfig);
                url = dialect.buildUrl(config);
                driverClassName = config.getDriverClass() != null
                        ? config.getDriverClass()
                        : dialect.getDefaultDriverClass();
                driverJar = config.getDriver();
            }

            if (driverClassName == null) {
                throw new RuntimeException("Driver class not specified and cannot be inferred for type: "
                        + config.getType());
            }

            // Load driver
            Driver driver;
            if (driverJar != null && !driverJar.isBlank()) {
                driver = driverLoader.loadDriver(driverJar, driverClassName);
            } else {
                // Try standard classpath loading
                try {
                    Class<?> clazz = Class.forName(driverClassName);
                    driver = (Driver) clazz.getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                         | java.lang.reflect.InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException("Driver class not found: " + driverClassName
                            + ". Please specify --driver <jar-file>.", e);
                }
            }

            // Connect
            Properties props = new Properties();
            if (config.getUser() != null) props.put("user", config.getUser());
            if (config.getPassword() != null) props.put("password", config.getPassword());

            Connection conn = driver.connect(url, props);
            if (conn == null) {
                throw new RuntimeException("Driver rejected the URL: " + url);
            }

            log.debug("Connected to {} via {}", url, driverClassName);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect: " + e.getMessage(), e);
        }
    }

    /**
     * Test a connection without keeping it open.
     */
    public boolean testConnection(ConnectionConfig config) {
        try (Connection conn = connect(config)) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.debug("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    public DriverLoader getDriverLoader() {
        return driverLoader;
    }

    private void copyFields(ConnectionConfig from, ConnectionConfig to) {
        to.setName(from.getName());
        to.setGroup(from.getGroup());
        to.setTags(from.getTags());
        to.setType(from.getType());
        to.setDriver(from.getDriver());
        to.setDriverClass(from.getDriverClass());
        to.setHost(from.getHost());
        to.setPort(from.getPort());
        to.setUser(from.getUser());
        to.setDb(from.getDb());
        to.setUrl(from.getUrl());
        to.setParams(from.getParams());
        to.setSafetyLevel(from.getSafetyLevel());
        to.setDefaultPort(from.getDefaultPort());
        // Password is set separately (decrypted)
    }
}
