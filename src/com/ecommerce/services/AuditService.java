package com.ecommerce.services;

import com.ecommerce.models.AuditLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Task 16: Audit Logging System - immutable append-only logs
 */
public class AuditService {
    private final List<AuditLog> logs = new ArrayList<>();

    public synchronized void log(String message) {
        logs.add(new AuditLog(message));
    }

    public void viewLogs() {
        if (logs.isEmpty()) {
            System.out.println("  No audit logs.");
            return;
        }
        System.out.println("  ---- Audit Logs ----");
        logs.forEach(l -> System.out.println("  " + l));
    }

    public List<AuditLog> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}