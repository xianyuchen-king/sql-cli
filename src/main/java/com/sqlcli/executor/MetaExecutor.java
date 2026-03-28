package com.sqlcli.executor;

import com.sqlcli.dialect.Dialect;
import com.sqlcli.output.OutputFormatter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MetaExecutor {

    /**
     * List all databases/schemas.
     */
    public String listDatabases(Connection conn, OutputFormatter formatter) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getCatalogs()) {
            return resultSetToTable(rs, formatter, List.of("Database"));
        }
    }

    /**
     * List all tables in a schema/database.
     */
    public String listTables(Connection conn, String schema, OutputFormatter formatter) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(schema, null, "%", new String[]{"TABLE"})) {
            return resultSetToTable(rs, formatter, List.of("Table", "Schema", "Remarks"));
        }
    }

    /**
     * List all views.
     */
    public String listViews(Connection conn, String schema, OutputFormatter formatter) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(schema, null, "%", new String[]{"VIEW"})) {
            return resultSetToTable(rs, formatter, List.of("View", "Schema", "Remarks"));
        }
    }

    /**
     * List all columns of a table.
     */
    public String listColumns(Connection conn, String schema, String table, OutputFormatter formatter)
            throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(schema, null, table, "%")) {
            return resultSetToTable(rs, formatter,
                    List.of("Column", "Type", "Size", "Nullable", "DefaultValue", "Remark"));
        }
    }

    /**
     * List all indexes of a table.
     */
    public String listIndexes(Connection conn, String schema, String table, OutputFormatter formatter)
            throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getIndexInfo(schema, null, table, false, false)) {
            return resultSetToTable(rs, formatter,
                    List.of("Index", "Column", "NonUnique", "Type", "AscOrDesc"));
        }
    }

    private String resultSetToTable(ResultSet rs, OutputFormatter formatter, List<String> displayColumns)
            throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }

        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }

        if (rows.isEmpty()) return "(no results)";
        return formatter.formatTable(columns, rows);
    }
}
