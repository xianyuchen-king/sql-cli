package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.dialect.DialectFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "remove-type", description = "Remove a custom database type")
public class ConnRemoveTypeCommand implements Runnable {

    @Option(names = {"-n", "--name"}, required = true, description = "Type name to remove")
    private String name;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();

        var existingTypes = DialectFactory.getAllTypes(config);
        if (!existingTypes.containsKey(name.toLowerCase())) {
            System.err.println("[ERROR] Type '" + name + "' not found.");
            return;
        }
        if ("builtin".equals(existingTypes.get(name.toLowerCase()))) {
            System.err.println("[ERROR] Cannot remove builtin type '" + name + "'. Only custom types can be removed.");
            return;
        }

        var connections = config.getConnections();
        if (connections != null) {
            long refCount = connections.stream()
                    .filter(c -> c.getType() != null && c.getType().equalsIgnoreCase(name))
                    .count();
            if (refCount > 0) {
                System.err.println("[ERROR] Cannot remove type '" + name + "'. It is referenced by " + refCount + " connection(s). Remove or update those connections first.");
                return;
            }
        }

        var customTypes = config.getCustomTypes();
        if (customTypes != null) {
            customTypes.removeIf(ct -> ct.getName().equalsIgnoreCase(name));
            cm.save(config);
            System.out.println("[DONE] Custom type '" + name + "' removed.");
        }
    }
}
