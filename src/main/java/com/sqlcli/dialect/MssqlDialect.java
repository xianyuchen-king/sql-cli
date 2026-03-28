package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

import java.util.Map;

public class MssqlDialect implements Dialect {

    @Override
    public String buildUrl(ConnectionConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:sqlserver://").append(config.getHost());
        if (config.getPort() != null) {
            sb.append(":").append(config.getPort());
        } else {
            sb.append(":").append(getDefaultPort());
        }
        if (config.getDb() != null && !config.getDb().isBlank()) {
            sb.append(";databaseName=").append(config.getDb());
        }
        if (config.getParams() != null && !config.getParams().isEmpty()) {
            for (Map.Entry<String, String> entry : config.getParams().entrySet()) {
                sb.append(";").append(entry.getKey()).append("=").append(entry.getValue());
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
        String upper = trimmed.toUpperCase();
        if (!upper.startsWith("SELECT")) {
            return null;
        }
        // SELECT DISTINCT → keep as is, add TOP after DISTINCT
        if (upper.startsWith("SELECT DISTINCT")) {
            return trimmed.substring(0, "SELECT DISTINCT".length())
                    + " TOP " + maxRows
                    + trimmed.substring("SELECT DISTINCT".length());
        }
        return "SELECT TOP " + maxRows + trimmed.substring("SELECT".length());
    }

    @Override
    public boolean hasLimit(String sql) {
        String upper = sql.toUpperCase();
        return upper.contains(" TOP ") || upper.matches("(?s).*\\bTOP\\s+\\d+.*");
    }

    @Override
    public int getDefaultPort() { return 1433; }

    @Override
    public String getDefaultDriverClass() { return "com.microsoft.sqlserver.jdbc.SQLServerDriver"; }
}
