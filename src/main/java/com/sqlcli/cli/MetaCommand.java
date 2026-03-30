package com.sqlcli.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "meta", description = "Browse database metadata",
        subcommands = {
                MetaDbsCommand.class,
                MetaTablesCommand.class,
                MetaColumnsCommand.class,
                MetaIndexesCommand.class,
                MetaViewsCommand.class,
                MetaDescribeCommand.class
        })
public class MetaCommand implements Runnable {

    @Mixin
    protected MetaConnectionMixin opts = new MetaConnectionMixin();

    @Override
    public void run() {
        System.out.println("Use a subcommand. See --help.");
    }
}
