package com.sqlcli.executor;

import com.sqlcli.output.AgentJsonFormatter;
import com.sqlcli.output.CsvFormatter;
import com.sqlcli.output.JsonFormatter;
import com.sqlcli.output.OutputFormatter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QueryExecutor {

    /**
     * Execute a SELECT query and return formatted results.
     * @return formatted string output
     */
    public String execute(Connection conn, String sql, OutputFormatter formatter, int maxRows, int timeoutSeconds)
            throws Exception {
        try (Statement stmt = conn.createStatement()) {
            if (maxRows > 0) {
                stmt.setMaxRows(maxRows);
            }
            if (timeoutSeconds > 0) {
                stmt.setQueryTimeout(timeoutSeconds);
            }
            long start = System.nanoTime();
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                List<List<Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }

                long elapsed = System.nanoTime() - start;
                double seconds = elapsed / 1_000_000_000.0;

                // For structured JSON formats, include timing in the envelope (no plaintext suffix)
                if (formatter instanceof AgentJsonFormatter ajf) {
                    return ajf.formatQueryEnvelope(columns, rows, seconds);
                }
                if (formatter instanceof JsonFormatter) {
                    return formatter.formatQuery(columns, rows);
                }

                // For markdown/csv, output plaintext summary (position depends on formatter)
                String summary = rows.size() + " row" + (rows.size() != 1 ? "s" : "")
                        + " in set (" + String.format("%.2f", seconds) + "s)";
                String formatted = formatter.formatQuery(columns, rows);
                if (formatter instanceof CsvFormatter) {
                    return formatted + "\n" + summary;
                }
                return summary + "\n" + formatted;
            }
        }
    }

    /**
     * Execute without timeout (backward compatible).
     */
    public String execute(Connection conn, String sql, OutputFormatter formatter, int maxRows)
            throws Exception {
        return execute(conn, sql, formatter, maxRows, 0);
    }
}
