package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.DriverLoader;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.List;

@Command(name = "list", description = "List installed drivers")
public class DriverListCommand implements Runnable {

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        DriverLoader loader = new DriverLoader(cm.getDriverDir().toFile());
        List<File> drivers = loader.listDrivers();

        if (drivers.isEmpty()) {
            System.out.println("No drivers installed. Use 'sql-cli driver install <type>' to install.");
            return;
        }

        System.out.printf("%-45s %-12s%n", "Driver File", "Size");
        System.out.println("-".repeat(60));

        for (File driver : drivers) {
            String size = formatSize(driver.length());
            System.out.printf("%-45s %-12s%n", driver.getName(), size);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
