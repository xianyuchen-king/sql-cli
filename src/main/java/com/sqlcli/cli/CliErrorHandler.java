package com.sqlcli.cli;

import com.sqlcli.output.AgentResult;
import com.sqlcli.output.ErrorCode;
import com.sqlcli.safety.SafetyException;

/**
 * Centralized error handling for CLI commands.
 * Outputs structured JSON when format is json/agent-json, plain text otherwise.
 */
public class CliErrorHandler {

    /**
     * Handle an exception from a CLI command.
     * @param e the exception
     * @param format the output format (null, "json", "agent-json", etc.)
     */
    public static void handleError(Exception e, String format) {
        if (isJsonFormat(format)) {
            if (e instanceof SafetyException se) {
                System.out.println(AgentResult.error(se.getErrorCode(), cleanMessage(se.getMessage())).toJson());
            } else {
                System.out.println(AgentResult.error(classifyError(e), cleanMessage(e.getMessage())).toJson());
            }
        } else {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    /**
     * Check if the format requires JSON output.
     */
    public static boolean isJsonFormat(String format) {
        return format != null && (format.equalsIgnoreCase("json") || format.equalsIgnoreCase("agent-json"));
    }

    /**
     * Traverse the cause chain and return the deepest non-null message.
     * If all messages are null, return null.
     */
    private static String getRootMessage(Throwable e) {
        String deepest = null;
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null) {
                deepest = current.getMessage();
            }
            current = current.getCause();
        }
        return deepest;
    }

    /**
     * Classify a generic exception into an ErrorCode.
     */
    private static ErrorCode classifyError(Exception e) {
        String msg = getRootMessage(e);
        if (msg == null) return ErrorCode.UNKNOWN;

        String lower = msg.toLowerCase();

        // --- Dialect-specific patterns (checked before generic) ---

        // Oracle errors
        if (lower.contains("ora-00942")) return ErrorCode.TABLE_NOT_FOUND;
        if (lower.contains("ora-00904")) return ErrorCode.VALIDATION_ERROR;
        if (lower.contains("ora-01017")) return ErrorCode.CONNECTION_FAILED;
        if (lower.contains("ora-12154")) return ErrorCode.CONNECTION_FAILED;
        if (lower.contains("ora-01031")) return ErrorCode.PERMIT_DENIED;
        if (lower.contains("ora-00001")) return ErrorCode.DUPLICATE_KEY;

        // DM (Dameng) Chinese messages
        if (lower.contains("无效的表名") || lower.contains("无效的表或视图名") || lower.contains("无效的对象名")) return ErrorCode.TABLE_NOT_FOUND;

        // PostgreSQL: "relation \"xxx\" does not exist"
        if (lower.contains("relation") && lower.contains("does not exist")) return ErrorCode.TABLE_NOT_FOUND;

        // SQL Server: "Invalid object name 'xxx'"
        if (lower.contains("invalid object name")) return ErrorCode.TABLE_NOT_FOUND;

        // --- Generic patterns ---

        // Connection not found
        if (lower.contains("connection") && (lower.contains("not found") || lower.contains("unknown"))) {
            return ErrorCode.CONNECTION_NOT_FOUND;
        }

        // Connection refused / network issues
        if (lower.contains("connection") && (lower.contains("refused") || lower.contains("failed") || lower.contains("unable"))) {
            return ErrorCode.CONNECTION_FAILED;
        }
        if (lower.contains("communicate") || lower.contains("broken pipe") || lower.contains("reset")
                || lower.contains("connection is closed") || lower.contains("connection has been closed")) {
            return ErrorCode.CONNECTION_FAILED;
        }

        // Authentication / permission
        if (lower.contains("access denied") || lower.contains("authentication failed")
                || lower.contains("login failed") || lower.contains("invalid credentials")
                || lower.contains("permission denied") || lower.contains("unauthorized")) {
            return ErrorCode.CONNECTION_FAILED;
        }

        // Timeout
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return ErrorCode.QUERY_TIMEOUT;
        }

        // Driver not found
        if (lower.contains("driver") && (lower.contains("not found") || lower.contains("no suitable"))) {
            return ErrorCode.DRIVER_NOT_FOUND;
        }
        if (lower.contains("classnotfoundexception") || lower.contains("class not found")) {
            return ErrorCode.DRIVER_CLASS_NOT_FOUND;
        }

        // Table not found
        if (lower.contains("table") && (lower.contains("not found") || lower.contains("doesn't exist")
                || lower.contains("does not exist") || lower.contains("unknown table"))) {
            return ErrorCode.TABLE_NOT_FOUND;
        }

        // Syntax error
        if (lower.contains("syntax error") || lower.contains("sqlsyntaxerror")
                || lower.contains("invalid sql")
                || lower.contains("you have an error in your sql syntax")) {
            return ErrorCode.VALIDATION_ERROR;
        }

        // Duplicate key
        if (lower.contains("duplicate") || lower.contains("unique constraint") || lower.contains("primary key")) {
            return ErrorCode.DUPLICATE_KEY;
        }

        // Config error (narrowed patterns)
        if (lower.contains("failed to load config")
                || lower.contains("failed to parse config")
                || lower.contains("invalid config")
                || lower.contains("failed to save config")) {
            return ErrorCode.CONFIG_ERROR;
        }

        // Broader YAML/parse fallback for config issues
        if (lower.contains("yaml") || lower.contains("parse")) {
            return ErrorCode.CONFIG_ERROR;
        }

        return ErrorCode.UNKNOWN;
    }

    /**
     * Remove [BLOCKED] / [DANGEROUS] / [WARN] prefixes for structured error output.
     */
    private static String cleanMessage(String message) {
        if (message == null) return null;
        return message.replaceFirst("^\\[(BLOCKED|DANGEROUS|WARN)\\]\\s*", "");
    }
}
