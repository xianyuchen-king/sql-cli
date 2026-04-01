package com.sqlcli.cli;

import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.executor.MetaExecutor;
import com.sqlcli.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParentCommand;

import java.sql.Connection;

@Command(name = "dbs", description = "List all databases/schemas")
public class MetaDbsCommand implements Runnable {

    @ParentCommand
    private MetaCommand parent;

    @Mixin
    private MetaConnectionMixin opts = new MetaConnectionMixin();

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        String connName = opts.getConnection() != null ? opts.getConnection() : parent.opts.getConnection();
        String resolvedFmt = opts.getFormat() != null ? opts.resolveFormat(cm) : parent.opts.resolveFormat(cm);
        ConnectionConfig inlineConfig = opts.getConnection() != null ? opts.buildInlineConfig() : parent.opts.buildInlineConfig();

        try {
            ConnectionConfig resolved = connMgr.resolveConnection(connName, inlineConfig);
            try (Connection conn = connMgr.connect(resolved)) {
                MetaExecutor executor = new MetaExecutor();
                OutputFormatter formatter = OutputFormatter.create(resolvedFmt);
                com.sqlcli.dialect.Dialect dialect = com.sqlcli.dialect.DialectFactory.getDialect(resolved.getType(), cm.load());
                System.out.println(executor.listDatabases(conn, dialect, formatter));
            }
        } catch (Exception e) {
            CliErrorHandler.handleError(e, resolvedFmt);
        }
    }
}
