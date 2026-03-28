package com.sqlcli.dialect;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.CustomTypeConfig;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DialectFactory {

    private static final Map<String, Dialect> BUILTIN_DIALECTS = Map.of(
            "mysql", new MysqlDialect(),
            "oracle", new OracleDialect(),
            "postgresql", new PostgresqlDialect(),
            "sqlite", new SqliteDialect(),
            "mssql", new MssqlDialect()
    );

    /**
     * Get dialect for a database type.
     * Checks builtin types first, then custom registered types.
     */
    public static Dialect getDialect(String type, AppConfig config) {
        // Check builtin
        Dialect dialect = BUILTIN_DIALECTS.get(type.toLowerCase());
        if (dialect != null) return dialect;

        // Check custom types
        if (config != null && config.getCustomTypes() != null) {
            for (CustomTypeConfig ct : config.getCustomTypes()) {
                if (ct.getName().equalsIgnoreCase(type)) {
                    return new CustomDialect(ct);
                }
            }
        }

        return new GenericDialect();
    }

    /**
     * Get all registered type names with their source (builtin/custom).
     */
    public static Map<String, String> getAllTypes(AppConfig config) {
        Map<String, String> types = BUILTIN_DIALECTS.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), k -> "builtin", (a, b) -> a));

        if (config != null && config.getCustomTypes() != null) {
            for (CustomTypeConfig ct : config.getCustomTypes()) {
                types.put(ct.getName(), "custom");
            }
        }
        return types;
    }

    /**
     * Get default driver class for a type.
     */
    public static String getDefaultDriverClass(String type, AppConfig config) {
        Dialect dialect = getDialect(type, config);
        String driverClass = dialect.getDefaultDriverClass();
        if (driverClass != null) return driverClass;

        // Check custom types
        if (config != null && config.getCustomTypes() != null) {
            for (CustomTypeConfig ct : config.getCustomTypes()) {
                if (ct.getName().equalsIgnoreCase(type)) {
                    return ct.getDriverClass();
                }
            }
        }
        return null;
    }

    /**
     * Get default port for a type.
     */
    public static int getDefaultPort(String type, AppConfig config) {
        Dialect dialect = getDialect(type, config);
        int port = dialect.getDefaultPort();
        if (port > 0) return port;

        if (config != null && config.getCustomTypes() != null) {
            for (CustomTypeConfig ct : config.getCustomTypes()) {
                if (ct.getName().equalsIgnoreCase(type) && ct.getDefaultPort() != null) {
                    return ct.getDefaultPort();
                }
            }
        }
        return 0;
    }

    /**
     * Custom dialect created from a registered CustomTypeConfig.
     */
    private static class CustomDialect implements Dialect {

        private final CustomTypeConfig config;

        CustomDialect(CustomTypeConfig config) {
            this.config = config;
        }

        @Override
        public String buildUrl(ConnectionConfig connConfig) {
            if (connConfig.getUrl() != null && !connConfig.getUrl().isBlank()) {
                return connConfig.getUrl();
            }
            if (config.getUrlTemplate() == null) {
                throw new IllegalArgumentException(
                        "Custom type '" + config.getName() + "' has no URL template. Use --url directly.");
            }
            String template = config.getUrlTemplate();
            String host = connConfig.getHost() != null ? connConfig.getHost() : "localhost";
            int port = connConfig.getPort() != null ? connConfig.getPort()
                    : (config.getDefaultPort() != null ? config.getDefaultPort() : 0);
            String db = connConfig.getDb() != null ? connConfig.getDb() : "";

            return template
                    .replace("{host}", host)
                    .replace("{port}", String.valueOf(port))
                    .replace("{db}", db);
        }

        @Override
        public String wrapLimit(String sql, int maxRows) {
            // Try generic LIMIT, return null if unsure
            return null;
        }

        @Override
        public boolean hasLimit(String sql) {
            return false;
        }

        @Override
        public int getDefaultPort() {
            return config.getDefaultPort() != null ? config.getDefaultPort() : 0;
        }

        @Override
        public String getDefaultDriverClass() {
            return config.getDriverClass();
        }
    }
}
