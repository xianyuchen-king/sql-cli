package com.sqlcli.cli;

import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.executor.MetaExecutor;
import com.sqlcli.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.sql.Connection;

@Command(name = "describe", description = "Show detailed table structure (columns, PKs, indexes, foreign keys)")
public class MetaDescribeCommand implements Runnable {

    @ParentCommand
    private MetaCommand parent;

    @Option(names = {"-t", "--table"}, required = true, description = "Table name")
    private String table;

    @Option(names = {"-d", "--database"}, description = "Database/schema name")
    private String database;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        ConnectionConfig resolved = connMgr.resolveConnection(parent.connection, parent.buildInlineConfig());

        try (Connection conn = connMgr.connect(resolved)) {
            MetaExecutor executor = new MetaExecutor();
            OutputFormatter formatter = parent.resolveFormatter(cm);
            String schema = parent.resolveEffectiveSchema(database, resolved);
            System.out.println(executor.describeTable(conn, schema, table, formatter));
        } catch (Exception e) {
            CliErrorHandler.handleError(e, parent.format);
        }
    }
}
