package com.sqlcli.output;

import java.util.List;
import java.util.stream.Collectors;

public class MarkdownFormatter implements OutputFormatter {

    static final int MAX_COLUMN_WIDTH = 50;

    @Override
    public String formatQuery(List<String> columns, List<List<Object>> rows) {
        return formatTable(columns, rows);
    }

    @Override
    public String formatTable(List<String> columns, List<List<Object>> rows) {
        if (columns == null || columns.isEmpty()) return "(no results)";

        // Calculate column widths
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = truncate(columns.get(i)).length();
        }
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size() && i < widths.length; i++) {
                int len = row.get(i) == null ? 4 : truncate(row.get(i).toString()).length();
                if (len > widths[i]) widths[i] = len;
            }
        }

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("| ");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(padRight(truncate(columns.get(i)), widths[i])).append(" | ");
        }
        sb.append("\n");

        // Separator
        sb.append("|");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append("|");
        }
        sb.append("\n");

        // Rows
        for (List<Object> row : rows) {
            sb.append("| ");
            for (int i = 0; i < columns.size(); i++) {
                Object val = i < row.size() ? row.get(i) : null;
                String str = val == null ? "null" : val.toString();
                sb.append(padRight(truncate(str), widths[i])).append(" | ");
            }
            sb.append("\n");
        }

        return sb.toString().stripTrailing();
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_COLUMN_WIDTH) return s;
        return s.substring(0, MAX_COLUMN_WIDTH - 3) + "...";
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}
