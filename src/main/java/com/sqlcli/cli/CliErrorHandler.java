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
                System.out.println(AgentResult.error(classifyError(e), e.getMessage()).toJson());
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
     * Classify a generic exception into an ErrorCode.
     */
    private static ErrorCode classifyError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return ErrorCode.UNKNOWN;

        String lower = msg.toLowerCase();
        if (lower.contains("connection") && (lower.contains("not found") || lower.contains("unknown"))) {
            return ErrorCode.CONNECTION_NOT_FOUND;
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return ErrorCode.QUERY_TIMEOUT;
        }
        if (lower.contains("driver") && lower.contains("not found")) {
            return ErrorCode.DRIVER_NOT_FOUND;
        }
        if (lower.contains("table") && (lower.contains("not found") || lower.contains("doesn't exist"))) {
            return ErrorCode.TABLE_NOT_FOUND;
        }
        if (lower.contains("duplicate") || lower.contains("unique constraint") || lower.contains("primary key")) {
            return ErrorCode.DUPLICATE_KEY;
        }
        if (lower.contains("connection") && (lower.contains("refused") || lower.contains("failed") || lower.contains("unable"))) {
            return ErrorCode.CONNECTION_FAILED;
        }
        return ErrorCode.UNKNOWN;
    }

    /**
     * Remove [BLOCKED] / [DANGEROUS] prefixes for structured error output.
     */
    private static String cleanMessage(String message) {
        if (message == null) return null;
        return message.replaceFirst("^\\[(BLOCKED|DANGEROUS|WARN)\\]\\s*", "");
    }
}
