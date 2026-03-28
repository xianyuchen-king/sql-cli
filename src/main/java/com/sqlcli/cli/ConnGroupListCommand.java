package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List all connection groups")
public class ConnGroupListCommand implements Runnable {

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();

        if (config.getGroups() == null || config.getGroups().isEmpty()) {
            System.out.println("No groups defined.");
            return;
        }

        for (String group : config.getGroups()) {
            System.out.println("  " + group);
        }
    }
}
