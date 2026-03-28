package com.sqlcli.executor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransferExecutor {

    /**
     * Export table data to a file or stdout in the specified format.
     */
    public void export(Connection conn, String table, String format, String outputPath,
                       String[] whereColumns, OutputWriter writer) throws Exception {
        String sql = "SELECT * FROM " + quoteTable(conn, table);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            if (outputPath != null) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
                    writeExport(rs, columns, format, whereColumns, pw, writer);
                }
                System.out.println("[DONE] Exported to " + outputPath);
            } else {
                writeExport(rs, columns, format, whereColumns, new PrintWriter(System.out), writer);
            }
        }
    }

    private void writeExport(ResultSet rs, List<String> columns, String format,
                              String[] whereColumns, PrintWriter pw, OutputWriter writer) throws Exception {
        int rowCount = 0;
        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columns.size(); i++) {
                row.add(rs.getObject(i));
            }

            String line = switch (format.toLowerCase()) {
                case "json" -> writer.toJson(columns, row);
                case "insert" -> writer.toInsert(columns, row);
                case "update" -> writer.toUpdate(columns, row, whereColumns);
                default -> writer.toCsv(columns, row);
            };
            pw.println(line);
            rowCount++;
        }
        pw.flush();
        System.out.println("[DONE] Exported " + rowCount + " rows.");
    }

    /**
     * Import data from a file.
     */
    public void importData(Connection conn, String table, String filePath, String format,
                           int batchSize, String onError) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        if (lines.isEmpty()) {
            System.out.println("[DONE] Empty file, nothing to import.");
            return;
        }

        // Parse header
        String[] headers = parseCsvLine(lines.getFirst());

        int success = 0, failed = 0;
        conn.setAutoCommit(false);

        try {
            PreparedStatement ps = null;
            int batchCount = 0;

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                try {
                    switch (format.toLowerCase()) {
                        case "insert" -> {
                            int count = executeUpdate(conn, line);
                            success += count;
                        }
                        case "update" -> {
                            int count = executeUpdate(conn, line);
                            success += count;
                        }
                        default -> {
                            // CSV - batch insert
                            if (ps == null) {
                                String sql = buildInsertSql(table, headers);
                                ps = conn.prepareStatement(sql);
                            }
                            String[] values = parseCsvLine(line);
                            for (int j = 0; j < headers.length && j < values.length; j++) {
                                ps.setString(j + 1, values[j]);
                            }
                            ps.addBatch();
                            batchCount++;
                            if (batchCount >= batchSize) {
                                int[] results = ps.executeBatch();
                                for (int r : results) success += (r >= 0 ? r : 1);
                                batchCount = 0;
                            }
                        }
                    }
                } catch (Exception e) {
                    if ("skip".equalsIgnoreCase(onError)) {
                        failed++;
                    } else {
                        conn.rollback();
                        throw new RuntimeException("Import failed at line " + (i + 1) + ": " + e.getMessage(), e);
                    }
                }
            }

            // Flush remaining batch
            if (ps != null && batchCount > 0) {
                int[] results = ps.executeBatch();
                for (int r : results) success += (r >= 0 ? r : 1);
            }

            conn.commit();
            System.out.println("[DONE] Success: " + success + " rows, Failed: " + failed + " rows");
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private int executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    private String buildInsertSql(String table, String[] columns) {
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(quoteIdentifier(table)).append(" (");
        sb.append(String.join(", ", columns));
        sb.append(") VALUES (");
        sb.append(String.join(", ", java.util.Collections.nCopies(columns.length, "?")));
        sb.append(")");
        return sb.toString();
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }

    private String quoteTable(Connection conn, String table) {
        try {
            String quote = conn.getMetaData().getIdentifierQuoteString();
            return quote + table.replace(quote, quote + quote) + quote;
        } catch (SQLException e) {
            return table;
        }
    }

    private String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    /**
     * Callback interface for format-specific output generation.
     */
    public interface OutputWriter {
        String toCsv(List<String> columns, List<Object> row);
        String toJson(List<String> columns, List<Object> row);
        String toInsert(List<String> columns, List<Object> row);
        String toUpdate(List<String> columns, List<Object> row, String[] whereColumns);
    }
}
