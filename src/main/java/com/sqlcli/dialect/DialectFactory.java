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
            String prefix = config.getLimitPrefix();
            String suffix = config.getLimitSuffix();

            if (prefix != null && !prefix.isBlank()) {
                // Insert after SELECT [DISTINCT]
                String trimmed = sql.trim();
                if (trimmed.endsWith(";")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
                }
                String upper = trimmed.toUpperCase();
                if (upper.startsWith("SELECT DISTINCT ")) {
                    return trimmed.substring(0, "SELECT DISTINCT ".length())
                            + prefix.replace("{n}", String.valueOf(maxRows))
                            + trimmed.substring("SELECT DISTINCT ".length());
                } else if (upper.startsWith("SELECT ")) {
                    return "SELECT "
                            + prefix.replace("{n}", String.valueOf(maxRows))
                            + trimmed.substring("SELECT ".length());
                }
                return null; // Cannot insert prefix
            }

            if (suffix != null && !suffix.isBlank()) {
                String trimmed = sql.trim();
                if (trimmed.endsWith(";")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
                }
                return trimmed + suffix.replace("{n}", String.valueOf(maxRows));
            }

            return null;
        }

        @Override
        public boolean hasLimit(String sql) {
            String pattern = config.getLimitPattern();
            if (pattern != null && !pattern.isBlank()) {
                try {
                    return java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(sql).find();
                } catch (java.util.regex.PatternSyntaxException e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String getDatabaseLabel() {
            String label = config.getDatabaseLabel();
            return (label != null && !label.isBlank()) ? label : "Database";
        }

        @Override
        public java.util.List<String[]> listDatabases(java.sql.Connection conn) throws Exception {
            String method = config.getListDatabasesMethod();
            if ("schemas".equalsIgnoreCase(method)) {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                java.util.List<String[]> results = new java.util.ArrayList<>();
                try (java.sql.ResultSet rs = meta.getSchemas()) {
                    while (rs.next()) {
                        String schema = rs.getString("TABLE_SCHEM");
                        if (schema != null && !isSystemSchema(schema, config.getSystemSchemaFilter())) {
                            results.add(new String[]{schema});
                        }
                    }
                }
                return results;
            }
            // Default: use catalogs (same as Dialect interface default)
            return Dialect.super.listDatabases(conn);
        }

        private boolean isSystemSchema(String schema, String filter) {
            if (filter == null || filter.isBlank()) return false;
            for (String entry : filter.split(",")) {
                entry = entry.trim();
                if (entry.isEmpty()) continue;
                if (entry.endsWith("%")) {
                    if (schema.toUpperCase().startsWith(entry.substring(0, entry.length() - 1).toUpperCase())) {
                        return true;
                    }
                } else {
                    if (schema.equalsIgnoreCase(entry)) return true;
                }
            }
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
