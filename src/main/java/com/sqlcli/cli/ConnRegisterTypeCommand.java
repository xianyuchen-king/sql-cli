package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.CustomTypeConfig;
import com.sqlcli.dialect.DialectFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;

@Command(name = "register-type", description = "Register a custom database type")
public class ConnRegisterTypeCommand implements Runnable {

    @Option(names = {"-n", "--name"}, required = true, description = "Type name")
    private String name;

    @Option(names = {"--driver"}, required = true, description = "Driver jar file name")
    private String driver;

    @Option(names = {"--driver-class"}, required = true, description = "Driver class name")
    private String driverClass;

    @Option(names = {"--url-template"}, required = true, description = "JDBC URL template")
    private String urlTemplate;

    @Option(names = {"--default-port"}, type = Integer.class, description = "Default port")
    private Integer defaultPort;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();

        var existingTypes = DialectFactory.getAllTypes(config);
        if (existingTypes.containsKey(name.toLowerCase())) {
            System.err.println("[ERROR] Type '" + name + "' already registered (" + existingTypes.get(name.toLowerCase()) + ").");
            return;
        }

        CustomTypeConfig ct = new CustomTypeConfig();
        ct.setName(name.toLowerCase());
        ct.setDriver(driver);
        ct.setDriverClass(driverClass);
        ct.setUrlTemplate(urlTemplate);
        ct.setDefaultPort(defaultPort);

        if (config.getCustomTypes() == null) {
            config.setCustomTypes(new ArrayList<>());
        }
        config.getCustomTypes().add(ct);
        cm.save(config);

        System.out.println("[DONE] Custom type '" + name + "' registered.");
    }
}
