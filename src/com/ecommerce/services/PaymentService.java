package com.ecommerce.services;

import java.util.Random;

/**
 * Task 6 : Payment Simulation (random success/failure)
 * Task 18: Failure Injection System
 */
public class PaymentService {
    private static final Random RANDOM = new Random();
    private boolean failureInjectionEnabled = false;
    private double failureProbability = 0.3; // 30% failure by default

    public void enableFailureInjection(double probability) {
        this.failureInjectionEnabled = true;
        this.failureProbability = probability;
        System.out.printf("  ⚡ Failure injection ENABLED (probability: %.0f%%)%n", probability * 100);
    }

    public void disableFailureInjection() {
        this.failureInjectionEnabled = false;
        System.out.println("  Failure injection DISABLED.");
    }

    public boolean isFailureInjectionEnabled() {
        return failureInjectionEnabled;
    }

    /**
     * Simulate payment processing.
     * Returns true if payment succeeds, false otherwise.
     */
    public boolean processPayment(String orderId, double amount) {
        System.out.printf("  Processing payment for order %s | Amount: ₹%.2f ...%n", orderId, amount);

        // Task 18: random failure injection
        if (failureInjectionEnabled && RANDOM.nextDouble() < failureProbability) {
            System.out.println("  ❌ Payment FAILED (simulated failure).");
            return false;
        }

        // Normal random failure (Task 6)
        if (!failureInjectionEnabled && RANDOM.nextDouble() < 0.2) {
            System.out.println("  ❌ Payment FAILED (random).");
            return false;
        }

        System.out.println("  ✅ Payment SUCCESSFUL.");
        return true;
    }
}