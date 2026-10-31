package com.keyflow.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * The single error contract returned by every endpoint.
 * {@code code} is a stable, machine-readable identifier; {@code message} is for humans.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    public record FieldViolation(String field, String message) {
    }
}