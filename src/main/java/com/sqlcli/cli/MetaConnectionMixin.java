package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.output.OutputFormatter;
import picocli.CommandLine.Option;

/**
 * Shared options mixin for meta subcommands.
 * Allows options to be specified either before or after the subcommand name:
 *   sql-cli meta -c conn tables -d schema   (parent form)
 *   sql-cli meta tables -c conn -d schema    (subcommand form)
 */
public class MetaConnectionMixin {

    @Option(names = {"-c", "--connection"}, description = "Connection name")
    private String connection;

    @Option(names = {"-f", "--format"}, description = "Output format: markdown/json/csv/agent-json")
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

    public String getConnection() {
        return connection;
    }

    /**
     * Build inline ConnectionConfig from CLI parameters.
     */
    public ConnectionConfig buildInlineConfig() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        ConnectionOptions opts = new ConnectionOptions(cm, connMgr);
        return opts.buildInlineConfig(type, host, port, user, password, db, url, driver, driverClass);
    }

    /**
     * Resolve effective schema: use subcommand's -d if provided, otherwise fall back to connection's db.
     */
    public String resolveEffectiveSchema(String subcommandSchema, ConnectionConfig resolved) {
        if (subcommandSchema != null && !subcommandSchema.isBlank()) {
            return subcommandSchema;
        }
        return resolved.getDb();
    }

    /**
     * Resolve output formatter based on --format or config default.
     * @return the resolved formatter
     */
    public OutputFormatter resolveFormatter(ConfigManager cm) {
        return OutputFormatter.create(resolveFormat(cm));
    }

    /**
     * Resolve the output format string (null → config default).
     */
    public String resolveFormat(ConfigManager cm) {
        return format != null ? format : cm.load().getDefaults().getOutputFormat();
    }

    /**
     * Get raw format option value (may be null).
     */
    public String getFormat() {
        return format;
    }
}
