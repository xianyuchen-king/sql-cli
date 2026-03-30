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

@Command(name = "tables", description = "List tables")
public class MetaTablesCommand implements Runnable {

    @ParentCommand
    private MetaCommand parent;

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
            System.out.println(executor.listTables(conn, schema, formatter));
        } catch (Exception e) {
            CliErrorHandler.handleError(e, parent.format);
        }
    }
}
