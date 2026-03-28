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

@Command(name = "indexes", description = "List table indexes")
public class MetaIndexesCommand implements Runnable {

    @ParentCommand
    private MetaCommand parent;

    @Option(names = {"-t", "--table"}, required = true, description = "Table name")
    private String table;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        ConnectionConfig resolved = connMgr.resolveConnection(parent.connection, parent.buildInlineConfig());

        try (Connection conn = connMgr.connect(resolved)) {
            MetaExecutor executor = new MetaExecutor();
            OutputFormatter formatter = OutputFormatter.create(cm.load().getDefaults().getOutputFormat());
            System.out.println(executor.listIndexes(conn, null, table, formatter));
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }
}
