package com.sqlcli.config;

import java.util.List;
import java.util.Map;

public class AppConfig {

    private Defaults defaults;
    private EncryptionConfig encryption;
    private List<String> groups;
    private List<CustomTypeConfig> customTypes;
    private List<ConnectionConfig> connections;

    public AppConfig() {
        this.defaults = new Defaults();
        this.encryption = new EncryptionConfig();
    }

    public Defaults getDefaults() { return defaults; }
    public void setDefaults(Defaults defaults) { this.defaults = defaults; }

    public EncryptionConfig getEncryption() { return encryption; }
    public void setEncryption(EncryptionConfig encryption) { this.encryption = encryption; }

    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }

    public List<CustomTypeConfig> getCustomTypes() { return customTypes; }
    public void setCustomTypes(List<CustomTypeConfig> customTypes) { this.customTypes = customTypes; }

    public List<ConnectionConfig> getConnections() { return connections; }
    public void setConnections(List<ConnectionConfig> connections) { this.connections = connections; }

    public ConnectionConfig getConnection(String name) {
        if (connections == null) return null;
        return connections.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static class Defaults {
        private int maxRows = 500;
        private boolean autoLimit = true;
        private String safetyLevel = "normal";
        private String outputFormat = "markdown";
        private String driverDir;

        public int getMaxRows() { return maxRows; }
        public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
        public boolean isAutoLimit() { return autoLimit; }
        public void setAutoLimit(boolean autoLimit) { this.autoLimit = autoLimit; }
        public String getSafetyLevel() { return safetyLevel; }
        public void setSafetyLevel(String safetyLevel) { this.safetyLevel = safetyLevel; }
        public String getOutputFormat() { return outputFormat; }
        public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
        public String getDriverDir() { return driverDir; }
        public void setDriverDir(String driverDir) { this.driverDir = driverDir; }
    }

    public static class EncryptionConfig {
        private String keyEnv = "SQL_CLI_SECRET";

        public String getKeyEnv() { return keyEnv; }
        public void setKeyEnv(String keyEnv) { this.keyEnv = keyEnv; }
    }
}
