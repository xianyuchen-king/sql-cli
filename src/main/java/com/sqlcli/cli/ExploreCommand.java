package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.dialect.Dialect;
import com.sqlcli.dialect.DialectFactory;
import com.sqlcli.output.AgentJsonFormatter;
import com.sqlcli.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * Explore database structure in a single call.
 * Without -s: overview mode (schemas/databases + table counts).
 * With -s: detail mode (tables + columns for a specific schema).
 */
@Command(name = "explore", description = "Explore database structure (schemas, tables, columns)")
public class ExploreCommand implements Runnable {

    @Mixin
    private MetaConnectionMixin opts = new MetaConnectionMixin();

    @Option(names = {"-s", "--schema"}, description = "Schema name to explore in detail")
    private String schema;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        String outputFormat = opts.resolveFormat(cm);

        ConnectionConfig inlineConfig = opts.buildInlineConfig();

        ConnectionConfig resolved;
        try {
            resolved = connMgr.resolveConnection(opts.getConnection(), inlineConfig);
        } catch (Exception e) {
            CliErrorHandler.handleError(e, outputFormat);
            return;
        }

        try (Connection conn = connMgr.connect(resolved)) {
            Dialect dialect = DialectFactory.getDialect(resolved.getType(), cm.load());
            DatabaseMetaData meta = conn.getMetaData();
            OutputFormatter formatter = OutputFormatter.create(outputFormat);

            if (schema != null && !schema.isBlank()) {
                System.out.println(exploreSchema(meta, schema, formatter, outputFormat));
            } else {
                System.out.println(exploreOverview(conn, meta, dialect, formatter, outputFormat));
            }
        } catch (Exception e) {
            CliErrorHandler.handleError(e, outputFormat);
        }
    }

    // --- Overview mode: list schemas with table counts ---

    String exploreOverview(Connection conn, DatabaseMetaData meta, Dialect dialect,
                            OutputFormatter formatter, String outputFormat) throws Exception {
        List<String[]> dbRows = dialect.listDatabases(conn);
        String label = dialect.getDatabaseLabel();

        List<String[]> tableData = new ArrayList<>();
        int totalCount = dbRows.size();
        int limit = 50;

        for (String[] row : dbRows) {
            String schemaName = row[0];
            if (tableData.size() < limit) {
                tableData.add(new String[]{schemaName, "-"});
            }
        }

        if (formatter instanceof AgentJsonFormatter) {
            return buildOverviewAgentJson(tableData, totalCount, limit, label);
        }

        // Markdown / plaintext output
        List<String> columns = List.of(label);
        List<List<Object>> rows = tableData.stream()
                .map(r -> List.<Object>of(r[0]))
                .toList();
        StringBuilder sb = new StringBuilder();
        sb.append(formatter.formatTable(columns, rows));
        if (totalCount > limit) {
            sb.append("\n... and ").append(totalCount - limit).append(" more ").append(label.toLowerCase()).append("(s)");
        }
        return sb.toString().trim();
    }

    // --- Detail mode: tables + columns for a specific schema ---

    String exploreSchema(DatabaseMetaData meta, String schemaName,
                          OutputFormatter formatter, String outputFormat) throws Exception {
        List<String[]> tableData = new ArrayList<>();
        try (java.sql.ResultSet rs = meta.getTables(schemaName, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String remarks = rs.getString("REMARKS");

                List<String[]> colData = new ArrayList<>();
                try (java.sql.ResultSet colRs = meta.getColumns(schemaName, null, tableName, "%")) {
                    while (colRs.next()) {
                        colData.add(new String[]{
                                colRs.getString("COLUMN_NAME"),
                                colRs.getString("TYPE_NAME"),
                                colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? "YES" : "NO"
                        });
                    }
                }

                tableData.add(new String[]{tableName, remarks != null ? remarks : "",
                        String.valueOf(colData.size()), serializeColumns(colData)});
            }
        }

        if (formatter instanceof AgentJsonFormatter) {
            return buildSchemaAgentJson(schemaName, tableData);
        }

        // Markdown / plaintext output
        StringBuilder sb = new StringBuilder();
        sb.append("Schema: ").append(schemaName).append("\n\n");
        for (String[] row : tableData) {
            sb.append("### ").append(row[0]);
            if (!row[1].isEmpty()) sb.append(" — ").append(row[1]);
            sb.append("\n\n");

            String colsJson = row[3];
            if (!colsJson.equals("[]") && !colsJson.isEmpty()) {
                List<String> colHeaders = List.of("Column", "Type", "Nullable");
                List<List<Object>> colRows = parseColumnsJson(colsJson);
                sb.append(formatter.formatTable(colHeaders, colRows)).append("\n\n");
            }
        }
        if (tableData.isEmpty()) {
            sb.append("(no tables found)\n");
        }
        return sb.toString().trim();
    }

    // --- Helper methods ---

    int countTables(DatabaseMetaData meta, String schema) throws Exception {
        int count = 0;
        try (java.sql.ResultSet rs = meta.getTables(schema, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) count++;
        }
        return count;
    }

    // --- JSON builders ---

    private String buildOverviewAgentJson(List<String[]> tableData, int totalCount,
                                           int limit, String label) {
        int displayCount = tableData.size();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"ok\",\n");
        sb.append("  \"data\": {\n");
        sb.append("    \"").append(label.toLowerCase()).append("s\": [");
        for (int i = 0; i < tableData.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n      {\"name\": \"").append(escape(tableData.get(i)[0])).append("\"}");
        }
        if (!tableData.isEmpty()) sb.append("\n    ");
        sb.append("]\n  },\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"").append(label.toLowerCase()).append("_count\": ").append(totalCount);
        if (totalCount > limit) {
            sb.append(",\n    \"displayed\": ").append(displayCount);
        }
        sb.append("\n  }\n");
        sb.append("}");
        return sb.toString();
    }

    private String buildSchemaAgentJson(String schemaName, List<String[]> tableData) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"ok\",\n");
        sb.append("  \"data\": {\n");
        sb.append("    \"schema\": \"").append(escape(schemaName)).append("\",\n");
        sb.append("    \"tables\": [");
        for (int i = 0; i < tableData.size(); i++) {
            String[] row = tableData.get(i);
            if (i > 0) sb.append(",");
            sb.append("\n      {\n");
            sb.append("        \"name\": \"").append(escape(row[0])).append("\",\n");
            sb.append("        \"remarks\": \"").append(escape(row[1])).append("\",\n");
            sb.append("        \"column_count\": ").append(row[2]).append(",\n");
            sb.append("        \"columns\": ").append(row[3]).append("\n");
            sb.append("      }");
        }
        if (!tableData.isEmpty()) sb.append("\n    ");
        sb.append("]\n  },\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"table_count\": ").append(tableData.size()).append("\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    String serializeColumns(List<String[]> cols) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n        {\"name\":\"").append(escape(cols.get(i)[0]))
              .append("\",\"type\":\"").append(escape(cols.get(i)[1]))
              .append("\",\"nullable\":\"").append(escape(cols.get(i)[2])).append("\"}");
        }
        if (!cols.isEmpty()) sb.append("\n      ");
        sb.append("]");
        return sb.toString();
    }

    List<List<Object>> parseColumnsJson(String json) {
        List<List<Object>> rows = new ArrayList<>();
        if (json == null || json.equals("[]") || json.isEmpty()) return rows;
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return rows;
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return rows;
        String[] objects = inner.split("\\},\\s*\\{");
        for (int i = 0; i < objects.length; i++) {
            String obj = objects[i];
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";
            String name = extractJsonValue(obj, "name");
            String type = extractJsonValue(obj, "type");
            String nullable = extractJsonValue(obj, "nullable");
            rows.add(List.of(name, type, nullable));
        }
        return rows;
    }

    String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end);
    }

    String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
