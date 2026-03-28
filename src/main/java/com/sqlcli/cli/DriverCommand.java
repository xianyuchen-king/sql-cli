package com.sqlcli.cli;

import picocli.CommandLine.Command;

@Command(name = "driver", description = "Manage JDBC drivers",
        subcommands = {
                DriverInstallCommand.class,
                DriverListCommand.class,
                DriverAvailableCommand.class,
                DriverRemoveCommand.class
        })
public class DriverCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use a subcommand. See --help.");
    }
}
