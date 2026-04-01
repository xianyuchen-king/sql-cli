package com.sqlcli.cli;

import com.sqlcli.output.ErrorCode;
import com.sqlcli.safety.SafetyException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class CliErrorHandlerTest {

    /**
     * Access the private classifyError method via reflection.
     */
    private ErrorCode classifyError(Exception e) {
        try {
            Method method = CliErrorHandler.class.getDeclaredMethod("classifyError", Exception.class);
            method.setAccessible(true);
            return (ErrorCode) method.invoke(null, e);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to invoke classifyError", ex);
        }
    }

    // --- Oracle errors ---

    @Test
    void oracle_00942_tableNotFound() {
        Exception e = new SQLException("ORA-00942: table or view does not exist");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    @Test
    void oracle_00904_validationError() {
        Exception e = new SQLException("ORA-00904: invalid identifier");
        assertEquals(ErrorCode.VALIDATION_ERROR, classifyError(e));
    }

    @Test
    void oracle_01017_connectionFailed() {
        Exception e = new SQLException("ORA-01017: invalid username/password; logon denied");
        assertEquals(ErrorCode.CONNECTION_FAILED, classifyError(e));
    }

    @Test
    void oracle_12154_connectionFailed() {
        Exception e = new SQLException("ORA-12154: TNS:could not resolve the connect identifier specified");
        assertEquals(ErrorCode.CONNECTION_FAILED, classifyError(e));
    }

    @Test
    void oracle_01031_permitDenied() {
        Exception e = new SQLException("ORA-01031: insufficient privileges");
        assertEquals(ErrorCode.PERMIT_DENIED, classifyError(e));
    }

    @Test
    void oracle_00001_duplicateKey() {
        Exception e = new SQLException("ORA-00001: unique constraint violated");
        assertEquals(ErrorCode.DUPLICATE_KEY, classifyError(e));
    }

    // --- DM (Dameng) Chinese messages ---

    @Test
    void dm_invalidTableName_tableNotFound() {
        Exception e = new SQLException("无效的表名");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    @Test
    void dm_invalidObjectName_tableNotFound() {
        Exception e = new SQLException("无效的对象名");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    @Test
    void dm_invalidTableOrViewName_tableNotFound() {
        Exception e = new SQLException("第1 行附近出现错误:\n无效的表或视图名[nonexistent_table]");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    // --- PostgreSQL ---

    @Test
    void postgresql_relationDoesNotExist_tableNotFound() {
        Exception e = new SQLException("ERROR: relation \"users\" does not exist");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    // --- SQL Server ---

    @Test
    void mssql_invalidObjectName_tableNotFound() {
        Exception e = new SQLException("Invalid object name 'dbo.users'");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    // --- MySQL ---

    @Test
    void mysql_tableDoesntExist_tableNotFound() {
        Exception e = new SQLException("Table 'mydb.users' doesn't exist");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    @Test
    void mysql_unknownTable_tableNotFound() {
        Exception e = new SQLException("Unknown table 'NONEXISTENT_TABLE' in information_schema");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
    }

    @Test
    void mysql_unknownDatabase_connectionNotFound() {
        // "Unknown database" without "connection" keyword falls through to UNKNOWN
        // because CONNECTION_NOT_FOUND requires "connection" AND ("not found" | "unknown")
        Exception e = new SQLException("Unknown database 'mydb'");
        assertEquals(ErrorCode.UNKNOWN, classifyError(e));
    }

    // --- Generic patterns ---

    @Test
    void generic_connectionRefused_connectionFailed() {
        Exception e = new Exception("Connection refused: connect");
        assertEquals(ErrorCode.CONNECTION_FAILED, classifyError(e));
    }

    @Test
    void generic_timeout_queryTimeout() {
        Exception e = new Exception("Query timed out after 30 seconds");
        assertEquals(ErrorCode.QUERY_TIMEOUT, classifyError(e));
    }

    @Test
    void generic_driverNotFound_driverNotFound() {
        Exception e = new Exception("Driver not found for type: mysql");
        assertEquals(ErrorCode.DRIVER_NOT_FOUND, classifyError(e));
    }

    @Test
    void generic_syntaxError_validationError() {
        Exception e = new Exception("You have an error in your SQL syntax");
        assertEquals(ErrorCode.VALIDATION_ERROR, classifyError(e));
    }

    // --- Cause chain traversal ---

    @Test
    void causeChain_outerNullInnerOracleMessage() {
        SQLException inner = new SQLException("ORA-00942: table or view does not exist");
        SQLException outer = new SQLException(null, inner);
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(outer));
    }

    @Test
    void causeChain_skipNullMiddle() {
        SQLException inner = new SQLException("ORA-00942: table or view does not exist");
        SQLException middle = new SQLException(null, inner);
        SQLException outer = new SQLException("some error", middle);
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(outer));
    }

    // --- Null message ---

    @Test
    void nullMessage_unknown() {
        Exception e = new Exception((String) null);
        assertEquals(ErrorCode.UNKNOWN, classifyError(e));
    }

    // --- Config narrowing ---

    @Test
    void configParameter_notConfigError() {
        Exception e = new Exception("Configuration parameter 'max_connections' is invalid");
        assertNotEquals(ErrorCode.CONFIG_ERROR, classifyError(e));
    }

    @Test
    void failedToLoadConfig_configError() {
        Exception e = new Exception("Failed to load config from ~/.sql-cli/config.yml");
        assertEquals(ErrorCode.CONFIG_ERROR, classifyError(e));
    }

    @Test
    void failedToParseConfig_configError() {
        Exception e = new Exception("Failed to parse config: unexpected token");
        assertEquals(ErrorCode.CONFIG_ERROR, classifyError(e));
    }

    @Test
    void invalidConfig_configError() {
        Exception e = new Exception("Invalid config: missing required field 'type'");
        assertEquals(ErrorCode.CONFIG_ERROR, classifyError(e));
    }

    @Test
    void yamlError_configError() {
        Exception e = new Exception("Error parsing YAML at line 5");
        assertEquals(ErrorCode.CONFIG_ERROR, classifyError(e));
    }

    // --- Priority: ORA-00942 should be TABLE_NOT_FOUND, not VALIDATION_ERROR ---

    @Test
    void priority_ora00942_isTableNotFound() {
        // ORA-00942 contains "ora-009" which could match syntax error,
        // but dialect-specific patterns are checked first
        Exception e = new SQLException("ORA-00942: table or view does not exist");
        assertEquals(ErrorCode.TABLE_NOT_FOUND, classifyError(e));
        assertNotEquals(ErrorCode.VALIDATION_ERROR, classifyError(e));
    }

    // --- handleError JSON vs plaintext ---

    @Test
    void handleError_jsonFormat_outputsJson() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        try {
            CliErrorHandler.handleError(new SQLException("Table 'x' doesn't exist"), "json");
            String output = baos.toString();
            assertTrue(output.contains("\"status\": \"error\""));
            assertTrue(output.contains("\"error_code\": \"TABLE_NOT_FOUND\""));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void handleError_agentJsonFormat_outputsJson() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        try {
            CliErrorHandler.handleError(new SQLException("connection refused"), "agent-json");
            String output = baos.toString();
            assertTrue(output.contains("\"status\": \"error\""));
            assertTrue(output.contains("\"error_code\": \"CONNECTION_FAILED\""));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void handleError_plainText_outputsToStderr() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(baos));
        try {
            CliErrorHandler.handleError(new SQLException("some error"), null);
            String output = baos.toString();
            assertTrue(output.contains("[ERROR] some error"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void handleError_safetyException_jsonFormat() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        try {
            CliErrorHandler.handleError(
                    new SafetyException(ErrorCode.SAFETY_BLOCKED, "[BLOCKED] DROP TABLE users"),
                    "json");
            String output = baos.toString();
            assertTrue(output.contains("\"status\": \"error\""));
            assertTrue(output.contains("\"error_code\": \"SAFETY_BLOCKED\""));
            assertFalse(output.contains("[BLOCKED]")); // prefix should be cleaned
        } finally {
            System.setOut(originalOut);
        }
    }

    // --- isJsonFormat ---

    @Test
    void isJsonFormat_json_true() {
        assertTrue(CliErrorHandler.isJsonFormat("json"));
    }

    @Test
    void isJsonFormat_agentJson_true() {
        assertTrue(CliErrorHandler.isJsonFormat("agent-json"));
    }

    @Test
    void isJsonFormat_caseInsensitive() {
        assertTrue(CliErrorHandler.isJsonFormat("JSON"));
        assertTrue(CliErrorHandler.isJsonFormat("Agent-JSON"));
    }

    @Test
    void isJsonFormat_null_false() {
        assertFalse(CliErrorHandler.isJsonFormat(null));
    }

    @Test
    void isJsonFormat_markdown_false() {
        assertFalse(CliErrorHandler.isJsonFormat("markdown"));
    }
}
