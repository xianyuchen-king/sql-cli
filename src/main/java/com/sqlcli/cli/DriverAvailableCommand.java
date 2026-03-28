package com.sqlcli.cli;

import com.sqlcli.driver.DriverRegistry;
import picocli.CommandLine.Command;

@Command(name = "available", description = "List available drivers to install")
public class DriverAvailableCommand implements Runnable {

    @Override
    public void run() {
        System.out.printf("%-15s %-15s %-40s%n", "Type", "Version", "Driver Class");
        System.out.println("-".repeat(75));

        DriverRegistry.all().forEach((type, info) -> {
            System.out.printf("%-15s %-15s %-40s%n", type, info.defaultVersion(), info.driverClass());
        });

        System.out.println("\nInstall with: sql-cli driver install <type> [--version <version>]");
    }
}
