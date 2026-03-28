package com.sqlcli.cli;

import com.sqlcli.Version;
import picocli.CommandLine.Command;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Command(name = "check", description = "Check for new version")
public class UpdateCheckCommand implements Runnable {

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
            String latestTag = extractJsonValue(body, "tag_name");
            if (latestTag == null) {
                System.out.println("failed");
                System.err.println("[ERROR] Could not parse release info");
                return;
            }

            String latest = latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;

            if (latest.equals(current)) {
                System.out.println("up to date!");
            } else {
                System.out.println();
                System.out.println("New version available: " + latest);
                System.out.println("Run 'sql-cli update' to upgrade.");
            }
        } catch (Exception e) {
            System.out.println("failed");
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    static String extractJsonValue(String json, String key) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + needle.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
}
