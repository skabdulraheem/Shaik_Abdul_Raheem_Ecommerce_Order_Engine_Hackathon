package com.ecommerce.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLog {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final LocalDateTime timestamp;
    private final String message;

    public AuditLog(String message) {
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "[" + timestamp.format(FMT) + "] " + message;
    }
}