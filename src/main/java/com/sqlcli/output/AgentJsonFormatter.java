package com.sqlcli.output;

import java.util.List;

/**
 * Structured JSON formatter for Agent consumption.
 * Wraps all output in a JSON envelope with status, data, and meta fields.
 */
public class AgentJsonFormatter implements OutputFormatter {

    @Override
    public String formatQuery(List<String> columns, List<List<Object>> rows) {
        return formatQueryEnvelope(columns, rows, -1);
    }

    @Override
    public String formatTable(List<String> columns, List<List<Object>> rows) {
        return formatTableEnvelope(columns, rows);
    }

    /**
     * Format query results with timing information in a structured envelope.
     */
    public String formatQueryEnvelope(List<String> columns, List<List<Object>> rows, double elapsedSeconds) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"ok\",\n");
        sb.append("  \"data\": ");
        sb.append(buildDataArray(columns, rows));
        sb.append(",\n");

        // meta
        sb.append("  \"meta\": {\n");
        sb.append("    \"columns\": ");
        sb.append(buildJsonArray(columns));
        sb.append(",\n");
        sb.append("    \"row_count\": ").append(rows.size());
        if (elapsedSeconds >= 0) {
            sb.append(",\n");
            sb.append("    \"elapsed_seconds\": ").append(String.format("%.2f", elapsedSeconds));
        }
        sb.append("\n  }\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Format table (metadata) results in a structured envelope.
     */
    public String formatTableEnvelope(List<String> columns, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"ok\",\n");
        sb.append("  \"data\": ");
        sb.append(buildDataArray(columns, rows));
        sb.append(",\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"columns\": ");
        sb.append(buildJsonArray(columns));
        sb.append(",\n");
        sb.append("    \"row_count\": ").append(rows.size());
        sb.append("\n  }\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Build a JSON array of row objects.
     */
    private String buildDataArray(List<String> columns, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n    {");
            List<Object> row = rows.get(i);
            for (int j = 0; j < columns.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("\n      \"").append(escape(columns.get(j))).append("\": ");
                Object val = j < row.size() ? row.get(j) : null;
                sb.append(valueToJson(val));
            }
            sb.append("\n    }");
        }
        if (!rows.isEmpty()) sb.append("\n  ");
        sb.append("]");
        return sb.toString();
    }

    /**
     * Build a JSON array from string list.
     */
    private String buildJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escape(items.get(i))).append("\"");
        }
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
}
