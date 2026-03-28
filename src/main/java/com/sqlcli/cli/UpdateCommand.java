package com.sqlcli.cli;

import com.sqlcli.Version;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Command(name = "update", description = "Update sql-cli to latest version",
        subcommands = {UpdateCheckCommand.class})
public class UpdateCommand implements Runnable {

    @Option(names = {"--yes", "-y"}, description = "Skip confirmation prompt")
    private boolean yes;

    private static final String RELEASES_API =
            "https://api.github.com/repos/xianyuchen-king/sql-cli/releases/latest";

    @Override
    public void run() {
        String current = Version.get();
        System.out.println("Current version: sql-cli " + current);
        System.out.print("Checking for updates... ");

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // Fetch latest release info
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RELEASES_API))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("failed");
                System.err.println("[ERROR] Could not check for updates (HTTP " + response.statusCode() + ")");
                return;
            }

            String body = response.body();
            String latestTag = UpdateCheckCommand.extractJsonValue(body, "tag_name");
            String htmlUrl = UpdateCheckCommand.extractJsonValue(body, "html_url");

            if (latestTag == null) {
                System.out.println("failed");
                System.err.println("[ERROR] Could not parse release info");
                return;
            }

            String latest = latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;

            if (latest.equals(current)) {
                System.out.println("already up to date!");
                return;
            }

            System.out.println();
            System.out.println("New version available: " + latest);

            if (!yes) {
                System.out.print("Update to " + latest + "? [y/N] ");
                String input = System.console() != null
                        ? System.console().readLine().trim()
                        : new java.util.Scanner(System.in).nextLine().trim();
                if (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("yes")) {
                    System.out.println("Cancelled.");
                    return;
                }
            }

            // Find the jar download URL
            String downloadUrl = null;
            int assetsIdx = body.indexOf("\"assets\"");
            if (assetsIdx > 0) {
                String assetsSection = body.substring(assetsIdx);
                // Look for browser_download_url ending with .jar
                int dlIdx = 0;
                while (dlIdx < assetsSection.length()) {
                    int found = assetsSection.indexOf("browser_download_url", dlIdx);
                    if (found < 0) break;
                    int urlStart = assetsSection.indexOf('"', found + 25);
                    if (urlStart < 0) break;
                    int urlEnd = assetsSection.indexOf('"', urlStart + 1);
                    if (urlEnd < 0) break;
                    String url = assetsSection.substring(urlStart + 1, urlEnd);
                    if (url.endsWith(".jar")) {
                        downloadUrl = url;
                        break;
                    }
                    dlIdx = urlEnd + 1;
                }
            }

            if (downloadUrl == null) {
                System.err.println("[ERROR] Could not find download URL. Visit: " + htmlUrl);
                return;
            }

            // Determine target path
            Path currentJar = getCurrentJarPath();
            if (currentJar == null) {
                System.err.println("[ERROR] Could not determine current jar location");
                return;
            }

            System.out.println("Downloading sql-cli " + latest + "...");

            Path tempFile = Files.createTempFile("sql-cli-update-", ".jar");
            try {
                HttpRequest dlRequest = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .timeout(Duration.ofSeconds(300))
                        .GET()
                        .build();

                HttpResponse<Path> dlResponse = client.send(dlRequest,
                        HttpResponse.BodyHandlers.ofFile(tempFile));

                if (dlResponse.statusCode() != 200) {
                    System.err.println("[ERROR] Download failed: HTTP " + dlResponse.statusCode());
                    return;
                }

                // Replace the current jar
                Files.copy(tempFile, currentJar, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[DONE] Updated to sql-cli " + latest);
                System.out.println("Restart sql-cli to use the new version.");
            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (Exception e) {
            System.out.println("failed");
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private Path getCurrentJarPath() {
        try {
            String path = UpdateCommand.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File f = new File(path);
            if (f.isFile() && f.getName().endsWith(".jar")) {
                return f.toPath();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
