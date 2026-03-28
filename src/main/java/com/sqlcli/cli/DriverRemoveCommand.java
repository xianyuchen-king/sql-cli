package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.DriverLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "remove", description = "Remove a driver")
public class DriverRemoveCommand implements Runnable {

    @Parameters(paramLabel = "JAR", description = "Driver jar file name")
    private String jar;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        DriverLoader loader = new DriverLoader(cm.getDriverDir().toFile());

        boolean removed = loader.removeDriver(jar);
        if (removed) {
            System.out.println("[DONE] Driver removed: " + jar);
        } else {
            System.err.println("[ERROR] Driver not found: " + jar);
        }
    }
}
