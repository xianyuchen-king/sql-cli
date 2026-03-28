package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.executor.TransferExecutor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.sql.Connection;

@Command(name = "import", description = "Import data")
public class ImportCommand implements Runnable {

    @Option(names = {"-c", "--connection"}, description = "Connection name")
    private String connection;

    @Option(names = {"-t", "--table"}, description = "Table name")
    private String table;

    @Option(names = {"-f", "--file"}, required = true, description = "Input file path")
    private String file;

    @Option(names = {"--format"}, description = "Import format: csv/json/insert/update (default: auto-detect)")
    private String format;

    @Option(names = {"--batch-size"}, type = Integer.class, description = "Batch size (default 500)")
    private Integer batchSize;

    @Option(names = {"--on-error"}, description = "Error handling: abort/skip (default abort)")
    private String onError;

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

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);

        ConnectionOptions opts = new ConnectionOptions(cm, connMgr);
        ConnectionConfig inlineConfig = opts.buildInlineConfig(type, host, port, user, password, db, url, driver, driverClass);
        ConnectionConfig resolved = connMgr.resolveConnection(connection, inlineConfig);

        String detectedFormat = format;
        if (detectedFormat == null) {
            if (file.endsWith(".csv")) detectedFormat = "csv";
            else if (file.endsWith(".json")) detectedFormat = "json";
            else if (file.endsWith(".sql")) detectedFormat = "insert";
            else detectedFormat = "csv";
        }

        int batch = batchSize != null ? batchSize : 500;
        String errorHandling = onError != null ? onError : "abort";

        TransferExecutor executor = new TransferExecutor();
        try (Connection conn = connMgr.connect(resolved)) {
            executor.importData(conn, table, file, detectedFormat, batch, errorHandling);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }
}
