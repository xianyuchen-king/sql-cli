package com.sqlcli.executor;

import com.sqlcli.dialect.Dialect;
import com.sqlcli.output.AgentJsonFormatter;
import com.sqlcli.output.OutputFormatter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        DatabaseMetaData meta = conn.getMetaData();

        // Collect columns
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

        // Collect indexes
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

        // Collect foreign keys
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

        // For structured JSON output, build a single combined envelope
        if (formatter instanceof AgentJsonFormatter) {
            return buildDescribeJson(table, colNames, colRows, idxNames, idxRows, fkNames, fkRows);
        }

        // For plaintext/markdown output, use section headers
        StringBuilder sb = new StringBuilder();
        sb.append("Columns:\n");
        sb.append(formatter.formatTable(colNames, colRows)).append("\n");
        if (!idxRows.isEmpty()) {
            sb.append("Indexes:\n");
            sb.append(formatter.formatTable(idxNames, idxRows)).append("\n");
        }
        if (!fkRows.isEmpty()) {
            sb.append("Foreign Keys:\n");
            sb.append(formatter.formatTable(fkNames, fkRows)).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Build a single JSON envelope for describe output with all sections combined.
     */
    private String buildDescribeJson(String table, List<String> colNames, List<List<Object>> colRows,
                                      List<String> idxNames, List<List<Object>> idxRows,
                                      List<String> fkNames, List<List<Object>> fkRows) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"ok\",\n");
        sb.append("  \"data\": {\n");
        sb.append("    \"table\": \"").append(escape(table)).append("\",\n");
        sb.append("    \"columns\": ").append(buildSectionArray(colNames, colRows));
        if (!idxRows.isEmpty()) {
            sb.append(",\n    \"indexes\": ").append(buildSectionArray(idxNames, idxRows));
        }
        if (!fkRows.isEmpty()) {
            sb.append(",\n    \"foreign_keys\": ").append(buildSectionArray(fkNames, fkRows));
        }
        sb.append("\n  },\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"column_count\": ").append(colRows.size());
        sb.append(",\n    \"index_count\": ").append(idxRows.size());
        sb.append(",\n    \"fk_count\": ").append(fkRows.size());
        sb.append("\n  }\n");
        sb.append("}");
        return sb.toString();
    }

    private String buildSectionArray(List<String> columns, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n      {");
            List<Object> row = rows.get(i);
            for (int j = 0; j < columns.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("\n        \"").append(escape(columns.get(j))).append("\": ");
                Object val = j < row.size() ? row.get(j) : null;
                sb.append(valueToJson(val));
            }
            sb.append("\n      }");
        }
        if (!rows.isEmpty()) sb.append("\n    ");
        sb.append("]");
        return sb.toString();
    }

    private String valueToJson(Object val) {
        if (val == null) return "null";
        if (val instanceof Number) return val.toString();
        if (val instanceof Boolean) return val.toString();
        return "\"" + escape(val.toString()) + "\"";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
