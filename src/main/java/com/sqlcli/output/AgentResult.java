package com.sqlcli.output;

import java.util.Map;

/**
 * Structured result wrapper for non-tabular command output (conn list, test, exec, etc.)
 * Produces valid JSON for Agent consumption.
 */
public class AgentResult {

    private final String status;
    private final String errorCode;
    private final String message;
    private final Object data;

    private AgentResult(String status, String errorCode, String message, Object data) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    public static AgentResult ok(String message, Object data) {
        return new AgentResult("ok", null, message, data);
    }

    public static AgentResult ok(Object data) {
        return new AgentResult("ok", null, null, data);
    }

    public static AgentResult ok(String message) {
        return new AgentResult("ok", null, message, null);
    }

    public static AgentResult error(ErrorCode code, String message) {
        return new AgentResult("error", code.name(), message, null);
    }

    public static AgentResult error(String message) {
        return new AgentResult("error", ErrorCode.UNKNOWN.name(), message, null);
    }

    /**
     * Serialize to JSON string.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"").append(escape(status)).append("\"");
        if (errorCode != null) {
            sb.append(",\n  \"error_code\": \"").append(escape(errorCode)).append("\"");
        }
        if (message != null) {
            sb.append(",\n  \"message\": \"").append(escape(message)).append("\"");
        }
        if (data != null) {
            sb.append(",\n  \"data\": ");
            if (data instanceof String) {
                sb.append("\"").append(escape((String) data)).append("\"");
            } else if (data instanceof Number) {
                sb.append(data);
            } else if (data instanceof Boolean) {
                sb.append(data);
            } else if (data instanceof Map) {
                sb.append(mapToJson((Map<?, ?>) data));
            } else if (data instanceof Iterable) {
                sb.append(iterableToJson((Iterable<?>) data));
            } else {
                sb.append("\"").append(escape(data.toString())).append("\"");
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * Build a plain text representation.
     */
    public String toPlainText() {
        if ("error".equals(status)) {
            return "[ERROR] " + (message != null ? message : "Unknown error");
        }
        if (message != null) {
            return message;
        }
        return status;
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\n    \"").append(escape(String.valueOf(entry.getKey()))).append("\": ");
            Object val = entry.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val instanceof Number) {
                sb.append(val);
            } else if (val instanceof Boolean) {
                sb.append(val);
            } else if (val instanceof Map) {
                sb.append(mapToJson((Map<?, ?>) val));
            } else if (val instanceof Iterable) {
                sb.append(iterableToJson((Iterable<?>) val));
            } else {
                sb.append("\"").append(escape(val.toString())).append("\"");
            }
        }
        if (!map.isEmpty()) sb.append("\n  ");
        sb.append("}");
        return sb.toString();
    }

    private String iterableToJson(Iterable<?> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Object item : items) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\n    ");
            if (item instanceof Map) {
                sb.append(mapToJson((Map<?, ?>) item));
            } else if (item instanceof String) {
                sb.append("\"").append(escape((String) item)).append("\"");
            } else if (item instanceof Number || item instanceof Boolean) {
                sb.append(item);
            } else if (item != null) {
                sb.append("\"").append(escape(item.toString())).append("\"");
            } else {
                sb.append("null");
            }
        }
        if (!first) sb.append("\n  ");
        sb.append("]");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
