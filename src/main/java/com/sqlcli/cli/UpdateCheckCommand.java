package com.sqlcli.cli;

import picocli.CommandLine.Command;

@Command(name = "check", description = "Check for new version")
public class UpdateCheckCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("TODO: implement version check");
    }
}
