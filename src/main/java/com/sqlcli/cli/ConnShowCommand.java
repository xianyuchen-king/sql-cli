package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.EncryptionService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "show", description = "Show connection details")
public class ConnShowCommand implements Runnable {

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

        System.out.println("Name:         " + cc.getName());
        System.out.println("Type:         " + cc.getType());
        System.out.println("Group:        " + (cc.getGroup() != null ? cc.getGroup() : "-"));
        System.out.println("Tags:         " + (cc.getTags() != null ? String.join(", ", cc.getTags()) : "-"));
        System.out.println("Safety:       " + (cc.getSafetyLevel() != null ? cc.getSafetyLevel() : "normal"));

        if (cc.isDirectUrl()) {
            System.out.println("URL:          " + cc.getUrl());
        } else {
            System.out.println("Host:         " + (cc.getHost() != null ? cc.getHost() : "-"));
            System.out.println("Port:         " + (cc.getPort() != null ? cc.getPort() : "-"));
            System.out.println("Database:     " + (cc.getDb() != null ? cc.getDb() : "-"));
        }

        System.out.println("User:         " + cc.getUser());
        System.out.println("Password:     " + EncryptionService.maskPassword(cc.getPassword()));
        System.out.println("Driver:       " + (cc.getDriver() != null ? cc.getDriver() : "-"));
        System.out.println("Driver Class: " + (cc.getDriverClass() != null ? cc.getDriverClass() : "-"));

        if (cc.getParams() != null && !cc.getParams().isEmpty()) {
            System.out.println("Params:       " + cc.getParams());
        }
    }
}
