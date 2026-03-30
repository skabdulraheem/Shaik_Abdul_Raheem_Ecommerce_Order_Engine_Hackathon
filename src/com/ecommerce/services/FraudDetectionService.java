package com.ecommerce.services;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Task 17: Fraud Detection System
 * - 3 orders in 1 minute → flag user
 * - High-value orders → suspicious
 */
public class FraudDetectionService {
    private static final int ORDER_COUNT_LIMIT = 3;
    private static final long TIME_WINDOW_SECONDS = 60;
    private static final double HIGH_VALUE_THRESHOLD = 10000.0;

    // userId -> list of order timestamps
    private final Map<String, List<LocalDateTime>> userOrderTimestamps = new HashMap<>();
    private final Set<String> flaggedUsers = new HashSet<>();
    private final AuditService auditService;

    public FraudDetectionService(AuditService auditService) {
        this.auditService = auditService;
    }

    public boolean checkOrder(String userId, double orderAmount) {
        boolean flagged = false;

        // Rule 1: too many orders in time window
        userOrderTimestamps.putIfAbsent(userId, new ArrayList<>());
        List<LocalDateTime> timestamps = userOrderTimestamps.get(userId);
        LocalDateTime now = LocalDateTime.now();
        // remove old timestamps outside window
        timestamps.removeIf(t -> t.isBefore(now.minusSeconds(TIME_WINDOW_SECONDS)));
        timestamps.add(now);

        if (timestamps.size() >= ORDER_COUNT_LIMIT) {
            flaggedUsers.add(userId);
            String msg = "FRAUD: " + userId + " placed " + timestamps.size()
                    + " orders in " + TIME_WINDOW_SECONDS + "s";
            System.out.println("  ⚠️  " + msg);
            auditService.log(msg);
            flagged = true;
        }

        // Rule 2: high-value order
        if (orderAmount > HIGH_VALUE_THRESHOLD) {
            flaggedUsers.add(userId);
            String msg = "FRAUD: " + userId + " placed high-value order ₹" + orderAmount;
            System.out.println("  ⚠️  " + msg);
            auditService.log(msg);
            flagged = true;
        }

        return flagged;
    }

    public boolean isUserFlagged(String userId) {
        return flaggedUsers.contains(userId);
    }

    public void viewFlaggedUsers() {
        if (flaggedUsers.isEmpty()) {
            System.out.println("  No flagged users.");
        } else {
            System.out.println("  Flagged users: " + flaggedUsers);
        }
    }
}