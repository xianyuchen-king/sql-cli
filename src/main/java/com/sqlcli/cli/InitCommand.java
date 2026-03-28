package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.EncryptionService;
import picocli.CommandLine.Command;

import java.io.File;

@Command(name = "init", description = "Initialize sql-cli configuration")
public class InitCommand implements Runnable {

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        File configDir = cm.getConfigDir().toFile();

        if (cm.getConfigFile().toFile().exists()) {
            System.out.println("Config already exists at " + cm.getConfigFile());
            return;
        }

        cm.init();
        System.out.println("Initialized sql-cli configuration at " + cm.getConfigFile());
        System.out.println("Driver directory: " + cm.getDriverDir());

        String envVar = cm.load().getEncryption().getKeyEnv();
        if (System.getenv(envVar) == null) {
            String secret = EncryptionService.generateSecret();
            System.out.println("\nEncryption key not set. Add this to your shell profile:");
            System.out.println("  export " + envVar + "=\"" + secret + "\"");
        }
    }
}
