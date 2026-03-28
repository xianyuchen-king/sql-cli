package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.EncryptionService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Scanner;

@Command(name = "add", description = "Add a new database connection")
public class ConnAddCommand implements Runnable {

    @Option(names = {"--non-interactive"}, description = "Non-interactive mode")
    private boolean nonInteractive;

    @Option(names = {"-n", "--name"}, description = "Connection name")
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

    @Option(names = {"--url"}, description = "JDBC URL (for direct connection)")
    private String url;

    @Option(names = {"--driver"}, description = "Driver jar file name")
    private String driver;

    @Option(names = {"--driver-class"}, description = "Driver class name")
    private String driverClass;

    @Option(names = {"--group"}, description = "Connection group")
    private String group;

    @Option(names = {"--safety-level"}, description = "Safety level: strict/normal/none")
    private String safetyLevel;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();

        // Auto-detect non-interactive mode if required params are provided
        if (nonInteractive || (name != null && type != null)) {
            addNonInteractive(cm, config);
        } else {
            addInteractive(cm, config);
        }
    }

    private void addInteractive(ConfigManager cm, AppConfig config) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Connection name: ");
        String connName = scanner.nextLine().trim();
        if (connName.isEmpty()) {
            System.err.println("[ERROR] Connection name is required.");
            return;
        }
        if (config.getConnection(connName) != null) {
            System.err.println("[ERROR] Connection '" + connName + "' already exists.");
            return;
        }

        System.out.print("Database type (mysql/oracle/postgresql/sqlite/generic): ");
        String connType = scanner.nextLine().trim();

        String connUrl = null;
        String connHost = null;
        Integer connPort = null;
        String connDb = null;

        if (!"sqlite".equalsIgnoreCase(connType)) {
            System.out.print("Use direct JDBC URL? (y/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                System.out.print("JDBC URL: ");
                connUrl = scanner.nextLine().trim();
            } else {
                System.out.print("Host: ");
                connHost = scanner.nextLine().trim();
                System.out.print("Port (press Enter for default): ");
                String portStr = scanner.nextLine().trim();
                connPort = portStr.isEmpty() ? null : Integer.parseInt(portStr);
                System.out.print("Database: ");
                connDb = scanner.nextLine().trim();
            }
        } else {
            System.out.print("Database file path: ");
            connDb = scanner.nextLine().trim();
        }

        System.out.print("Username: ");
        String connUser = scanner.nextLine().trim();
        System.out.print("Password: ");
        String connPassword = scanner.nextLine().trim();

        System.out.print("Driver jar file name: ");
        String connDriver = scanner.nextLine().trim();

        System.out.print("Group (optional): ");
        String connGroup = scanner.nextLine().trim();

        System.out.print("Safety level (strict/normal/none, default normal): ");
        String connSafety = scanner.nextLine().trim();
        if (connSafety.isEmpty()) connSafety = null;

        ConnectionConfig cc = new ConnectionConfig();
        cc.setName(connName);
        cc.setType(connType);
        cc.setHost(connHost);
        cc.setPort(connPort);
        cc.setUser(connUser);
        cc.setDb(connDb);
        cc.setUrl(connUrl);
        cc.setDriver(connDriver.isEmpty() ? null : connDriver);
        cc.setGroup(connGroup.isEmpty() ? null : connGroup);
        cc.setSafetyLevel(connSafety);

        if (connPassword != null && !connPassword.isEmpty()) {
            try {
                cc.setPassword(cm.getEncryptionService().encrypt(connPassword));
            } catch (Exception e) {
                System.err.println("[WARN] Failed to encrypt password: " + e.getMessage());
                cc.setPassword(connPassword);
            }
        }

        if (config.getConnections() == null) {
            config.setConnections(new ArrayList<>());
        }
        config.getConnections().add(cc);
        cm.save(config);

        System.out.println("[DONE] Connection '" + connName + "' added.");
    }

    private void addNonInteractive(ConfigManager cm, AppConfig config) {
        if (name == null || name.isBlank()) {
            System.err.println("[ERROR] --name is required");
            return;
        }
        if (config.getConnection(name) != null) {
            System.err.println("[ERROR] Connection '" + name + "' already exists.");
            return;
        }

        ConnectionConfig cc = new ConnectionConfig();
        cc.setName(name);
        cc.setType(type);
        cc.setHost(host);
        cc.setPort(port);
        cc.setUser(user);
        cc.setDb(db);
        cc.setUrl(url);
        cc.setDriver(driver);
        cc.setDriverClass(driverClass);
        cc.setGroup(group);
        cc.setSafetyLevel(safetyLevel);

        if (password != null && !password.isEmpty()) {
            try {
                cc.setPassword(cm.getEncryptionService().encrypt(password));
            } catch (Exception e) {
                System.err.println("[WARN] Failed to encrypt password: " + e.getMessage());
                cc.setPassword(password);
            }
        }

        if (config.getConnections() == null) {
            config.setConnections(new ArrayList<>());
        }
        config.getConnections().add(cc);
        cm.save(config);

        System.out.println("[DONE] Connection '" + name + "' added.");
    }
}
