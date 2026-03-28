package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.dialect.DialectFactory;
import picocli.CommandLine.Command;

@Command(name = "types", description = "List all registered database types")
public class ConnTypesCommand implements Runnable {

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();

        var types = DialectFactory.getAllTypes(config);

        System.out.printf("%-15s %-10s %-40s%n", "Type", "Source", "Default Driver Class");
        System.out.println("-".repeat(70));

        for (var entry : types.entrySet()) {
            String driverClass = DialectFactory.getDefaultDriverClass(entry.getKey(), config);
            System.out.printf("%-15s %-10s %-40s%n",
                    entry.getKey(), entry.getValue(), driverClass != null ? driverClass : "-");
        }
    }
}
