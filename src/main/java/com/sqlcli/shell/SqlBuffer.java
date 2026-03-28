package com.sqlcli.shell;

/**
 * Buffer for collecting multi-line SQL statements.
 * SQL is executed when a line ends with semicolon (;).
 */
public class SqlBuffer {

    private final StringBuilder buffer = new StringBuilder();
    private boolean hasContent = false;

    /**
     * Append a line to the buffer.
     * @param line the line to append
     * @return true if the SQL is complete (ends with ;), false otherwise
     */
    public boolean append(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() && !hasContent) {
            return false;
        }

        if (hasContent) {
            buffer.append('\n');
        }
        buffer.append(line);
        hasContent = true;

        return trimmed.endsWith(";");
    }

    /**
     * Get the complete SQL statement.
     * @return the buffered SQL, or empty string if buffer is empty
     */
    public String getSql() {
        if (!hasContent) {
            return "";
        }
        return buffer.toString().trim();
    }

    /**
     * Clear the buffer.
     */
    public void clear() {
        buffer.setLength(0);
        hasContent = false;
    }

    /**
     * Check if buffer has content.
     */
    public boolean hasContent() {
        return hasContent;
    }

    /**
     * Get the current buffer content for display (e.g., prompt).
     */
    public String getPreview() {
        if (!hasContent) {
            return "";
        }
        String sql = buffer.toString().trim();
        if (sql.length() > 30) {
            return sql.substring(0, 27) + "...";
        }
        return sql;
    }
}
