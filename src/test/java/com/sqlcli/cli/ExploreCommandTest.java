package com.sqlcli.cli;

import com.sqlcli.dialect.Dialect;
import com.sqlcli.output.AgentJsonFormatter;
import com.sqlcli.output.OutputFormatter;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExploreCommandTest {

    private final ExploreCommand command = new ExploreCommand();

    // --- serializeColumns / parseColumnsJson round-trip ---

    @Test
    void serializeColumns_emptyList() {
        List<String[]> cols = List.<String[]>of();
        assertEquals("[]", command.serializeColumns(cols));
    }

    @Test
    void serializeColumns_singleColumn() {
        List<String[]> cols = List.<String[]>of(new String[]{"id", "INT", "NO"});
        String json = command.serializeColumns(cols);
        assertTrue(json.contains("\"name\":\"id\""));
        assertTrue(json.contains("\"type\":\"INT\""));
        assertTrue(json.contains("\"nullable\":\"NO\""));
    }

    @Test
    void serializeColumns_multipleColumns() {
        List<String[]> cols = List.<String[]>of(
                new String[]{"id", "INT", "NO"},
                new String[]{"name", "VARCHAR", "YES"}
        );
        String json = command.serializeColumns(cols);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("\"name\":\"id\""));
        assertTrue(json.contains("\"name\":\"name\""));
    }

    @Test
    void serializeColumns_escapeSpecialChars() {
        List<String[]> cols = List.<String[]>of(new String[]{"col\"name", "type\nwith\nnewline", "YES"});
        String json = command.serializeColumns(cols);
        assertTrue(json.contains("col\\\"name"));
        assertTrue(json.contains("type\\nwith\\nnewline"));
    }

    @Test
    void parseColumnsJson_emptyArray() {
        assertTrue(command.parseColumnsJson("[]").isEmpty());
    }

    @Test
    void parseColumnsJson_nullInput() {
        assertTrue(command.parseColumnsJson(null).isEmpty());
    }

    @Test
    void parseColumnsJson_emptyString() {
        assertTrue(command.parseColumnsJson("").isEmpty());
    }

    @Test
    void parseColumnsJson_singleColumn() {
        String json = "[{\"name\":\"id\",\"type\":\"INT\",\"nullable\":\"NO\"}]";
        List<List<Object>> rows = command.parseColumnsJson(json);
        assertEquals(1, rows.size());
        assertEquals("id", rows.get(0).get(0));
        assertEquals("INT", rows.get(0).get(1));
        assertEquals("NO", rows.get(0).get(2));
    }

    @Test
    void parseColumnsJson_multipleColumns() {
        String json = "[{\"name\":\"id\",\"type\":\"INT\",\"nullable\":\"NO\"},{\"name\":\"name\",\"type\":\"VARCHAR\",\"nullable\":\"YES\"}]";
        List<List<Object>> rows = command.parseColumnsJson(json);
        assertEquals(2, rows.size());
        assertEquals("id", rows.get(0).get(0));
        assertEquals("name", rows.get(1).get(0));
        assertEquals("VARCHAR", rows.get(1).get(1));
    }

    @Test
    void parseColumnsJson_roundTrip() {
        List<String[]> cols = List.<String[]>of(
                new String[]{"id", "INT", "NO"},
                new String[]{"name", "VARCHAR(255)", "YES"},
                new String[]{"created_at", "TIMESTAMP", "NO"}
        );
        String json = command.serializeColumns(cols);
        List<List<Object>> parsed = command.parseColumnsJson(json);
        assertEquals(3, parsed.size());
        assertEquals("id", parsed.get(0).get(0));
        assertEquals("VARCHAR(255)", parsed.get(1).get(1));
        assertEquals("NO", parsed.get(2).get(2));
    }

    // --- extractJsonValue ---

    @Test
    void extractJsonValue_found() {
        assertEquals("hello", command.extractJsonValue("{\"name\":\"hello\",\"type\":\"INT\"}", "name"));
        assertEquals("INT", command.extractJsonValue("{\"name\":\"hello\",\"type\":\"INT\"}", "type"));
    }

    @Test
    void extractJsonValue_notFound() {
        assertEquals("", command.extractJsonValue("{\"name\":\"hello\"}", "missing"));
    }

    @Test
    void extractJsonValue_emptyJson() {
        assertEquals("", command.extractJsonValue("{}", "name"));
    }

    // --- escape ---

    @Test
    void escape_null() {
        assertEquals("", command.escape(null));
    }

    @Test
    void escape_specialChars() {
        assertEquals("line1\\nline2", command.escape("line1\nline2"));
        assertEquals("tab\\there", command.escape("tab\there"));
        assertEquals("back\\\\slash", command.escape("back\\slash"));
        assertEquals("quote\\\"inside", command.escape("quote\"inside"));
    }

    // --- countTables ---

    @Test
    void countTables_zeroTables() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        ResultSet rs = mockEmptyResultSet();
        doReturn(rs).when(meta).getTables("SCHEMA1", null, "%", new String[]{"TABLE"});

        assertEquals(0, command.countTables(meta, "SCHEMA1"));
    }

    @Test
    void countTables_threeTables() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        ResultSet rs = mockResultSetWithRows(3);
        doReturn(rs).when(meta).getTables("HR", null, "%", new String[]{"TABLE"});

        assertEquals(3, command.countTables(meta, "HR"));
    }

    // --- exploreOverview: markdown format ---

    @Test
    void exploreOverview_markdownFormat() throws Exception {
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Dialect dialect = mock(Dialect.class);
        OutputFormatter formatter = OutputFormatter.create("markdown");

        when(dialect.listDatabases(conn)).thenReturn(List.of(
                new String[]{"HR"},
                new String[]{"SCOTT"}
        ));
        when(dialect.getDatabaseLabel()).thenReturn("Schema");

        // Overview mode no longer counts tables per schema (performance)
        String result = command.exploreOverview(conn, meta, dialect, formatter, "markdown");
        assertTrue(result.contains("HR"));
        assertTrue(result.contains("SCOTT"));
    }

    @Test
    void exploreOverview_markdownFormat_emptyDatabase() throws Exception {
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Dialect dialect = mock(Dialect.class);
        OutputFormatter formatter = OutputFormatter.create("markdown");

        when(dialect.listDatabases(conn)).thenReturn(List.of());
        when(dialect.getDatabaseLabel()).thenReturn("Database");

        String result = command.exploreOverview(conn, meta, dialect, formatter, "markdown");
        assertTrue(result.contains("no results") || result.contains("Database") || result.contains("Table Count"));
    }

    // --- exploreOverview: agent-json format ---

    @Test
    void exploreOverview_agentJsonFormat() throws Exception {
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Dialect dialect = mock(Dialect.class);
        OutputFormatter formatter = OutputFormatter.create("agent-json");

        when(dialect.listDatabases(conn)).thenReturn(List.of(
                new String[]{"HR"},
                new String[]{"SCOTT"}
        ));
        when(dialect.getDatabaseLabel()).thenReturn("Schema");

        // Overview mode no longer counts tables per schema (performance)
        String result = command.exploreOverview(conn, meta, dialect, formatter, "agent-json");
        assertTrue(result.contains("\"status\": \"ok\""));
        assertTrue(result.contains("\"data\""));
        assertTrue(result.contains("\"meta\""));
        assertTrue(result.contains("\"name\": \"HR\""));
        assertTrue(result.contains("\"name\": \"SCOTT\""));
        assertTrue(result.contains("\"schema_count\": 2"));
    }

    @Test
    void exploreOverview_agentJsonFormat_emptyDatabase() throws Exception {
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Dialect dialect = mock(Dialect.class);
        OutputFormatter formatter = OutputFormatter.create("agent-json");

        when(dialect.listDatabases(conn)).thenReturn(List.of());
        when(dialect.getDatabaseLabel()).thenReturn("Database");

        String result = command.exploreOverview(conn, meta, dialect, formatter, "agent-json");
        assertTrue(result.contains("\"status\": \"ok\""));
        assertTrue(result.contains("\"databases\": []"));
        assertTrue(result.contains("\"database_count\": 0"));
    }

    @Test
    void exploreOverview_agentJsonFormat_truncation() throws Exception {
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Dialect dialect = mock(Dialect.class);
        OutputFormatter formatter = OutputFormatter.create("agent-json");

        // Create 55 schemas to exceed the 50 limit
        List<String[]> schemas = new java.util.ArrayList<>();
        for (int i = 1; i <= 55; i++) {
            schemas.add(new String[]{"SCHEMA_" + i});
        }
        when(dialect.listDatabases(conn)).thenReturn(schemas);
        when(dialect.getDatabaseLabel()).thenReturn("Schema");

        // Each schema has 1 table — create mocks first
        java.util.Map<String, ResultSet> rsMap = new java.util.HashMap<>();
        for (int i = 1; i <= 55; i++) {
            rsMap.put("SCHEMA_" + i, mockResultSetWithRows(1));
        }
        for (int i = 1; i <= 55; i++) {
            when(meta.getTables("SCHEMA_" + i, null, "%", new String[]{"TABLE"}))
                    .thenReturn(rsMap.get("SCHEMA_" + i));
        }

        String result = command.exploreOverview(conn, meta, dialect, formatter, "agent-json");
        assertTrue(result.contains("\"schema_count\": 55"));
        assertTrue(result.contains("\"displayed\": 50"));
        assertTrue(result.contains("\"name\": \"SCHEMA_1\""));
        assertTrue(result.contains("\"name\": \"SCHEMA_50\""));
        // SCHEMA_51 should not appear
        assertFalse(result.contains("\"name\": \"SCHEMA_51\""));
    }

    // --- exploreSchema: markdown format ---

    @Test
    void exploreSchema_markdownFormat() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        OutputFormatter formatter = OutputFormatter.create("markdown");

        // getTables returns 2 tables
        ResultSet tableRs = mock(ResultSet.class);
        java.util.Deque<Boolean> tableOrder = new java.util.ArrayDeque<>(List.of(true, true, false));
        when(tableRs.next()).thenAnswer(inv -> tableOrder.poll());
        when(tableRs.getString("TABLE_NAME")).thenReturn("EMPLOYEES", "DEPARTMENTS");
        when(tableRs.getString("REMARKS")).thenReturn("Employee data", "Department info");
        when(meta.getTables("HR", null, "%", new String[]{"TABLE"})).thenReturn(tableRs);

        // getColumns for EMPLOYEES: id, name
        ResultSet empColRs = mock(ResultSet.class);
        java.util.Deque<Boolean> empColOrder = new java.util.ArrayDeque<>(List.of(true, true, false));
        when(empColRs.next()).thenAnswer(inv -> empColOrder.poll());
        when(empColRs.getString("COLUMN_NAME")).thenReturn("EMP_ID", "EMP_NAME");
        when(empColRs.getString("TYPE_NAME")).thenReturn("NUMBER", "VARCHAR2");
        when(empColRs.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls, DatabaseMetaData.columnNullable);
        when(meta.getColumns("HR", null, "EMPLOYEES", "%")).thenReturn(empColRs);

        // getColumns for DEPARTMENTS: dept_id
        ResultSet deptColRs = mock(ResultSet.class);
        java.util.Deque<Boolean> deptColOrder = new java.util.ArrayDeque<>(List.of(true, false));
        when(deptColRs.next()).thenAnswer(inv -> deptColOrder.poll());
        when(deptColRs.getString("COLUMN_NAME")).thenReturn("DEPT_ID");
        when(deptColRs.getString("TYPE_NAME")).thenReturn("NUMBER");
        when(deptColRs.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls);
        when(meta.getColumns("HR", null, "DEPARTMENTS", "%")).thenReturn(deptColRs);

        String result = command.exploreSchema(meta, "HR", formatter, "markdown");
        assertTrue(result.contains("Schema: HR"));
        assertTrue(result.contains("EMPLOYEES"));
        assertTrue(result.contains("Employee data"));
        assertTrue(result.contains("DEPARTMENTS"));
        assertTrue(result.contains("Department info"));
        assertTrue(result.contains("EMP_ID"));
        assertTrue(result.contains("EMP_NAME"));
        assertTrue(result.contains("DEPT_ID"));
    }

    @Test
    void exploreSchema_markdownFormat_noTables() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        OutputFormatter formatter = OutputFormatter.create("markdown");

        ResultSet tableRs = mockEmptyResultSet();
        when(meta.getTables("EMPTY", null, "%", new String[]{"TABLE"})).thenReturn(tableRs);

        String result = command.exploreSchema(meta, "EMPTY", formatter, "markdown");
        assertTrue(result.contains("Schema: EMPTY"));
        assertTrue(result.contains("no tables found"));
    }

    @Test
    void exploreSchema_markdownFormat_nullRemarks() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        OutputFormatter formatter = OutputFormatter.create("markdown");

        ResultSet tableRs = mock(ResultSet.class);
        java.util.Deque<Boolean> order = new java.util.ArrayDeque<>(List.of(true, false));
        when(tableRs.next()).thenAnswer(inv -> order.poll());
        when(tableRs.getString("TABLE_NAME")).thenReturn("USERS");
        when(tableRs.getString("REMARKS")).thenReturn(null);
        when(meta.getTables("PUBLIC", null, "%", new String[]{"TABLE"})).thenReturn(tableRs);

        ResultSet colRs = mockEmptyResultSet();
        when(meta.getColumns("PUBLIC", null, "USERS", "%")).thenReturn(colRs);

        String result = command.exploreSchema(meta, "PUBLIC", formatter, "markdown");
        assertTrue(result.contains("USERS"));
        // Should not crash with null remarks
        assertNotNull(result);
    }

    // --- exploreSchema: agent-json format ---

    @Test
    void exploreSchema_agentJsonFormat() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        OutputFormatter formatter = OutputFormatter.create("agent-json");

        // getTables returns 2 tables
        ResultSet tableRs = mock(ResultSet.class);
        java.util.Deque<Boolean> tableOrder = new java.util.ArrayDeque<>(List.of(true, true, false));
        when(tableRs.next()).thenAnswer(inv -> tableOrder.poll());
        when(tableRs.getString("TABLE_NAME")).thenReturn("EMPLOYEES", "DEPARTMENTS");
        when(tableRs.getString("REMARKS")).thenReturn("Employee data", "Department info");
        when(meta.getTables("HR", null, "%", new String[]{"TABLE"})).thenReturn(tableRs);

        // getColumns for EMPLOYEES
        ResultSet empColRs = mock(ResultSet.class);
        java.util.Deque<Boolean> empColOrder = new java.util.ArrayDeque<>(List.of(true, true, false));
        when(empColRs.next()).thenAnswer(inv -> empColOrder.poll());
        when(empColRs.getString("COLUMN_NAME")).thenReturn("EMP_ID", "EMP_NAME");
        when(empColRs.getString("TYPE_NAME")).thenReturn("NUMBER", "VARCHAR2");
        when(empColRs.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls, DatabaseMetaData.columnNullable);
        when(meta.getColumns("HR", null, "EMPLOYEES", "%")).thenReturn(empColRs);

        // getColumns for DEPARTMENTS
        ResultSet deptColRs = mock(ResultSet.class);
        java.util.Deque<Boolean> deptColOrder = new java.util.ArrayDeque<>(List.of(true, false));
        when(deptColRs.next()).thenAnswer(inv -> deptColOrder.poll());
        when(deptColRs.getString("COLUMN_NAME")).thenReturn("DEPT_ID");
        when(deptColRs.getString("TYPE_NAME")).thenReturn("NUMBER");
        when(deptColRs.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls);
        when(meta.getColumns("HR", null, "DEPARTMENTS", "%")).thenReturn(deptColRs);

        String result = command.exploreSchema(meta, "HR", formatter, "agent-json");
        assertTrue(result.contains("\"status\": \"ok\""));
        assertTrue(result.contains("\"data\""));
        assertTrue(result.contains("\"meta\""));
        assertTrue(result.contains("\"schema\": \"HR\""));
        assertTrue(result.contains("\"name\": \"EMPLOYEES\""));
        assertTrue(result.contains("\"name\": \"DEPARTMENTS\""));
        assertTrue(result.contains("\"remarks\": \"Employee data\""));
        assertTrue(result.contains("\"column_count\": 2"));
        assertTrue(result.contains("\"column_count\": 1"));
        assertTrue(result.contains("\"table_count\": 2"));
        // Column details should be present
        assertTrue(result.contains("\"name\":\"EMP_ID\""));
        assertTrue(result.contains("\"type\":\"NUMBER\""));
        assertTrue(result.contains("\"nullable\":\"NO\""));
    }

    @Test
    void exploreSchema_agentJsonFormat_emptySchema() throws Exception {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        OutputFormatter formatter = OutputFormatter.create("agent-json");

        ResultSet tableRs = mockEmptyResultSet();
        when(meta.getTables("EMPTY", null, "%", new String[]{"TABLE"})).thenReturn(tableRs);

        String result = command.exploreSchema(meta, "EMPTY", formatter, "agent-json");
        assertTrue(result.contains("\"status\": \"ok\""));
        assertTrue(result.contains("\"schema\": \"EMPTY\""));
        assertTrue(result.contains("\"tables\": []"));
        assertTrue(result.contains("\"table_count\": 0"));
    }

    // --- exploreOverview: countTables throws exception ---

    @Test
    void exploreOverview_markdownFormat_schemaError() throws Exception {
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Dialect dialect = mock(Dialect.class);
        OutputFormatter formatter = OutputFormatter.create("markdown");

        when(dialect.listDatabases(conn)).thenReturn(List.of(
                new String[]{"GOOD"},
                new String[]{"BAD"}
        ));
        when(dialect.getDatabaseLabel()).thenReturn("Schema");

        // Overview mode no longer calls getTables(), so both schemas appear regardless of access
        String result = command.exploreOverview(conn, meta, dialect, formatter, "markdown");
        assertTrue(result.contains("GOOD"));
        assertTrue(result.contains("BAD"));
    }

    // --- Helper: create mock ResultSet that returns n rows ---

    private ResultSet mockResultSetWithRows(int count) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        java.util.Deque<Boolean> order = new java.util.ArrayDeque<>();
        for (int i = 0; i < count; i++) {
            order.add(true);
        }
        order.add(false);
        final java.util.Deque<Boolean> finalOrder = order;
        org.mockito.stubbing.Answer<Boolean> answer = inv -> {
            Boolean val = finalOrder.poll();
            return val != null ? val : false;
        };
        org.mockito.Mockito.doAnswer(answer).when(rs).next();
        return rs;
    }

    private ResultSet mockEmptyResultSet() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        doReturn(false).when(rs).next();
        return rs;
    }
}
