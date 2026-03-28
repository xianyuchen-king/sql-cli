package com.sqlcli.cli;

import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.executor.MetaExecutor;
import com.sqlcli.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.sql.Connection;

@Command(name = "dbs", description = "List all databases/schemas")
public class MetaDbsCommand implements Runnable {

    @ParentCommand
    private MetaCommand parent;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        ConnectionConfig resolved = connMgr.resolveConnection(parent.connection, parent.buildInlineConfig());

        try (Connection conn = connMgr.connect(resolved)) {
            MetaExecutor executor = new MetaExecutor();
            OutputFormatter formatter = OutputFormatter.create(cm.load().getDefaults().getOutputFormat());
            System.out.println(executor.listDatabases(conn, formatter));
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }
}
