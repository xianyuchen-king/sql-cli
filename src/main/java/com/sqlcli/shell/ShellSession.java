package com.sqlcli.shell;

import com.sqlcli.Version;
import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.executor.QueryExecutor;
import com.sqlcli.output.OutputFormatter;
import com.sqlcli.safety.SafetyGuard;
import com.sqlcli.safety.SqlAnalyzer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Interactive SQL shell session with connection reuse.
 * Provides REPL (Read-Eval-Print Loop) for database operations.
 */
public class ShellSession implements AutoCloseable {

    private Connection connection;
    private ConnectionConfig connectionConfig;
    private final ConnectionManager connMgr;
    private final ConfigManager configManager;
    private final ShellState state;
    private final SqlBuffer sqlBuffer = new SqlBuffer();
    private final SlashCommandHandler slashHandler = new SlashCommandHandler();
    private final QueryExecutor queryExecutor = new QueryExecutor();
    private final SafetyGuard safetyGuard = new SafetyGuard();

    private LineReader lineReader;
    private Terminal terminal;

    public ShellSession(ConnectionConfig config, ConnectionManager connMgr) throws SQLException {
        this.connectionConfig = config;
        this.connMgr = connMgr;
        this.configManager = new ConfigManager();
        this.connection = connMgr.connect(config);

        AppConfig appConfig = configManager.load();
        String format = appConfig.getDefaults().getOutputFormat();
        OutputFormatter.Format fmt = OutputFormatter.Format.valueOf(format.toUpperCase());

        int maxRows = appConfig.getDefaults().getMaxRows();
        boolean autoLimit = appConfig.getDefaults().isAutoLimit();

        this.state = new ShellState(fmt, maxRows, autoLimit);
    }

    /**
     * Start the REPL loop.
     */
    public void start() {
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .dumb(true)
                    .build();

            // Setup history file at ~/.sql-cli/history
            Path historyPath = getHistoryPath();

            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .variable(LineReader.HISTORY_FILE, historyPath)
                    .option(org.jline.reader.LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .build();

            printWelcome();

            while (!state.isExitRequested()) {
                try {
                    // Check for connection switch request
                    if (state.isSwitchConnectionRequested()) {
                        switchConnection();
                        continue;
                    }

                    String prompt = buildPrompt();
                    String line = lineReader.readLine(prompt);

                    if (line == null) {
                        break;  // EOF
                    }

                    processLine(line);

                } catch (UserInterruptException e) {
                    // Ctrl+C - cancel current input
                    System.out.println("^C");
                    sqlBuffer.clear();
                } catch (EndOfFileException e) {
                    // Ctrl+D - exit
                    break;
                }
            }

            // Save history before exiting
            try {
                lineReader.getHistory().save();
            } catch (Exception e) {
                // Ignore history save errors
            }

            printGoodbye();

        } catch (Exception e) {
            System.err.println("[ERROR] Shell error: " + e.getMessage());
        }
    }

    private void switchConnection() {
        String newConnName = state.getSwitchToConnection();
        state.clearSwitchConnection();

        // Save current connection info for potential rollback
        Connection oldConnection = connection;
        ConnectionConfig oldConfig = connectionConfig;

        try {
            // Load new connection config first
            ConnectionConfig newConfig = configManager.load().getConnection(newConnName);
            if (newConfig == null) {
                System.err.println("[ERROR] Connection not found: " + newConnName);
                System.err.println("Staying on current connection.");
                return;
            }

            // Try to connect to new database first
            Connection newConnection = connMgr.connect(newConfig);

            // Success - close old connection and switch
            if (oldConnection != null && !oldConnection.isClosed()) {
                oldConnection.close();
            }

            connection = newConnection;
            connectionConfig = newConfig;

            System.out.println("Switched to connection: " + newConnName);
            System.out.println("Connected to: " + connectionConfig.getType() + "://" + getConnectionDisplay());
            System.out.println();

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to switch connection: " + e.getMessage());
            System.err.println("Staying on current connection.");
            // Keep old connection intact
        }
    }

    private Path getHistoryPath() {
        String home = System.getProperty("user.home");
        Path configDir = Paths.get(home, ".sql-cli");
        if (!configDir.toFile().exists()) {
            configDir.toFile().mkdirs();
        }
        return configDir.resolve("history");
    }

    private void printWelcome() {
        String name = connectionConfig.getName() != null ? connectionConfig.getName() : "unnamed";
        String type = connectionConfig.getType() != null ? connectionConfig.getType() : "unknown";

        System.out.println();
        System.out.println("sql-cli shell " + Version.get());
        System.out.println("Connected to: " + type + "://" + getConnectionDisplay());
        System.out.println("Type '\\help' for available commands, '\\exit' or Ctrl+D to exit.");
        System.out.println();
    }

    private void printGoodbye() {
        System.out.println();
        System.out.println("Connection closed. Bye!");
    }

    private String getConnectionDisplay() {
        if (connectionConfig.getHost() != null) {
            String host = connectionConfig.getHost();
            int port = connectionConfig.getPort() != null ? connectionConfig.getPort() : 0;
            String db = connectionConfig.getDb() != null ? connectionConfig.getDb() : "";
            if (port > 0) {
                return host + ":" + port + "/" + db;
            }
            return host + "/" + db;
        }
        if (connectionConfig.getUrl() != null) {
            String url = connectionConfig.getUrl();
            if (url.length() > 50) {
                return url.substring(0, 47) + "...";
            }
            return url;
        }
        return "(direct)";
    }

    private String buildPrompt() {
        String name = connectionConfig.getName() != null ? connectionConfig.getName() : "db";

        if (sqlBuffer.hasContent()) {
            String preview = sqlBuffer.getPreview();
            String cont = preview.length() > 20 ? preview.substring(0, 17) + "..." : preview;
            return name + "> " + cont + "\n... ";
        }
        return name + "> ";
    }

    private void processLine(String line) {
        String trimmed = line.trim();

        if (trimmed.isEmpty()) {
            return;
        }

        // Check for exit/quit commands (without requiring backslash)
        if (trimmed.equalsIgnoreCase("exit") || trimmed.equalsIgnoreCase("quit")
                || trimmed.equalsIgnoreCase("q")) {
            state.requestExit();
            return;
        }

        // Slash command
        if (trimmed.startsWith("\\")) {
            String result = slashHandler.handle(trimmed, connection, state, getPromptName());
            if (result != null && !result.isEmpty()) {
                System.out.println(result);
            }
            return;
        }

        // SQL statement
        boolean complete = sqlBuffer.append(line);
        if (!complete) {
            return;  // Wait for more input
        }

        String sql = sqlBuffer.getSql();
        sqlBuffer.clear();

        if (sql.isEmpty()) {
            return;
        }

        executeSql(sql);
    }

    private String getPromptName() {
        return connectionConfig.getName() != null ? connectionConfig.getName() : "db";
    }

    private void executeSql(String sql) {
        // Remove trailing semicolon for analysis
        String sqlForAnalysis = sql;
        if (sqlForAnalysis.endsWith(";")) {
            sqlForAnalysis = sqlForAnalysis.substring(0, sqlForAnalysis.length() - 1).trim();
        }

        // Safety check
        SqlAnalyzer.RiskLevel risk = checkSafety(sqlForAnalysis);
        if (risk == SqlAnalyzer.RiskLevel.BLOCKED) {
            System.err.println("[BLOCKED] This SQL is not allowed by safety rules.");
            return;
        }
        if (risk == SqlAnalyzer.RiskLevel.DANGEROUS) {
            String confirm = lineReader.readLine("This is a dangerous operation. Confirm? [y/N]: ");
            if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                System.out.println("Cancelled.");
                return;
            }
        }

        try {
            String result = doExecute(sqlForAnalysis);
            System.out.println(result);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private SqlAnalyzer.RiskLevel checkSafety(String sql) {
        // Check strict mode
        String effectiveLevel = connectionConfig.getEffectiveSafetyLevel("normal");
        if ("strict".equalsIgnoreCase(effectiveLevel)) {
            SqlAnalyzer analyzer = new SqlAnalyzer();
            if (!analyzer.isSelect(sql)) {
                return SqlAnalyzer.RiskLevel.BLOCKED;
            }
        }

        if ("none".equalsIgnoreCase(effectiveLevel)) {
            return SqlAnalyzer.RiskLevel.SAFE;
        }

        // Normal safety check
        SqlAnalyzer analyzer = new SqlAnalyzer();
        return analyzer.analyze(sql);
    }

    private String doExecute(String sql) throws Exception {
        SqlAnalyzer analyzer = new SqlAnalyzer();

        if (analyzer.isSelect(sql)) {
            // SELECT query
            OutputFormatter formatter = state.createFormatter();
            String wrappedSql = applyRowLimit(sql);
            return queryExecutor.execute(connection, wrappedSql, formatter,
                    state.getMaxRows(), state.getQueryTimeout());
        } else {
            // DML/DDL statement
            try (java.sql.Statement stmt = connection.createStatement()) {
                if (state.getQueryTimeout() > 0) {
                    stmt.setQueryTimeout(state.getQueryTimeout());
                }
                long start = System.nanoTime();
                int affected = stmt.executeUpdate(sql);
                long elapsed = System.nanoTime() - start;
                double seconds = elapsed / 1_000_000_000.0;
                return "Query OK, " + affected + " row" + (affected != 1 ? "s" : "")
                        + " affected (" + String.format("%.2f", seconds) + "s)";
            }
        }
    }

    private String applyRowLimit(String sql) {
        if (!state.isAutoLimit() || state.getMaxRows() <= 0) {
            return sql;
        }

        // Apply row limit via dialect
        AppConfig appConfig = configManager.load();
        com.sqlcli.dialect.Dialect dialect;
        if (connectionConfig.getType() != null) {
            dialect = com.sqlcli.dialect.DialectFactory.getDialect(connectionConfig.getType(), appConfig);
        } else {
            dialect = new com.sqlcli.dialect.GenericDialect();
        }

        if (dialect.hasLimit(sql)) {
            return sql;
        }

        String wrapped = dialect.wrapLimit(sql, state.getMaxRows());
        return wrapped != null ? wrapped : sql;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        if (terminal != null) {
            try {
                terminal.close();
            } catch (Exception ignored) {
            }
        }
    }
}
