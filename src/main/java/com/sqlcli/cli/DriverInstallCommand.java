package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.connection.DriverLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

@Command(name = "install", description = "Install a JDBC driver")
public class DriverInstallCommand implements Runnable {

    private static final Map<String, DriverInfo> DRIVER_INFO = Map.of(
            "mysql", new DriverInfo("com.mysql", "mysql-connector-j", "8.3.0"),
            "oracle", new DriverInfo("com.oracle.database.jdbc", "ojdbc11", "23.3.0.23.09"),
            "postgresql", new DriverInfo("org.postgresql", "postgresql", "42.7.2"),
            "sqlite", new DriverInfo("org.xerial", "sqlite-jdbc", "3.45.1.0")
    );

    @Parameters(paramLabel = "TYPE", description = "Database type (mysql/oracle/postgresql/sqlite)")
    private String type;

    @Option(names = {"--version"}, description = "Driver version")
    private String version;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        DriverInfo info = DRIVER_INFO.get(type.toLowerCase());

        if (info == null) {
            System.err.println("[ERROR] Unknown driver type: " + type);
            System.err.println("Available types: " + String.join(", ", DRIVER_INFO.keySet()));
            return;
        }

        String driverVersion = version != null ? version : info.version();
        String jarName = info.artifactId() + "-" + driverVersion + ".jar";

        File driverDir = cm.getDriverDir().toFile();
        File targetFile = new File(driverDir, jarName);

        if (targetFile.exists()) {
            System.out.println("[INFO] Driver already exists: " + jarName);
            return;
        }

        String mavenUrl = "https://repo1.maven.org/maven2/"
                + info.groupId().replace(".", "/") + "/" + info.artifactId()
                + "/" + driverVersion + "/" + jarName;

        System.out.println("Downloading " + jarName + "...");

        try {
            driverDir.mkdirs();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mavenUrl))
                    .timeout(Duration.ofSeconds(120))
                    .GET()
                    .build();

            HttpResponse<java.nio.file.Path> response = client.send(request,
                    HttpResponse.BodyHandlers.ofFile(targetFile.toPath()));

            if (response.statusCode() == 200) {
                System.out.println("[DONE] Driver installed: " + jarName);
            } else {
                Files.deleteIfExists(targetFile.toPath());
                System.err.println("[ERROR] Download failed: HTTP " + response.statusCode());
                System.err.println("You can manually download and place it in: " + driverDir);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Download failed: " + e.getMessage());
            System.err.println("You can manually download and place it in: " + driverDir);
        }
    }

    record DriverInfo(String groupId, String artifactId, String version) {}
}
