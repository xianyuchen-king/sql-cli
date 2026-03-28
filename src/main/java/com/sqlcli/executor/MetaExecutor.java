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

    /**
     * Describe a table: columns with types, primary keys, indexes, foreign keys.
     */
    public String describeTable(Connection conn, String schema, String table, OutputFormatter formatter)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        DatabaseMetaData meta = conn.getMetaData();

        // Columns
        sb.append(formatter.formatTable(List.of("Column"), List.of(List.of("---"))));
        List<String> colNames = List.of("Column", "Type", "Nullable", "Default", "PK");
        List<List<Object>> colRows = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(schema, null, table, "%")) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                String colType = rs.getString("TYPE_NAME");
                int colSize = rs.getInt("COLUMN_SIZE");
                String nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? "YES" : "NO";
                String defVal = rs.getString("COLUMN_DEF");
                colRows.add(List.of(colName, colType + "(" + colSize + ")", nullable,
                        defVal != null ? defVal : "", ""));
            }
        }

        // Mark primary keys
        try (ResultSet rs = meta.getPrimaryKeys(schema, null, table)) {
            while (rs.next()) {
                String pkCol = rs.getString("COLUMN_NAME");
                for (int i = 0; i < colRows.size(); i++) {
                    if (colRows.get(i).get(0).equals(pkCol)) {
                        List<Object> row = new ArrayList<>(colRows.get(i));
                        row.set(4, "PK");
                        colRows.set(i, row);
                    }
                }
            }
        }

        sb.append("Columns:\n");
        sb.append(formatter.formatTable(colNames, colRows)).append("\n");

        // Indexes
        List<String> idxNames = List.of("Index", "Column", "Unique", "Type");
        List<List<Object>> idxRows = new ArrayList<>();
        try (ResultSet rs = meta.getIndexInfo(schema, null, table, false, false)) {
            while (rs.next()) {
                idxRows.add(List.of(
                        rs.getString("INDEX_NAME") != null ? rs.getString("INDEX_NAME") : "(primary)",
                        rs.getString("COLUMN_NAME") != null ? rs.getString("COLUMN_NAME") : "",
                        !rs.getBoolean("NON_UNIQUE") ? "YES" : "NO",
                        rs.getShort("TYPE") == DatabaseMetaData.tableIndexClustered ? "CLUSTERED" : "OTHER"
                ));
            }
        }
        if (!idxRows.isEmpty()) {
            sb.append("Indexes:\n");
            sb.append(formatter.formatTable(idxNames, idxRows)).append("\n");
        }

        // Foreign keys
        List<String> fkNames = List.of("FK Name", "Column", "Ref Table", "Ref Column");
        List<List<Object>> fkRows = new ArrayList<>();
        try (ResultSet rs = meta.getImportedKeys(schema, null, table)) {
            while (rs.next()) {
                fkRows.add(List.of(
                        rs.getString("FK_NAME") != null ? rs.getString("FK_NAME") : "(unnamed)",
                        rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKTABLE_NAME"),
                        rs.getString("PKCOLUMN_NAME")
                ));
            }
        }
        if (!fkRows.isEmpty()) {
            sb.append("Foreign Keys:\n");
            sb.append(formatter.formatTable(fkNames, fkRows)).append("\n");
        }

        return sb.toString().trim();
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
