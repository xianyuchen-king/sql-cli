package com.sqlcli.safety;

import com.sqlcli.config.AppConfig;
import com.sqlcli.config.ConnectionConfig;
import com.sqlcli.dialect.Dialect;
import com.sqlcli.dialect.DialectFactory;
import com.sqlcli.output.ErrorCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SafetyGuard {

    private final SqlAnalyzer analyzer = new SqlAnalyzer();
    private final List<String> warnings = new ArrayList<>();

    /**
     * Validate SQL against safety rules. Throws SafetyException if blocked.
     * Warnings are collected and can be retrieved via getWarnings().
     * @return the (possibly modified) SQL to execute
     */
    public String validate(String sql, ConnectionConfig config, boolean confirm,
                           boolean noLimit, int userLimit, AppConfig appConfig) {
        warnings.clear();

        // Check connection-level safety
        String effectiveLevel = config.getEffectiveSafetyLevel(
                appConfig.getDefaults().getSafetyLevel());

        if ("strict".equalsIgnoreCase(effectiveLevel)) {
            if (!analyzer.isSelect(sql)) {
                throw new SafetyException(ErrorCode.SAFETY_STRICT_MODE,
                        "[BLOCKED] Connection '" + config.getName()
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
                throw new SafetyException(ErrorCode.SAFETY_BLOCKED, analyzer.getMessage(risk, sql));
            case DANGEROUS:
                if (!confirm) {
                    throw new SafetyException(ErrorCode.SAFETY_DANGEROUS, analyzer.getMessage(risk, sql));
                }
                break;
            case WARNING:
                String msg = analyzer.getMessage(risk, sql);
                if (msg != null) {
                    warnings.add(msg.replaceFirst("^\\[WARN\\]\\s*", ""));
                }
                break;
            case SAFE:
                break;
        }

        return applyRowLimit(sql, config, noLimit, userLimit, appConfig);
    }

    /**
     * Get warnings collected during the last validate() call.
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
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
            warnings.add("Row limit disabled. Large table queries may cause memory issues.");
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
            // Dialect cannot wrap limit — collect as warning instead of printing to stderr
            warnings.add("Cannot auto-append row limit for " + config.getType() + " type");
        }

        return sql;
    }
}
