package com.sqlcli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "sql-cli", mixinStandardHelpOptions = true, versionProvider = com.sqlcli.Version.class,
        subcommands = {
                com.sqlcli.cli.InitCommand.class,
                com.sqlcli.cli.ConnCommand.class,
                com.sqlcli.cli.QueryCommand.class,
                com.sqlcli.cli.ExecCommand.class,
                com.sqlcli.cli.MetaCommand.class,
                com.sqlcli.cli.ExportCommand.class,
                com.sqlcli.cli.ImportCommand.class,
                com.sqlcli.cli.DriverCommand.class,
                com.sqlcli.cli.UpdateCommand.class,
                com.sqlcli.cli.UninstallCommand.class,
                com.sqlcli.cli.ShellCommand.class,
                com.sqlcli.cli.ExploreCommand.class
        })
public class SqlCliApp implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SqlCliApp()).execute(args);
        System.exit(exitCode);
    }
}
