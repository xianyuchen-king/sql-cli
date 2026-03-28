package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;

import java.sql.Connection;

/**
 * Shared utility for CLI commands that need database connections.
 */
public class ConnectionOptions {

    private final ConfigManager configManager;
    private final ConnectionManager connectionManager;

    public ConnectionOptions() {
        this.configManager = new ConfigManager();
        this.connectionManager = new ConnectionManager(configManager);
    }

    public ConnectionOptions(ConfigManager configManager, ConnectionManager connectionManager) {
        this.configManager = configManager;
        this.connectionManager = connectionManager;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public ConnectionManager getConnectionManager() { return connectionManager; }

    /**
     * Build a ConnectionConfig from inline CLI parameters.
     */
    public ConnectionConfig buildInlineConfig(String type, String host, Integer port,
                                               String user, String password, String db,
                                               String url, String driver, String driverClass) {
        ConnectionConfig config = new ConnectionConfig();
        config.setType(type);
        config.setHost(host);
        config.setPort(port);
        config.setUser(user);
        config.setPassword(password);
        config.setDb(db);
        config.setUrl(url);
        config.setDriver(driver);
        config.setDriverClass(driverClass);
        return config;
    }

    /**
     * Resolve connection and open it.
     */
    public Connection openConnection(String connectionName, ConnectionConfig inlineConfig) {
        ConnectionConfig resolved = connectionManager.resolveConnection(connectionName, inlineConfig);
        return connectionManager.connect(resolved);
    }

    /**
     * Load app config.
     */
    public AppConfig loadConfig() {
        return configManager.load();
    }
}
