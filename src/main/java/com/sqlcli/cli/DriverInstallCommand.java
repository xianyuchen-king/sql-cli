package com.sqlcli.cli;

import com.sqlcli.config.ConfigManager;
import com.sqlcli.driver.DriverRegistry;
import com.sqlcli.driver.DriverRegistry.DriverInfo;
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

@Command(name = "install", description = "Install a JDBC driver")
public class DriverInstallCommand implements Runnable {

    @Parameters(paramLabel = "TYPE", description = "Database type (mysql/oracle/postgresql/sqlite)")
    private String type;

    @Option(names = {"--version"}, description = "Driver version")
    private String version;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        DriverInfo info = DriverRegistry.get(type);

        if (info == null) {
            System.err.println("[ERROR] Unknown driver type: " + type);
            System.err.println("Available types: " + String.join(", ", DriverRegistry.all().keySet()));
            return;
        }

        String driverVersion = version != null ? version : info.defaultVersion();
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
}
