package com.sqlcli.cli;

import picocli.CommandLine.Command;

import java.util.Map;

@Command(name = "available", description = "List available drivers to install")
public class DriverAvailableCommand implements Runnable {

    private static final Map<String, String[]> AVAILABLE = Map.of(
            "mysql", new String[]{"8.3.0", "com.mysql.cj.jdbc.Driver"},
            "oracle", new String[]{"23.3.0.23.09", "oracle.jdbc.OracleDriver"},
            "postgresql", new String[]{"42.7.2", "org.postgresql.Driver"},
            "sqlite", new String[]{"3.45.1.0", "org.sqlite.JDBC"}
    );

    @Override
    public void run() {
        System.out.printf("%-15s %-15s %-40s%n", "Type", "Version", "Driver Class");
        System.out.println("-".repeat(75));

        for (var entry : AVAILABLE.entrySet()) {
            System.out.printf("%-15s %-15s %-40s%n", entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
        }

        System.out.println("\nInstall with: sql-cli driver install <type> [--version <version>]");
    }
}
