package com.sqlcli.dialect;

import com.sqlcli.config.ConnectionConfig;

import java.sql.Connection;
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
     * Table metadata record.
     */
    record TableMeta(String name, String type, String schema, String remark) {}
    record ColumnMeta(String name, String type, boolean nullable, String defaultValue,
                      boolean isPrimaryKey, boolean isAutoIncrement, String remark) {}
    record IndexMeta(String name, String tableName, List<String> columns, boolean isUnique) {}
}
