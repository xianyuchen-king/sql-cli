package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;

@Command(name = "remove", description = "Remove a database connection")
public class ConnRemoveCommand implements Runnable {

    @Parameters(paramLabel = "NAME", description = "Connection name")
    private String name;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();
        ConnectionConfig cc = config.getConnection(name);

        if (cc == null) {
            System.err.println("[ERROR] Connection not found: " + name);
            return;
        }

        List<ConnectionConfig> connections = new ArrayList<>(config.getConnections());
        connections.removeIf(c -> c.getName().equals(name));
        config.setConnections(connections);
        cm.save(config);

        System.out.println("[DONE] Connection '" + name + "' removed.");
    }
}
