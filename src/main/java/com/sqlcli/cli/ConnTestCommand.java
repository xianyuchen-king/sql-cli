package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.output.AgentResult;
import com.sqlcli.output.ErrorCode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

@Command(name = "test", description = "Test a database connection")
public class ConnTestCommand implements Runnable {

    @Parameters(paramLabel = "NAME", description = "Connection name")
    private String name;

    @Option(names = {"--verbose", "-v"}, description = "Show detailed error")
    private boolean verbose;

    @Option(names = {"-f", "--format"}, description = "Output format: markdown/json/agent-json")
    private String format;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);

        try {
            ConnectionConfig config = connMgr.resolveConnection(name, null);
            if (verbose && !CliErrorHandler.isJsonFormat(format)) {
                System.out.println("=== Connection Configuration ===");
                System.out.println("Host: " + config.getHost());
                System.out.println("Port: " + config.getPort());
                System.out.println("User: " + config.getUser());
                System.out.println("Database: " + (config.getDb() != null ? config.getDb() : "(not specified)"));
                System.out.println("Type: " + config.getType());
                System.out.println("Driver: " + config.getDriver());
            }
            boolean ok = connMgr.testConnection(config);

            if (CliErrorHandler.isJsonFormat(format)) {
                if (ok) {
                    System.out.println(AgentResult.ok(Map.of("connection", name, "reachable", true)).toJson());
                } else {
                    System.out.println(AgentResult.error(ErrorCode.CONNECTION_FAILED,
                            "Connection '" + name + "' is NOT reachable").toJson());
                }
            } else {
                if (ok) {
                    System.out.println("[OK] Connection '" + name + "' is reachable.");
                } else {
                    System.out.println("[FAIL] Connection '" + name + "' is NOT reachable.");
                    if (verbose) {
                        try {
                            connMgr.connect(config);
                        } catch (Exception e) {
                            System.out.println("\n=== Error Details ===");
                            e.printStackTrace(System.out);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (CliErrorHandler.isJsonFormat(format)) {
                System.out.println(AgentResult.error(ErrorCode.CONNECTION_FAILED,
                        "Connection '" + name + "' failed: " + e.getMessage()).toJson());
            } else {
                System.out.println("[FAIL] Connection '" + name + "' failed: " + e.getMessage());
                if (verbose) {
                    System.out.println("\n=== Error Details ===");
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}
