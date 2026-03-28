package com.sqlcli.shell;

import com.sqlcli.executor.MetaExecutor;
import com.sqlcli.output.OutputFormatter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handler for slash commands (\dt, \d, etc.) in the interactive shell.
 */
public class SlashCommandHandler {

    private final MetaExecutor metaExecutor = new MetaExecutor();

    /**
     * Parse and execute a slash command.
     * @param line the command line starting with \
     * @param conn the database connection
     * @param state the current shell state
     * @param promptName the name to display in prompt
     * @return result message or null
     */
    public String handle(String line, Connection conn, ShellState state, String promptName) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("\\")) {
            return null;
        }

        // Remove backslash and split
        String cmdLine = trimmed.substring(1).trim();
        String[] parts = cmdLine.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1].trim() : "";

        try {
            return switch (cmd) {
                case "dt", "tables" -> handleListTables(conn, state);
                case "d", "describe" -> handleDescribe(conn, state, args);
                case "db", "databases" -> handleListDatabases(conn, state);
                case "dv", "views" -> handleListViews(conn, state);
                case "c", "connect" -> handleSwitchConnection(state, args);
                case "format" -> handleFormat(state, args);
                case "limit" -> handleLimit(state, args);
                case "nolimit" -> handleNoLimit(state);
                case "timeout" -> handleTimeout(state, args);
                case "help", "h", "?" -> handleHelp();
                case "exit", "quit", "q" -> handleExit(state);
                case "status" -> handleStatus(state, promptName);
                default -> "Unknown command: \\" + cmd + ". Type \\help for available commands.";
            };
        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }
    }

    private String handleListTables(Connection conn, ShellState state) throws Exception {
        return metaExecutor.listTables(conn, null, state.createFormatter());
    }

    private String handleDescribe(Connection conn, ShellState state, String tableName) throws Exception {
        if (tableName.isEmpty()) {
            return "Usage: \\d <table-name>  or  \\describe <table-name>";
        }
        return metaExecutor.describeTable(conn, null, tableName, state.createFormatter());
    }

    private String handleListDatabases(Connection conn, ShellState state) throws Exception {
        return metaExecutor.listDatabases(conn, state.createFormatter());
    }

    private String handleListViews(Connection conn, ShellState state) throws Exception {
        return metaExecutor.listViews(conn, null, state.createFormatter());
    }

    private String handleSwitchConnection(ShellState state, String connectionName) {
        if (connectionName.isEmpty()) {
            return "Usage: \\c <connection-name>  or  \\connect <connection-name>";
        }
        state.requestSwitchConnection(connectionName);
        return "Switching to connection: " + connectionName + "...";
    }

    private String handleFormat(ShellState state, String format) {
        if (format.isEmpty()) {
            return "Current format: " + state.getOutputFormat().name().toLowerCase();
        }
        try {
            OutputFormatter.Format f = OutputFormatter.Format.valueOf(format.toUpperCase());
            state.setOutputFormat(f);
            return "Format set to: " + f.name().toLowerCase();
        } catch (IllegalArgumentException e) {
            return "Unknown format: " + format + ". Available: markdown, json, csv";
        }
    }

    private String handleLimit(ShellState state, String limitStr) {
        if (limitStr.isEmpty()) {
            return "Current limit: " + (state.getMaxRows() > 0 ? state.getMaxRows() : "unlimited");
        }
        try {
            int limit = Integer.parseInt(limitStr);
            if (limit <= 0) {
                return "Limit must be a positive integer. Use \\nolimit to disable.";
            }
            state.setMaxRows(limit);
            state.setAutoLimit(true);
            return "Row limit set to: " + limit;
        } catch (NumberFormatException e) {
            return "Invalid number: " + limitStr;
        }
    }

    private String handleNoLimit(ShellState state) {
        state.disableLimit();
        return "Row limit disabled.";
    }

    private String handleTimeout(ShellState state, String timeoutStr) {
        if (timeoutStr.isEmpty()) {
            return "Current timeout: " + (state.getQueryTimeout() > 0 ? state.getQueryTimeout() + "s" : "none");
        }
        try {
            int timeout = Integer.parseInt(timeoutStr);
            if (timeout < 0) {
                return "Timeout must be non-negative. Use 0 to disable.";
            }
            state.setQueryTimeout(timeout);
            return "Query timeout set to: " + (timeout > 0 ? timeout + "s" : "none");
        } catch (NumberFormatException e) {
            return "Invalid number: " + timeoutStr;
        }
    }

    private String handleExit(ShellState state) {
        state.requestExit();
        return null;  // No output, just exit
    }

    private String handleStatus(ShellState state, String promptName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Connection: ").append(promptName).append('\n');
        sb.append("Format: ").append(state.getOutputFormat().name().toLowerCase()).append('\n');
        sb.append("Row limit: ").append(state.getMaxRows() > 0 ? state.getMaxRows() : "unlimited").append('\n');
        sb.append("Query timeout: ").append(state.getQueryTimeout() > 0 ? state.getQueryTimeout() + "s" : "none");
        return sb.toString();
    }

    private String handleHelp() {
        return """
            Slash Commands:
              \\dt, \\tables           List all tables
              \\d <table>              Describe table structure
              \\db, \\databases        List all databases
              \\dv, \\views            List all views
              \\c <name>               Switch to another connection
              \\format [markdown|json|csv]  Set output format
              \\limit <n>              Set max rows (0 = unlimited)
              \\nolimit                Disable row limit
              \\timeout <n>            Set query timeout in seconds
              \\status                 Show current settings
              \\help                   Show this help
              \\exit, \\quit, \\q       Exit shell

            SQL:
              End with ; to execute. Multi-line supported.
            """;
    }
}
