package com.sqlcli.output;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public interface OutputFormatter {

    String formatQuery(List<String> columns, List<List<Object>> rows);

    String formatTable(List<String> columns, List<List<Object>> rows);

    enum Format {
        MARKDOWN, JSON, CSV
    }

    static OutputFormatter create(Format format) {
        return switch (format) {
            case JSON -> new JsonFormatter();
            case CSV -> new CsvFormatter();
            default -> new MarkdownFormatter();
        };
    }

    static OutputFormatter create(String formatName) {
        if (formatName == null) return new MarkdownFormatter();
        return switch (formatName.toLowerCase()) {
            case "json" -> new JsonFormatter();
            case "csv" -> new CsvFormatter();
            default -> new MarkdownFormatter();
        };
    }
}
