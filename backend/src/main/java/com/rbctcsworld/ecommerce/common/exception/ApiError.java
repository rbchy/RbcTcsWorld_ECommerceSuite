package com.rbctcsworld.ecommerce.common.exception;

import java.time.Instant;
import java.util.Map;

/**
 * One JSON shape for every error response, so automation can assert on
 * status, error and message consistently.
 */
public record ApiError(String timestamp, int status, String error, String message, String path,
                       Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now().toString(), status, error, message, path, Map.of());
    }

    public static ApiError of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now().toString(), status, error, message, path, fieldErrors);
    }
}
