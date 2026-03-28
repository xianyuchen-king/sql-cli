package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

import java.util.Map;

public class PostgresqlDialect implements Dialect {

    @Override
    public String buildUrl(ConnectionConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:postgresql://").append(config.getHost());
        if (config.getPort() != null) {
            sb.append(":").append(config.getPort());
        } else {
            sb.append(":").append(getDefaultPort());
        }
        if (config.getDb() != null && !config.getDb().isBlank()) {
            sb.append("/").append(config.getDb());
        }
        if (config.getParams() != null && !config.getParams().isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : config.getParams().entrySet()) {
                if (!first) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        return sb.toString();
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
        String upper = sql.toUpperCase();
        return upper.contains(" LIMIT ") || upper.contains(" FETCH FIRST ");
    }

    @Override
    public int getDefaultPort() { return 5432; }

    @Override
    public String getDefaultDriverClass() { return "org.postgresql.Driver"; }
}
