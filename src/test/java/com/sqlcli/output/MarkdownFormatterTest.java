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

    @Test
    void shortValueNotTruncated() {
        List<String> cols = List.of("col");
        List<List<Object>> rows = List.of(List.of("hello"));
        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains("hello"));
        assertFalse(result.contains("..."));
    }

    @Test
    void exactlyFiftyCharsNotTruncated() {
        String val = "a".repeat(50);
        List<String> cols = List.of("col");
        List<List<Object>> rows = List.of(List.of(val));
        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains(val));
        assertFalse(result.contains("..."));
    }

    @Test
    void fiftyOneCharsTruncated() {
        String val = "a".repeat(51);
        List<String> cols = List.of("col");
        List<List<Object>> rows = List.of(List.of(val));
        String result = formatter.formatQuery(cols, rows);
        String truncated = "a".repeat(47) + "...";
        assertTrue(result.contains(truncated));
        assertFalse(result.contains(val));
    }

    @Test
    void hundredCharsTruncated() {
        String val = "b".repeat(100);
        List<String> cols = List.of("col");
        List<List<Object>> rows = List.of(List.of(val));
        String result = formatter.formatQuery(cols, rows);
        String truncated = "b".repeat(47) + "...";
        assertTrue(result.contains(truncated));
        assertFalse(result.contains(val));
    }

    @Test
    void nullValueHandledCorrectly() {
        List<String> cols = List.of("col");
        List<Object> row = new ArrayList<>();
        row.add(null);
        List<List<Object>> rows = List.of(row);
        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains("null"));
        assertFalse(result.contains("..."));
    }

    @Test
    void columnHeaderTruncated() {
        String header = "x".repeat(60);
        List<String> cols = List.of(header);
        List<List<Object>> rows = List.of(List.of("short"));
        String result = formatter.formatQuery(cols, rows);
        String truncatedHeader = "x".repeat(47) + "...";
        assertTrue(result.contains(truncatedHeader));
    }

    @Test
    void separatorLineMatchesTruncatedWidth() {
        String longVal = "z".repeat(80);
        List<String> cols = List.of("col");
        List<List<Object>> rows = List.of(List.of(longVal));
        String result = formatter.formatQuery(cols, rows);
        // The truncated column value should be 50 chars (47 + "...")
        // The separator for that column should be 52 dashes (50 + 2 padding)
        String expectedSeparator = "-".repeat(52);
        assertTrue(result.contains(expectedSeparator));
    }

    @Test
    void fortySevenCharsNotTruncated() {
        // 47 chars should NOT be truncated because 47 + "..." = 50 = MAX_COLUMN_WIDTH
        String val = "c".repeat(47);
        List<String> cols = List.of("col");
        List<List<Object>> rows = List.of(List.of(val));
        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains(val));
        assertFalse(result.contains("..."));
    }

    @Test
    void fortyEightCharsNotTruncated() {
        // 48 chars should NOT be truncated because 48 <= MAX_COLUMN_WIDTH(50)
        String val = "d".repeat(48);
        List<String> cols = List.of("col");
        List<List<Object>> rows = List.of(List.of(val));
        String result = formatter.formatQuery(cols, rows);
        assertTrue(result.contains(val));
        assertFalse(result.contains("..."));
    }
}
