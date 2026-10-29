package com.keyflow.common.exception;

/**
 * A well-formed request that conflicts with current state, such as a duplicate
 * or a disallowed status transition. Carries its own stable error code.
 */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}