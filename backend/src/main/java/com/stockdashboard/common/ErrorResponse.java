package com.stockdashboard.common;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(String message, List<String> details, Instant timestamp) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, List.of(), Instant.now());
    }

    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse(message, details, Instant.now());
    }
}
