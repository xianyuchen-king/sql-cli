package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.config.EncryptionService;
import com.sqlcli.output.AgentResult;
import com.sqlcli.output.ErrorCode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.LinkedHashMap;
import java.util.Map;

@Command(name = "show", description = "Show connection details")
public class ConnShowCommand implements Runnable {

    @Parameters(paramLabel = "NAME", description = "Connection name")
    private String name;

    @Option(names = {"-f", "--format"}, description = "Output format: markdown/json/agent-json")
    private String format;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        AppConfig config = cm.load();
        ConnectionConfig cc = config.getConnection(name);

        if (cc == null) {
            if (CliErrorHandler.isJsonFormat(format)) {
                System.out.println(AgentResult.error(ErrorCode.CONNECTION_NOT_FOUND, "Connection not found: " + name).toJson());
            } else {
                System.err.println("[ERROR] Connection not found: " + name);
            }
            return;
        }

        if (CliErrorHandler.isJsonFormat(format)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", cc.getName());
            data.put("type", cc.getType());
            data.put("group", cc.getGroup());
            data.put("tags", cc.getTags());
            data.put("safety", cc.getSafetyLevel() != null ? cc.getSafetyLevel() : "normal");
            if (cc.isDirectUrl()) {
                data.put("url", cc.getUrl());
            } else {
                data.put("host", cc.getHost());
                data.put("port", cc.getPort());
                data.put("database", cc.getDb());
            }
            data.put("user", cc.getUser());
            data.put("password", EncryptionService.maskPassword(cc.getPassword()));
            data.put("driver", cc.getDriver());
            data.put("driver_class", cc.getDriverClass());
            if (cc.getParams() != null && !cc.getParams().isEmpty()) {
                data.put("params", cc.getParams());
            }
            System.out.println(AgentResult.ok(data).toJson());
            return;
        }

        System.out.println("Name:         " + cc.getName());
        System.out.println("Type:         " + cc.getType());
        System.out.println("Group:        " + (cc.getGroup() != null ? cc.getGroup() : "-"));
        System.out.println("Tags:         " + (cc.getTags() != null ? String.join(", ", cc.getTags()) : "-"));
        System.out.println("Safety:       " + (cc.getSafetyLevel() != null ? cc.getSafetyLevel() : "normal"));

        if (cc.isDirectUrl()) {
            System.out.println("URL:          " + cc.getUrl());
        } else {
            System.out.println("Host:         " + (cc.getHost() != null ? cc.getHost() : "-"));
            System.out.println("Port:         " + (cc.getPort() != null ? cc.getPort() : "-"));
            System.out.println("Database:     " + (cc.getDb() != null ? cc.getDb() : "-"));
        }

        System.out.println("User:         " + cc.getUser());
        System.out.println("Password:     " + EncryptionService.maskPassword(cc.getPassword()));
        System.out.println("Driver:       " + (cc.getDriver() != null ? cc.getDriver() : "-"));
        System.out.println("Driver Class: " + (cc.getDriverClass() != null ? cc.getDriverClass() : "-"));

        if (cc.getParams() != null && !cc.getParams().isEmpty()) {
            System.out.println("Params:       " + cc.getParams());
        }
    }
}
