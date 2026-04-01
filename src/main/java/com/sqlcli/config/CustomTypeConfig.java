package com.sqlcli.config;

public class CustomTypeConfig {

    private String name;
    private String driverClass;
    private String driver;
    private String urlTemplate;
    private Integer defaultPort;
    private String limitSuffix;
    private String limitPrefix;
    private String limitPattern;
    private String databaseLabel;
    private String listDatabasesMethod;
    private String systemSchemaFilter;

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

    public String getLimitSuffix() { return limitSuffix; }
    public void setLimitSuffix(String limitSuffix) { this.limitSuffix = limitSuffix; }

    public String getLimitPrefix() { return limitPrefix; }
    public void setLimitPrefix(String limitPrefix) { this.limitPrefix = limitPrefix; }

    public String getLimitPattern() { return limitPattern; }
    public void setLimitPattern(String limitPattern) { this.limitPattern = limitPattern; }

    public String getDatabaseLabel() { return databaseLabel; }
    public void setDatabaseLabel(String databaseLabel) { this.databaseLabel = databaseLabel; }

    public String getListDatabasesMethod() { return listDatabasesMethod; }
    public void setListDatabasesMethod(String listDatabasesMethod) { this.listDatabasesMethod = listDatabasesMethod; }

    public String getSystemSchemaFilter() { return systemSchemaFilter; }
    public void setSystemSchemaFilter(String systemSchemaFilter) { this.systemSchemaFilter = systemSchemaFilter; }
}
