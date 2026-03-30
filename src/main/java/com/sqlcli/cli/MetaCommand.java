package com.sqlcli.cli;

import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "meta", description = "Browse database metadata",
        subcommands = {
                MetaDbsCommand.class,
                MetaTablesCommand.class,
                MetaColumnsCommand.class,
                MetaIndexesCommand.class,
                MetaViewsCommand.class,
                MetaDescribeCommand.class
        })
public class MetaCommand implements Runnable {

    @Option(names = {"-c", "--connection"}, description = "Connection name")
    protected String connection;

    @Option(names = {"-f", "--format"}, description = "Output format: markdown/json/csv/agent-json")
    protected String format;

    @Option(names = {"--url"}, description = "JDBC URL")
    protected String url;

    @Option(names = {"--type"}, description = "Database type")
    protected String type;

    @Option(names = {"--host"}, description = "Database host")
    protected String host;

    @Option(names = {"--port"}, type = Integer.class, description = "Database port")
    protected Integer port;

    @Option(names = {"--user"}, description = "Username")
    protected String user;

    @Option(names = {"--password"}, description = "Password")
    protected String password;

    @Option(names = {"--db"}, description = "Database name")
    protected String db;

    @Option(names = {"--driver"}, description = "Driver jar file name")
    protected String driver;

    @Option(names = {"--driver-class"}, description = "Driver class name")
    protected String driverClass;

    protected ConnectionConfig buildInlineConfig() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        ConnectionOptions opts = new ConnectionOptions(cm, connMgr);
        return opts.buildInlineConfig(type, host, port, user, password, db, url, driver, driverClass);
    }

    /**
     * Resolve effective schema: use subcommand's -d if provided, otherwise fall back to connection's db.
     */
    protected String resolveEffectiveSchema(String subcommandSchema, ConnectionConfig resolved) {
        if (subcommandSchema != null && !subcommandSchema.isBlank()) {
            return subcommandSchema;
        }
        return resolved.getDb();
    }

    /**
     * Resolve output formatter: use --format if provided, otherwise fall back to config default.
     */
    protected OutputFormatter resolveFormatter(ConfigManager cm) {
        String fmt = format != null ? format : cm.load().getDefaults().getOutputFormat();
        return OutputFormatter.create(fmt);
    }

    @Override
    public void run() {
        System.out.println("Use a subcommand. See --help.");
    }
}
