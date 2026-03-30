package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.output.AgentResult;
import com.sqlcli.safety.SafetyGuard;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Command(name = "exec", description = "Execute DDL/DML statement")
public class ExecCommand implements Runnable {

    @Parameters(paramLabel = "SQL", description = "SQL statement to execute")
    private String sql;

    @Option(names = {"-c", "--connection"}, description = "Connection name")
    private String connection;

    @Option(names = {"-f", "--format"}, description = "Output format: markdown/json/agent-json")
    private String format;

    @Option(names = {"--url"}, description = "JDBC URL")
    private String url;

    @Option(names = {"--type"}, description = "Database type")
    private String type;

    @Option(names = {"--host"}, description = "Database host")
    private String host;

    @Option(names = {"--port"}, type = Integer.class, description = "Database port")
    private Integer port;

    @Option(names = {"--user"}, description = "Username")
    private String user;

    @Option(names = {"--password"}, description = "Password")
    private String password;

    @Option(names = {"--db"}, description = "Database name")
    private String db;

    @Option(names = {"--driver"}, description = "Driver jar file name")
    private String driver;

    @Option(names = {"--driver-class"}, description = "Driver class name")
    private String driverClass;

    @Option(names = {"--confirm"}, description = "Confirm dangerous operations")
    private boolean confirm;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        AppConfig config = cm.load();

        ConnectionOptions opts = new ConnectionOptions(cm, connMgr);
        ConnectionConfig inlineConfig = opts.buildInlineConfig(type, host, port, user, password, db, url, driver, driverClass);
        ConnectionConfig resolved = connMgr.resolveConnection(connection, inlineConfig);

        // Resolve format first
        String outputFormat = format != null ? format : config.getDefaults().getOutputFormat();
        boolean isJson = CliErrorHandler.isJsonFormat(outputFormat);

        SafetyGuard guard = new SafetyGuard();
        String validatedSql;
        try {
            validatedSql = guard.validate(sql, resolved, confirm, true, 0, config);
        } catch (Exception e) {
            CliErrorHandler.handleError(e, outputFormat);
            return;
        }

        // Route warnings for plaintext mode
        List<String> warnings = guard.getWarnings();
        if (!isJson) {
            for (String w : warnings) {
                System.err.println("[WARN] " + w);
            }
        }

        try (Connection conn = connMgr.connect(resolved)) {
            try (Statement stmt = conn.createStatement()) {
                int affected = stmt.executeUpdate(validatedSql);
                if (isJson) {
                    System.out.println(AgentResult.ok(Map.of("affected_rows", affected), warnings).toJson());
                } else {
                    System.out.println("[DONE] " + affected + " row(s) affected.");
                }
            }
        } catch (Exception e) {
            CliErrorHandler.handleError(e, outputFormat);
        }
    }
}
