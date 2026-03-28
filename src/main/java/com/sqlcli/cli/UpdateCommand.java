package com.sqlcli.cli;

import picocli.CommandLine.Command;

@Command(name = "update", description = "Update sql-cli",
        subcommands = {UpdateCheckCommand.class})
public class UpdateCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("TODO: implement update (download latest jar from GitHub Releases)");
    }
}
