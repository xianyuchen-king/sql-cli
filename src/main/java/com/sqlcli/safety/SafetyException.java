package com.sqlcli.safety;

import com.sqlcli.output.ErrorCode;

public class SafetyException extends RuntimeException {

    private final ErrorCode errorCode;

    public SafetyException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
