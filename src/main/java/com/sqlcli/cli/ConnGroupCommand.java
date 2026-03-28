package com.sqlcli.cli;

import picocli.CommandLine.Command;

@Command(name = "group", description = "Manage connection groups",
        subcommands = {ConnGroupListCommand.class})
public class ConnGroupCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use a subcommand. See --help.");
    }
}
