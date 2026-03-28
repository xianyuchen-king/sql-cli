package com.sqlcli.output;

import java.util.List;

public class CsvFormatter implements OutputFormatter {

    @Override
    public String formatQuery(List<String> columns, List<List<Object>> rows) {
        return formatTable(columns, rows);
    }

    @Override
    public String formatTable(List<String> columns, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(String.join(",", columns.stream().map(this::escapeCsv).toList()));
        sb.append("\n");

        // Rows
        for (List<Object> row : rows) {
            sb.append(row.stream()
                    .map(v -> escapeCsv(v == null ? "" : v.toString()))
                    .collect(java.util.stream.Collectors.joining(",")));
            sb.append("\n");
        }

        return sb.toString().stripTrailing();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
