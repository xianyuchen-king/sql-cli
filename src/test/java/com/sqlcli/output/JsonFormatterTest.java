package com.sqlcli.output;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class JsonFormatterTest {

    private final JsonFormatter formatter = new JsonFormatter();

    @Test
    void formatSimple() {
        List<String> cols = List.of("id", "name");
        List<List<Object>> rows = List.of(
                List.of(1, "Alice"),
                List.of(2, "Bob")
        );

        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.startsWith("["));
        assertTrue(result.contains("\"id\": 1"));
        assertTrue(result.contains("\"name\": \"Alice\""));
        assertTrue(result.endsWith("]"));
    }

    @Test
    void formatWithNull() {
        List<String> cols = List.of("id", "value");
        List<Object> row = new ArrayList<>();
        row.add(1);
        row.add(null);
        List<List<Object>> rows = List.of(row);

        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains("\"value\": null"));
    }
}
