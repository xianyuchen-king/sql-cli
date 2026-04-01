package com.sqlcli.executor;

import com.sqlcli.output.AgentJsonFormatter;
import com.sqlcli.output.CsvFormatter;
import com.sqlcli.output.JsonFormatter;
import com.sqlcli.output.MarkdownFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryExecutorTest {

    private final QueryExecutor executor = new QueryExecutor();

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private ResultSetMetaData metaData;

    @BeforeEach
    void setUp() {
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);
        metaData = mock(ResultSetMetaData.class);
    }

    private void setupSingleRow() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(42);
    }

    private void setupEmptyResult() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(resultSet.next()).thenReturn(false);
    }

    @Test
    void markdownFormatter_summaryBeforeTable() throws Exception {
        setupSingleRow();
        String result = executor.execute(connection, "SELECT id FROM t", new MarkdownFormatter(), 500, 0);

        // Summary line should appear before the markdown table
        String summaryPrefix = "1 row in set (";
        int summaryIndex = result.indexOf(summaryPrefix);
        int tableHeaderIndex = result.indexOf("| id |");

        assertTrue(summaryIndex >= 0, "Summary should be present");
        assertTrue(tableHeaderIndex >= 0, "Table header should be present");
        assertTrue(summaryIndex < tableHeaderIndex,
                "Summary should appear BEFORE the table in MarkdownFormatter output");
    }

    @Test
    void csvFormatter_summaryAfterData() throws Exception {
        setupSingleRow();
        String result = executor.execute(connection, "SELECT id FROM t", new CsvFormatter(), 500, 0);

        // CSV data should appear before the summary
        int csvHeaderIndex = result.indexOf("id");
        int summaryPrefixIndex = result.indexOf("1 row in set (");

        assertTrue(csvHeaderIndex >= 0, "CSV header should be present");
        assertTrue(summaryPrefixIndex >= 0, "Summary should be present");
        assertTrue(csvHeaderIndex < summaryPrefixIndex,
                "CSV data should appear BEFORE the summary in CsvFormatter output");
    }

    @Test
    void agentJsonFormatter_noPlaintextSummary() throws Exception {
        setupSingleRow();
        String result = executor.execute(connection, "SELECT id FROM t", new AgentJsonFormatter(), 500, 0);

        // Should be a JSON envelope, no plaintext "X row(s) in set" suffix
        assertTrue(result.startsWith("{"), "Should start with JSON object");
        assertTrue(result.contains("\"status\": \"ok\""), "Should contain status field");
        assertTrue(result.contains("\"elapsed_seconds\""), "Should contain timing in JSON");
        // The plaintext summary pattern should NOT appear as free text
        assertFalse(result.matches(".*\\d+ row[s]? in set \\(.*\\)$"),
                "Should not have plaintext summary suffix");
    }

    @Test
    void jsonFormatter_noSummary() throws Exception {
        setupSingleRow();
        String result = executor.execute(connection, "SELECT id FROM t", new JsonFormatter(), 500, 0);

        // Should be a plain JSON array, no plaintext summary
        assertTrue(result.startsWith("["), "Should start with JSON array");
        assertFalse(result.contains("row in set"),
                "JsonFormatter output should not contain plaintext summary");
    }

    @Test
    void zeroRows_singularForm() throws Exception {
        setupEmptyResult();
        String result = executor.execute(connection, "SELECT id FROM t", new MarkdownFormatter(), 500, 0);

        assertTrue(result.contains("0 rows in set ("),
                "Should say '0 rows' (plural)");
    }

    @Test
    void oneRow_singularForm() throws Exception {
        setupSingleRow();
        String result = executor.execute(connection, "SELECT id FROM t", new MarkdownFormatter(), 500, 0);

        assertTrue(result.contains("1 row in set ("),
                "Should say '1 row' (singular, no 's')");
        assertFalse(result.contains("1 rows in set ("),
                "Should not say '1 rows'");
    }
}
