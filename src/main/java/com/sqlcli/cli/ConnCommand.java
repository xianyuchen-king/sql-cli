package com.sqlcli.cli;

import picocli.CommandLine.Command;

@Command(name = "conn", description = "Manage database connections",
        subcommands = {
                ConnAddCommand.class,
                ConnListCommand.class,
                ConnShowCommand.class,
                ConnUpdateCommand.class,
                ConnRemoveCommand.class,
                ConnTestCommand.class,
                ConnGroupCommand.class,
                ConnTagCommand.class,
                ConnTypesCommand.class,
                ConnRegisterTypeCommand.class,
                ConnRemoveTypeCommand.class
        })
public class ConnCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use a subcommand. See --help.");
    }
}
