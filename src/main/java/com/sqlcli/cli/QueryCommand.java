package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.executor.QueryExecutor;
import com.sqlcli.output.OutputFormatter;
import com.sqlcli.safety.SafetyGuard;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

@Command(name = "query", description = "Execute a SELECT query")
public class QueryCommand implements Runnable {

    @Parameters(paramLabel = "SQL", description = "SQL statement to execute")
    private String sql;

    @Option(names = {"-c", "--connection"}, description = "Connection name")
    private String connection;

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

    @Option(names = {"-f", "--format"}, description = "Output format: markdown/json/csv/agent-json")
    private String format;

    @Option(names = {"--limit"}, type = Integer.class, description = "Max rows to return")
    private Integer limit;

    @Option(names = {"--no-limit"}, description = "Disable row limit")
    private boolean noLimit;

    @Option(names = {"--timeout"}, type = Integer.class, description = "Query timeout in seconds")
    private Integer timeout;

    @Option(names = {"-o", "--output"}, description = "Output file path")
    private String output;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        AppConfig config = cm.load();

        ConnectionOptions opts = new ConnectionOptions(cm, connMgr);
        ConnectionConfig inlineConfig = opts.buildInlineConfig(type, host, port, user, password, db, url, driver, driverClass);
        ConnectionConfig resolved = connMgr.resolveConnection(connection, inlineConfig);

        SafetyGuard guard = new SafetyGuard();
        String validatedSql = guard.validate(sql, resolved, false, noLimit,
                limit != null ? limit : 0, config);

        String outputFormat = format != null ? format : config.getDefaults().getOutputFormat();
        OutputFormatter formatter = OutputFormatter.create(outputFormat);

        try (Connection conn = connMgr.connect(resolved)) {
            QueryExecutor executor = new QueryExecutor();
            int maxRows = noLimit ? 0 : (limit != null ? limit : config.getDefaults().getMaxRows());
            int timeoutSec = timeout != null ? timeout : 0;
            String result = executor.execute(conn, validatedSql, formatter, maxRows, timeoutSec);

            if (output != null) {
                Files.writeString(Path.of(output), result + "\n");
                System.out.println("Result written to " + output);
            } else {
                System.out.println(result);
            }
        } catch (Exception e) {
            CliErrorHandler.handleError(e, outputFormat);
        }
    }
}
