package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    @Override
    public String getDatabaseLabel() {
        return "Schema";
    }

    @Override
    public List<String[]> listDatabases(Connection conn) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        List<String[]> results = new ArrayList<>();
        Set<String> systemPrefixes = Set.of("SYS", "DBMS_", "OUTLN", "XDB", "MDSYS", "WMSYS",
                "OLAPSYS", "ORDSYS", "CTXSYS", "FLOWS_FILES", "APPQOSSYS");
        Set<String> systemExact = Set.of("APEX_PUBLIC_USER", "APEX_030200", "APEX_040000");
        try (ResultSet rs = meta.getSchemas()) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (schema != null) {
                    boolean isSystem = false;
                    for (String prefix : systemPrefixes) {
                        if (schema.startsWith(prefix)) { isSystem = true; break; }
                    }
                    if (!isSystem) isSystem = systemExact.contains(schema.toUpperCase());
                    if (!isSystem) results.add(new String[]{schema});
                }
            }
        }
        return results;
    }
}
