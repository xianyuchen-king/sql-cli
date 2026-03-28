package com.sqlcli.cli;

import picocli.CommandLine.Command;

@Command(name = "tag", description = "Manage connection tags",
        subcommands = {ConnTagListCommand.class})
public class ConnTagCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Use a subcommand. See --help.");
    }
}
