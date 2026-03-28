package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "uninstall", description = "Uninstall sql-cli")
public class UninstallCommand implements Runnable {

    @Option(names = {"--confirm"}, description = "Confirm uninstallation")
    private boolean confirm;

    @Override
    public void run() {
        if (!confirm) {
            System.err.println("[WARN] This will remove ~/.sql-cli/ including all config and drivers.");
            System.err.println("       Use --confirm to proceed.");
            return;
        }

        ConfigManager cm = new ConfigManager();
        Path configDir = cm.getConfigDir();

        try {
            if (Files.exists(configDir)) {
                deleteRecursive(configDir);
                System.out.println("[DONE] sql-cli uninstalled. Removed: " + configDir);
            } else {
                System.out.println("Nothing to remove.");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to uninstall: " + e.getMessage());
        }
    }

    private void deleteRecursive(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(p -> {
                    try { deleteRecursive(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
        Files.delete(path);
    }
}
