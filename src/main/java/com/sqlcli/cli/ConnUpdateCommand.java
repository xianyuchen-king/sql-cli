package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.EncryptionService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

@Command(name = "update", description = "Update a database connection")
public class ConnUpdateCommand implements Runnable {

    @Parameters(paramLabel = "NAME", description = "Connection name")
    private String name;

    @Option(names = {"--type"}, description = "Database type")
    private String type;

    @Option(names = {"--host"}, description = "Database host")
    private String host;

    @Option(names = {"--port"}, type = Integer.class, description = "Database port")
    private Integer port;

    @Option(names = {"--user"}, description = "Username")
    private String user;

    @Option(names = {"--password"}, description = "Password")
    private String password;

    @Option(names = {"--db"}, description = "Database name")
    private String db;

    @Option(names = {"--url"}, description = "JDBC URL")
    private String url;

    @Option(names = {"--driver"}, description = "Driver jar file name")
    private String driver;

    @Option(names = {"--driver-class"}, description = "Driver class name")
    private String driverClass;

    @Option(names = {"--group"}, description = "Connection group")
    private String group;

        @Option(names = {"--rename"}, description = "New connection name")
    private String newName;

    @Option(names = {"--safety-level"}, description = "Safety level: strict/normal/none")
    private String safetyLevel;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();
        ConnectionConfig cc = config.getConnection(name);

        if (cc == null) {
            System.err.println("[ERROR] Connection not found: " + name);
            return;
        }

        if (type != null) cc.setType(type);
        if (host != null) cc.setHost(host);
        if (port != null) cc.setPort(port);
        if (user != null) cc.setUser(user);
        if (db != null) cc.setDb(db);
        if (url != null) cc.setUrl(url);
        if (driver != null) cc.setDriver(driver);
        if (driverClass != null) cc.setDriverClass(driverClass);
        if (group != null) cc.setGroup(group);
        if (safetyLevel != null) cc.setSafetyLevel(safetyLevel);

        if (password != null) {
            try {
                cc.setPassword(cm.getEncryptionService().encrypt(password));
            } catch (Exception e) {
                System.err.println("[WARN] Failed to encrypt password: " + e.getMessage());
                cc.setPassword(password);
            }
        }

        // Handle rename
        if (newName != null && !newName.equals(name)) {
            // Check if new name already exists
            if (config.getConnection(newName) != null) {
                System.err.println("[ERROR] Connection '" + newName + "' already exists.");
                return;
            }
            // Remove old connection first, then update name and add back
            config.getConnections().removeIf(c -> c.getName().equals(name));
            cc.setName(newName);
            config.getConnections().add(cc);
            cm.save(config);
            System.out.println("[DONE] Connection '" + name + "' renamed to '" + newName + "'.");
        } else {
            cm.save(config);
            System.out.println("[DONE] Connection '" + name + "' updated.");
        }
    }
}
