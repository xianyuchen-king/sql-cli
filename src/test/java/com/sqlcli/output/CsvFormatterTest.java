package com.sqlcli.output;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CsvFormatterTest {

    private final CsvFormatter formatter = new CsvFormatter();

    @Test
    void formatSimple() {
        List<String> cols = List.of("id", "name");
        List<List<Object>> rows = List.of(
                List.of(1, "Alice"),
                List.of(2, "Bob")
        );

        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.startsWith("id,name\n"));
        assertTrue(result.contains("1,Alice"));
        assertTrue(result.contains("2,Bob"));
    }

    @Test
    void formatWithComma() {
        List<String> cols = List.of("name");
        List<List<Object>> rows = List.of(List.of("Alice, Bob"));

        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains("\"Alice, Bob\""));
    }
}
