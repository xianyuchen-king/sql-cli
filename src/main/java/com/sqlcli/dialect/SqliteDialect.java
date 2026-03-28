package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

public class SqliteDialect implements Dialect {

    @Override
    public String buildUrl(ConnectionConfig config) {
        if (config.getDb() != null && !config.getDb().isBlank()) {
            return "jdbc:sqlite:" + config.getDb();
        }
        throw new IllegalArgumentException("SQLite requires a database file path (--db)");
    }

    @Override
    public String wrapLimit(String sql, int maxRows) {
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed + " LIMIT " + maxRows;
    }

    @Override
    public boolean hasLimit(String sql) {
        return sql.toUpperCase().contains(" LIMIT ");
    }

    @Override
    public int getDefaultPort() { return 0; }

    @Override
    public String getDefaultDriverClass() { return "org.sqlite.JDBC"; }
}
