package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface Dialect {

    /**
     * Build JDBC URL from simplified connection parameters.
     * Returns null if this dialect doesn't support URL building (GenericDialect).
     */
    String buildUrl(ConnectionConfig config);

    /**
     * Append row limit clause to SQL.
     * Returns the modified SQL, or null if not supported.
     */
    String wrapLimit(String sql, int maxRows);

    /**
     * Check if SQL already contains a row limit clause.
     */
    boolean hasLimit(String sql);

    /**
     * Get default port for this database type.
     */
    int getDefaultPort();

    /**
     * Get default driver class name for this database type.
     */
    String getDefaultDriverClass();

    /**
     * Get the label for "database" in this dialect's terminology.
     * Oracle uses "Schema", most others use "Database".
     */
    default String getDatabaseLabel() {
        return "Database";
    }

    /**
     * List databases (or schemas) accessible via this connection.
     * Default implementation uses DatabaseMetaData.getCatalogs().
     * Dialects like Oracle override this to use getSchemas() instead.
     */
    default List<String[]> listDatabases(Connection conn) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        List<String[]> results = new ArrayList<>();
        try (ResultSet rs = meta.getCatalogs()) {
            while (rs.next()) {
                String catalog = rs.getString("TABLE_CAT");
                if (catalog != null) {
                    results.add(new String[]{catalog});
                }
            }
        }
        return results;
    }

    /**
     * Table metadata record.
     */
    record TableMeta(String name, String type, String schema, String remark) {}
    record ColumnMeta(String name, String type, boolean nullable, String defaultValue,
                      boolean isPrimaryKey, boolean isAutoIncrement, String remark) {}
    record IndexMeta(String name, String tableName, List<String> columns, boolean isUnique) {}
}
