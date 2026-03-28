package com.sqlcli.output;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class MarkdownFormatterTest {

    private final MarkdownFormatter formatter = new MarkdownFormatter();

    @Test
    void formatEmpty() {
        assertEquals("(no results)", formatter.formatQuery(List.of(), List.of()));
    }

    @Test
    void formatSimpleTable() {
        List<String> cols = List.of("id", "name");
        List<List<Object>> rows = List.of(
                List.of(1, "Alice"),
                List.of(2, "Bob")
        );

        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains("id") && result.contains("name"));
        assertTrue(result.contains("1") && result.contains("Alice"));
        assertTrue(result.contains("2") && result.contains("Bob"));
    }

    @Test
    void formatWithNulls() {
        List<String> cols = List.of("id", "email");
        List<Object> row = new ArrayList<>();
        row.add(1);
        row.add(null);
        List<List<Object>> rows = List.of(row);

        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains("null"));
    }
}
