package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import picocli.CommandLine.Command;

import java.util.TreeSet;

@Command(name = "list", description = "List all tags")
public class ConnTagListCommand implements Runnable {

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();

        if (config.getConnections() == null || config.getConnections().isEmpty()) {
            System.out.println("No connections configured.");
            return;
        }

        TreeSet<String> tags = new TreeSet<>();
        for (ConnectionConfig cc : config.getConnections()) {
            if (cc.getTags() != null) {
                tags.addAll(cc.getTags());
            }
        }

        if (tags.isEmpty()) {
            System.out.println("No tags defined.");
            return;
        }

        for (String tag : tags) {
            System.out.println("  " + tag);
        }
    }
}
