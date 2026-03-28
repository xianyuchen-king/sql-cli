package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

public class GenericDialect implements Dialect {

    @Override
    public String buildUrl(ConnectionConfig config) {
        // Generic dialect doesn't build URLs - user must provide --url directly
        if (config.getUrl() != null && !config.getUrl().isBlank()) {
            return config.getUrl();
        }
        throw new IllegalArgumentException(
                "Generic database type requires a direct JDBC URL (--url). "
                        + "Or register a custom type with 'sql-cli conn register-type'.");
    }

    @Override
    public String wrapLimit(String sql, int maxRows) {
        // Don't attempt to add LIMIT for unknown databases
        return null;
    }

    @Override
    public boolean hasLimit(String sql) {
        // Assume no limit for generic databases
        return false;
    }

    @Override
    public int getDefaultPort() { return 0; }

    @Override
    public String getDefaultDriverClass() { return null; }
}
