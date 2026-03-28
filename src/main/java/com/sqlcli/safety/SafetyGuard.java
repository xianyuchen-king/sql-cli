package com.sqlcli.safety;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.dialect.Dialect;
import com.sqlcli.dialect.DialectFactory;

public class SafetyGuard {

    private final SqlAnalyzer analyzer = new SqlAnalyzer();

    /**
     * Validate SQL against safety rules. Throws RuntimeException if blocked.
     * @return the (possibly modified) SQL to execute
     */
    public String validate(String sql, ConnectionConfig config, boolean confirm,
                           boolean noLimit, int userLimit, AppConfig appConfig) {
        // Check connection-level safety
        String effectiveLevel = config.getEffectiveSafetyLevel(
                appConfig.getDefaults().getSafetyLevel());

        if ("strict".equalsIgnoreCase(effectiveLevel)) {
            if (!analyzer.isSelect(sql)) {
                throw new RuntimeException("[BLOCKED] Connection '" + config.getName()
                        + "' is in strict mode. Only SELECT and metadata queries are allowed.");
            }
        }

        if ("none".equalsIgnoreCase(effectiveLevel)) {
            // No safety checks, but still apply row limit
            return applyRowLimit(sql, config, noLimit, userLimit, appConfig);
        }

        // Check SQL risk level
        SqlAnalyzer.RiskLevel risk = analyzer.analyze(sql);

        switch (risk) {
            case BLOCKED:
                throw new RuntimeException(analyzer.getMessage(risk, sql));
            case DANGEROUS:
                if (!confirm) {
                    throw new RuntimeException(analyzer.getMessage(risk, sql));
                }
                break;
            case WARNING:
                String msg = analyzer.getMessage(risk, sql);
                if (msg != null) System.err.println(msg);
                break;
            case SAFE:
                break;
        }

        return applyRowLimit(sql, config, noLimit, userLimit, appConfig);
    }

    /**
     * Apply row limit to SELECT queries.
     */
    private String applyRowLimit(String sql, ConnectionConfig config, boolean noLimit,
                                 int userLimit, AppConfig appConfig) {
        if (!analyzer.isSelect(sql)) return sql;

        // Determine effective limit
        int effectiveLimit;
        if (noLimit) {
            System.err.println("[WARN] Row limit disabled. Large table queries may cause memory issues.");
            return sql;
        } else if (userLimit > 0) {
            effectiveLimit = userLimit;
        } else {
            effectiveLimit = appConfig.getDefaults().getMaxRows();
        }

        // Get dialect
        Dialect dialect;
        if (config.isDirectUrl() || config.getType() == null) {
            dialect = new com.sqlcli.dialect.GenericDialect();
        } else {
            dialect = DialectFactory.getDialect(config.getType(), appConfig);
        }

        // Check if already has limit
        if (dialect.hasLimit(sql)) return sql;

        // Apply auto limit
        if (appConfig.getDefaults().isAutoLimit()) {
            String wrapped = dialect.wrapLimit(sql, effectiveLimit);
            if (wrapped != null) return wrapped;
            System.err.println("[WARN] Cannot auto-append row limit for this database type. "
                    + "Please add LIMIT clause manually.");
        }

        return sql;
    }
}
