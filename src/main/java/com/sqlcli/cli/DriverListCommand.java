package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.DriverLoader;
import com.sqlcli.output.AgentResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Command(name = "list", description = "List installed drivers")
public class DriverListCommand implements Runnable {

    @Option(names = {"-f", "--format"}, description = "Output format: markdown/json/agent-json")
    private String format;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        DriverLoader loader = new DriverLoader(cm.getDriverDir().toFile());
        List<File> drivers = loader.listDrivers();

        if (drivers.isEmpty()) {
            if (CliErrorHandler.isJsonFormat(format)) {
                System.out.println(AgentResult.ok(List.of()).toJson());
            } else {
                System.out.println("No drivers installed. Use 'sql-cli driver install <type>' to install.");
            }
            return;
        }

        if (CliErrorHandler.isJsonFormat(format)) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (File driver : drivers) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("file", driver.getName());
                item.put("size", formatSize(driver.length()));
                items.add(item);
            }
            System.out.println(AgentResult.ok(items).toJson());
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
