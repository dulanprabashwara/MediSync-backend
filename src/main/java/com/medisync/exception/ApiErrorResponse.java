package com.medisync.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(OffsetDateTime.now(ZoneOffset.UTC), status, error, message, path, Map.of());
    }
}
