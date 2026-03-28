package com.sqlcli.config;

import java.util.List;
import java.util.Map;

public class ConnectionConfig {

    private String name;
    private String group;
    private List<String> tags;
    private String type;
    private String driver;
    private String driverClass;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private String db;
    private String url;
    private Map<String, String> params;
    private String safetyLevel;
    private Integer defaultPort;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }

    public String getDriverClass() { return driverClass; }
    public void setDriverClass(String driverClass) { this.driverClass = driverClass; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDb() { return db; }
    public void setDb(String db) { this.db = db; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }

    public String getSafetyLevel() { return safetyLevel; }
    public void setSafetyLevel(String safetyLevel) { this.safetyLevel = safetyLevel; }

    public Integer getDefaultPort() { return defaultPort; }
    public void setDefaultPort(Integer defaultPort) { this.defaultPort = defaultPort; }

    /**
     * Check if this connection uses a direct URL instead of simplified parameters.
     */
    public boolean isDirectUrl() {
        return url != null && !url.isBlank();
    }

    /**
     * Get effective safety level, falling back to config defaults.
     */
    public String getEffectiveSafetyLevel(String defaultSafetyLevel) {
        return safetyLevel != null ? safetyLevel : defaultSafetyLevel;
    }
}
