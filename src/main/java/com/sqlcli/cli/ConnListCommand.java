package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "list", description = "List all database connections")
public class ConnListCommand implements Runnable {

    @Option(names = {"-g", "--group"}, description = "Filter by group")
    private String group;

    @Option(names = {"-t", "--tag"}, description = "Filter by tag")
    private String tag;

    @Option(names = {"--type"}, description = "Filter by database type")
    private String type;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();
        List<ConnectionConfig> connections = config.getConnections();

        if (connections == null || connections.isEmpty()) {
            System.out.println("No connections configured. Use 'sql-cli conn add' to add one.");
            return;
        }

        System.out.printf("%-20s %-12s %-20s %-25s %-10s%n",
                "Name", "Type", "Group", "Host", "Safety");
        System.out.println("-".repeat(90));

        for (ConnectionConfig cc : connections) {
            if (group != null && !group.equals(cc.getGroup())) continue;
            if (type != null && !type.equalsIgnoreCase(cc.getType())) continue;
            if (tag != null && (cc.getTags() == null || !cc.getTags().contains(tag))) continue;

            String hostDisplay = cc.isDirectUrl() ? "(direct URL)" :
                    (cc.getHost() != null ? cc.getHost() + ":" + cc.getPort() : "-");
            String safety = cc.getSafetyLevel() != null ? cc.getSafetyLevel() : "normal";

            System.out.printf("%-20s %-12s %-20s %-25s %-10s%n",
                    cc.getName(), cc.getType(), cc.getGroup() != null ? cc.getGroup() : "-",
                    hostDisplay, safety);
        }
    }
}
