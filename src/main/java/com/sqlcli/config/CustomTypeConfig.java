package com.sqlcli.config;

public class CustomTypeConfig {

    private String name;
    private String driverClass;
    private String driver;
    private String urlTemplate;
    private Integer defaultPort;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDriverClass() { return driverClass; }
    public void setDriverClass(String driverClass) { this.driverClass = driverClass; }

    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }

    public String getUrlTemplate() { return urlTemplate; }
    public void setUrlTemplate(String urlTemplate) { this.urlTemplate = urlTemplate; }

    public Integer getDefaultPort() { return defaultPort; }
    public void setDefaultPort(Integer defaultPort) { this.defaultPort = defaultPort; }
}
