package com.sqlcli.cli;

import com.sqlcli.Version;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.shell.ShellSession;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Interactive SQL shell command - enters REPL mode with connection reuse.
 */
@Command(name = "shell", description = "Start interactive SQL shell with connection reuse")
public class ShellCommand implements Runnable {

    @Option(names = {"-c", "--connection"}, description = "Connection name from config")
    private String connection;

    @Option(names = {"--type"}, description = "Database type (mysql, oracle, postgresql, etc.)")
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

    @Option(names = {"--url"}, description = "JDBC URL (for direct connection)")
    private String url;

    @Option(names = {"--driver"}, description = "Driver jar file name")
    private String driver;

    @Option(names = {"--driver-class"}, description = "JDBC driver class name")
    private String driverClass;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);

        ConnectionOptions opts = new ConnectionOptions(cm, connMgr);
        ConnectionConfig inlineConfig = opts.buildInlineConfig(
                type, host, port, user, password, db, url, driver, driverClass);

        ConnectionConfig resolved;
        try {
            resolved = connMgr.resolveConnection(connection, inlineConfig);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to resolve connection: " + e.getMessage());
            System.err.println("\nUsage: sql-cli shell -c <connection-name>");
            System.err.println("   or: sql-cli shell --type mysql --host localhost --port 3306 ...");
            return;
        }

        // Start interactive shell
        try (ShellSession session = new ShellSession(resolved, connMgr)) {
            session.start();
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
        }
    }
}
