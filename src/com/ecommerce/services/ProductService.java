package com.ecommerce.services;

import com.ecommerce.models.Product;

import java.util.*;
import java.util.concurrent.*;

/**
 * Task 1  : Product Management
 * Task 10 : Inventory Alert System
 * Task 15 : Inventory Reservation Expiry
 */
public class ProductService {
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final AuditService auditService;

    // Task 15: reservation expiry tracking
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int LOW_STOCK_THRESHOLD = 3;

    public ProductService(AuditService auditService) {
        this.auditService = auditService;
    }

    /** Add a new product; prevents duplicate IDs */
    public boolean addProduct(String productId, String name, double price, int stock) {
        if (products.containsKey(productId)) {
            System.out.println("  ERROR: Product ID '" + productId + "' already exists.");
            return false;
        }
        if (stock < 0) {
            System.out.println("  ERROR: Stock cannot be negative.");
            return false;
        }
        products.put(productId, new Product(productId, name, price, stock));
        auditService.log("Product added: " + productId + " '" + name + "' stock=" + stock);
        System.out.println("  Product added successfully.");
        return true;
    }

    /** Update stock of an existing product */
    public boolean updateStock(String productId, int newStock) {
        Product p = products.get(productId);
        if (p == null) {
            System.out.println("  ERROR: Product not found.");
            return false;
        }
        if (newStock < 0) {
            System.out.println("  ERROR: Stock cannot be negative.");
            return false;
        }
        p.setStock(newStock);
        auditService.log("Stock updated: " + productId + " -> " + newStock);
        return true;
    }

    public void viewAllProducts() {
        if (products.isEmpty()) {
            System.out.println("  No products available.");
            return;
        }
        System.out.println("  ---- Product Catalog ----");
        products.values().forEach(p -> System.out.println("  " + p));
    }

    /** Task 10: show products with low stock */
    public void showLowStockAlert() {
        System.out.println("  ---- Low Stock Alert (threshold: " + LOW_STOCK_THRESHOLD + ") ----");
        boolean found = false;
        for (Product p : products.values()) {
            if (p.getAvailableStock() <= LOW_STOCK_THRESHOLD) {
                System.out.printf("  ⚠️  %s | Available: %d%n", p.getName(), p.getAvailableStock());
                found = true;
            }
        }
        if (!found) System.out.println("  All products are well-stocked.");
    }

    public Product getProduct(String productId) {
        return products.get(productId);
    }

    public Collection<Product> getAllProducts() {
        return products.values();
    }

    /**
     * Task 15: Schedule auto-release of reserved stock after timeout.
     * Called when stock is reserved for a cart item.
     */
    public void scheduleReservationExpiry(String userId, String productId, int quantity, long delaySeconds) {
        scheduler.schedule(() -> {
            Product p = products.get(productId);
            if (p != null && p.getReservedStock() > 0) {
                p.releaseReservation(quantity);
                auditService.log("RESERVATION EXPIRED: user=" + userId + " product=" + productId
                        + " qty=" + quantity + " auto-released after " + delaySeconds + "s");
                System.out.println("\n  [EXPIRY] Reservation expired for " + productId
                        + " (qty=" + quantity + ", user=" + userId + ")");
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}