package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "test", description = "Test a database connection")
public class ConnTestCommand implements Runnable {

    @Parameters(paramLabel = "NAME", description = "Connection name")
    private String name;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);

        ConnectionConfig config = connMgr.resolveConnection(name, null);
        boolean ok = connMgr.testConnection(config);

        if (ok) {
            System.out.println("[OK] Connection '" + name + "' is reachable.");
        } else {
            System.out.println("[FAIL] Connection '" + name + "' is NOT reachable.");
        }
    }
}
