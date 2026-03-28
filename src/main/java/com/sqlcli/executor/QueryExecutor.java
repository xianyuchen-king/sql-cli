package com.sqlcli.executor;

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
    public String execute(Connection conn, String sql, OutputFormatter formatter, int maxRows)
            throws Exception {
        try (Statement stmt = conn.createStatement()) {
            if (maxRows > 0) {
                stmt.setMaxRows(maxRows);
            }
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

                return formatter.formatQuery(columns, rows);
            }
        }
    }
}
