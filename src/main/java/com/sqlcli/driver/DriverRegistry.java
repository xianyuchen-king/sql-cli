package com.sqlcli.driver;

import java.util.Map;
import java.util.TreeMap;

public final class DriverRegistry {

    private DriverRegistry() {}

    public record DriverInfo(String groupId, String artifactId, String defaultVersion, String driverClass, int defaultPort) {}

    private static final Map<String, DriverInfo> DRIVERS = new TreeMap<>(Map.ofEntries(
            Map.entry("mysql", new DriverInfo("com.mysql", "mysql-connector-j", "8.3.0", "com.mysql.cj.jdbc.Driver", 3306)),
            Map.entry("oracle", new DriverInfo("com.oracle.database.jdbc", "ojdbc11", "23.3.0.23.09", "oracle.jdbc.OracleDriver", 1521)),
            Map.entry("postgresql", new DriverInfo("org.postgresql", "postgresql", "42.7.2", "org.postgresql.Driver", 5432)),
            Map.entry("sqlite", new DriverInfo("org.xerial", "sqlite-jdbc", "3.45.1.0", "org.sqlite.JDBC", 0)),
            Map.entry("mssql", new DriverInfo("com.microsoft.sqlserver", "mssql-jdbc", "12.4.2.jre11", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433))
    ));

    public static DriverInfo get(String type) {
        return DRIVERS.get(type.toLowerCase());
    }

    public static Map<String, DriverInfo> all() {
        return Map.copyOf(DRIVERS);
    }
}
