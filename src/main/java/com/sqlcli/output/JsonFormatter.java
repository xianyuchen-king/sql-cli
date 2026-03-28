package com.sqlcli.output;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonFormatter implements OutputFormatter {

    @Override
    public String formatQuery(List<String> columns, List<List<Object>> rows) {
        return toJsonArray(columns, rows);
    }

    @Override
    public String formatTable(List<String> columns, List<List<Object>> rows) {
        return toJsonArray(columns, rows);
    }

    private String toJsonArray(List<String> columns, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n  {");
            List<Object> row = rows.get(i);
            for (int j = 0; j < columns.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("\n    \"").append(escape(columns.get(j))).append("\": ");
                Object val = j < row.size() ? row.get(j) : null;
                sb.append(valueToJson(val));
            }
            sb.append("\n  }");
        }
        if (!rows.isEmpty()) sb.append("\n");
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
