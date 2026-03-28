package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

import java.util.Map;

public class OracleDialect implements Dialect {

    @Override
    public String buildUrl(ConnectionConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:oracle:thin:@//");
        sb.append(config.getHost());
        if (config.getPort() != null) {
            sb.append(":").append(config.getPort());
        } else {
            sb.append(":").append(getDefaultPort());
        }
        if (config.getDb() != null && !config.getDb().isBlank()) {
            sb.append("/").append(config.getDb());
        }
        return sb.toString();
    }

    @Override
    public String wrapLimit(String sql, int maxRows) {
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed + " FETCH FIRST " + maxRows + " ROWS ONLY";
    }

    @Override
    public boolean hasLimit(String sql) {
        String upper = sql.toUpperCase();
        return upper.contains(" FETCH FIRST ") || upper.contains(" ROWNUM ");
    }

    @Override
    public int getDefaultPort() { return 1521; }

    @Override
    public String getDefaultDriverClass() { return "oracle.jdbc.OracleDriver"; }
}
