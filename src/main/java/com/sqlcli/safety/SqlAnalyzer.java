package com.sqlcli.safety;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlAnalyzer {

    public enum RiskLevel {
        BLOCKED,     // Must not execute
        DANGEROUS,   // Requires --confirm
        WARNING,     // Warn but allow
        SAFE         // Execute normally
    }

    private static final Pattern STATEMENT_TYPE = Pattern.compile(
            "^\\s*(SELECT|INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|RENAME|GRANT|REVOKE|EXEC|CALL)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DROP_DATABASE = Pattern.compile(
            "^\\s*DROP\\s+(DATABASE|SCHEMA)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern DROP_TABLE = Pattern.compile(
            "^\\s*DROP\\s+TABLE\\b",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern TRUNCATE = Pattern.compile(
            "^\\s*TRUNCATE\\b",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern ALTER_DROP = Pattern.compile(
            "ALTER\\s+TABLE\\s+\\S+\\s+DROP\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WHERE_CLAUSE = Pattern.compile(
            "\\bWHERE\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LIMIT_CLAUSE = Pattern.compile(
            "\\b(LIMIT\\s+\\d+|FETCH\\s+FIRST\\s+\\d+\\s+ROWS\\s+ONLY|ROWNUM\\s*[<>=])",
            Pattern.CASE_INSENSITIVE);

    /**
     * Analyze SQL and determine risk level.
     */
    public RiskLevel analyze(String sql) {
        if (sql == null || sql.isBlank()) return RiskLevel.SAFE;

        String trimmed = sql.trim().toUpperCase();

        // DROP DATABASE / SCHEMA - always blocked
        if (DROP_DATABASE.matcher(sql).find()) {
            return RiskLevel.BLOCKED;
        }

        // DROP TABLE
        if (DROP_TABLE.matcher(sql).find()) {
            return RiskLevel.DANGEROUS;
        }

        // TRUNCATE
        if (TRUNCATE.matcher(sql).find()) {
            return RiskLevel.DANGEROUS;
        }

        // ALTER TABLE ... DROP
        if (ALTER_DROP.matcher(sql).find()) {
            return RiskLevel.DANGEROUS;
        }

        // DELETE / UPDATE without WHERE
        if (isDeleteOrUpdate(sql) && !WHERE_CLAUSE.matcher(sql).find()) {
            return RiskLevel.BLOCKED;
        }

        // ALTER TABLE (non-DROP)
        if (trimmed.startsWith("ALTER")) {
            return RiskLevel.WARNING;
        }

        // SELECT is always safe
        if (trimmed.startsWith("SELECT")) {
            return RiskLevel.SAFE;
        }

        // CREATE statements are safe
        if (trimmed.startsWith("CREATE") || trimmed.startsWith("RENAME")) {
            return RiskLevel.SAFE;
        }

        // INSERT is normally safe (warning only for large batches)
        if (trimmed.startsWith("INSERT")) {
            return RiskLevel.SAFE;
        }

        return RiskLevel.SAFE;
    }

    /**
     * Check if SQL is a SELECT statement.
     */
    public boolean isSelect(String sql) {
        return sql != null && sql.trim().toUpperCase().startsWith("SELECT");
    }

    /**
     * Get a human-readable message for the risk level.
     */
    public String getMessage(RiskLevel level, String sql) {
        return switch (level) {
            case BLOCKED -> "[BLOCKED] Operation is not allowed: " + getStatementSummary(sql);
            case DANGEROUS -> "[DANGEROUS] This operation requires --confirm: " + getStatementSummary(sql);
            case WARNING -> "[WARN] " + getStatementSummary(sql);
            case SAFE -> null;
        };
    }

    private boolean isDeleteOrUpdate(String sql) {
        Matcher m = STATEMENT_TYPE.matcher(sql);
        if (m.find()) {
            String type = m.group(1).toUpperCase();
            return type.equals("DELETE") || type.equals("UPDATE");
        }
        return false;
    }

    private String getStatementSummary(String sql) {
        String trimmed = sql.trim();
        if (trimmed.length() > 80) {
            return trimmed.substring(0, 77) + "...";
        }
        return trimmed;
    }
}
