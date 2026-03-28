package com.sqlcli.cli;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConfigManager;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.connection.ConnectionManager;
import com.sqlcli.executor.TransferExecutor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.sql.Connection;
import java.util.List;

@Command(name = "export", description = "Export table data")
public class ExportCommand implements Runnable {

    @Option(names = {"-c", "--connection"}, description = "Connection name")
    private String connection;

    @Option(names = {"-t", "--table"}, description = "Table name")
    private String table;

    @Option(names = {"--all-tables"}, description = "Export all tables")
    private boolean allTables;

    @Option(names = {"-d", "--database"}, description = "Database name")
    private String database;

    @Option(names = {"-f", "--format"}, description = "Export format: csv/json/insert/update")
    private String format;

    @Option(names = {"-o", "--output"}, description = "Output file path")
    private String output;

    @Option(names = {"--where-columns"}, split = ",", description = "Columns for WHERE clause (update format)")
    private String[] whereColumns;

    @Option(names = {"--url"}, description = "JDBC URL")
    private String url;

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

    @Option(names = {"--driver"}, description = "Driver jar file name")
    private String driver;

    @Option(names = {"--driver-class"}, description = "Driver class name")
    private String driverClass;

    @Override
    public void run() {
        ConfigManager cm = new ConfigManager();
        ConnectionManager connMgr = new ConnectionManager(cm);
        AppConfig config = cm.load();

        ConnectionOptions opts = new ConnectionOptions(cm, connMgr);
        ConnectionConfig inlineConfig = opts.buildInlineConfig(type, host, port, user, password, db, url, driver, driverClass);
        ConnectionConfig resolved = connMgr.resolveConnection(connection, inlineConfig);

        String exportFormat = format != null ? format : "csv";
        TransferExecutor executor = new TransferExecutor();

        try (Connection conn = connMgr.connect(resolved)) {
            TransferExecutor.OutputWriter writer = createWriter();

            if (allTables) {
                com.sqlcli.executor.MetaExecutor metaExecutor = new com.sqlcli.executor.MetaExecutor();
                String tablesStr = metaExecutor.listTables(conn, database,
                        com.sqlcli.output.OutputFormatter.create("csv"));
                if (tablesStr.startsWith("(no results)")) {
                    System.out.println("[WARN] No tables found.");
                    return;
                }
                String[] tableLines = tablesStr.split("\n");
                for (int i = 1; i < tableLines.length; i++) {
                    String tableName = tableLines[i].split(",")[0].trim();
                    if (!tableName.isEmpty()) {
                        String outFile = output != null
                                ? output + (output.endsWith("/") ? "" : "/") + tableName + "." + exportFormat
                                : null;
                        executor.export(conn, tableName, exportFormat, outFile, whereColumns, writer);
                    }
                }
            } else if (table != null) {
                executor.export(conn, table, exportFormat, output, whereColumns, writer);
            } else {
                System.err.println("[ERROR] Specify --table or --all-tables");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private TransferExecutor.OutputWriter createWriter() {
        return new TransferExecutor.OutputWriter() {
            @Override
            public String toCsv(List<String> columns, List<Object> row) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(",");
                    String val = row.get(i) == null ? "" : row.get(i).toString();
                    if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                        sb.append("\"").append(val.replace("\"", "\"\"")).append("\"");
                    } else {
                        sb.append(val);
                    }
                }
                return sb.toString();
            }

            @Override
            public String toJson(List<String> columns, List<Object> row) {
                StringBuilder sb = new StringBuilder("{");
                for (int i = 0; i < columns.size() && i < row.size(); i++) {
                    if (i > 0) sb.append(",");
                    Object val = row.get(i);
                    sb.append("\"").append(columns.get(i)).append("\": ");
                    if (val == null) sb.append("null");
                    else if (val instanceof Number) sb.append(val);
                    else sb.append("\"").append(val.toString().replace("\"", "\\\"")).append("\"");
                }
                sb.append("}");
                return sb.toString();
            }

            @Override
            public String toInsert(List<String> columns, List<Object> row) {
                StringBuilder sb = new StringBuilder("INSERT INTO ").append(table).append(" (");
                sb.append(String.join(", ", columns));
                sb.append(") VALUES (");
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(", ");
                    Object val = row.get(i);
                    if (val == null) sb.append("NULL");
                    else if (val instanceof Number) sb.append(val);
                    else sb.append("'").append(val.toString().replace("'", "''")).append("'");
                }
                sb.append(");");
                return sb.toString();
            }

            @Override
            public String toUpdate(List<String> columns, List<Object> row, String[] wc) {
                String[] whereCols = wc != null && wc.length > 0 ? wc
                        : new String[]{columns.getFirst()};
                StringBuilder sb = new StringBuilder("UPDATE ").append(table).append(" SET ");
                boolean first = true;
                for (int i = 0; i < columns.size(); i++) {
                    if (isInArray(columns.get(i), whereCols)) continue;
                    if (!first) sb.append(", ");
                    Object val = i < row.size() ? row.get(i) : null;
                    sb.append(columns.get(i)).append(" = ");
                    if (val == null) sb.append("NULL");
                    else if (val instanceof Number) sb.append(val);
                    else sb.append("'").append(val.toString().replace("'", "''")).append("'");
                    first = false;
                }
                sb.append(" WHERE ");
                for (int i = 0; i < whereCols.length; i++) {
                    if (i > 0) sb.append(" AND ");
                    int colIdx = columns.indexOf(whereCols[i]);
                    Object val = colIdx >= 0 && colIdx < row.size() ? row.get(colIdx) : null;
                    sb.append(whereCols[i]).append(" = ");
                    if (val == null) sb.append("NULL");
                    else if (val instanceof Number) sb.append(val);
                    else sb.append("'").append(val.toString().replace("'", "''")).append("'");
                }
                sb.append(";");
                return sb.toString();
            }

            private boolean isInArray(String s, String[] arr) {
                for (String a : arr) { if (a.equalsIgnoreCase(s)) return true; }
                return false;
            }
        };
    }
}
