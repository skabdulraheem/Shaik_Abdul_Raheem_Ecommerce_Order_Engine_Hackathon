package com.ecommerce.engine;

import com.ecommerce.models.Product;
import com.ecommerce.services.AuditService;
import com.ecommerce.services.ProductService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task 4: Concurrency Simulation
 * Simulates multiple users trying to access the same product simultaneously.
 * Uses logical locking via ReentrantLock inside Product.
 */
public class ConcurrencyEngine {

    private final ProductService productService;
    private final AuditService auditService;

    public ConcurrencyEngine(ProductService productService, AuditService auditService) {
        this.productService = productService;
        this.auditService = auditService;
    }

    /**
     * Simulate N users each trying to reserve `qtyPerUser` units of a product concurrently.
     * Only as many users as stock allows should succeed.
     */
    public void simulateConcurrentAccess(String productId, int numUsers, int qtyPerUser) {
        Product product = productService.getProduct(productId);
        if (product == null) {
            System.out.println("  ERROR: Product not found.");
            return;
        }

        System.out.printf("%n  ---- Concurrency Simulation ----%n");
        System.out.printf("  Product  : %s | Stock: %d%n", product.getName(), product.getAvailableStock());
        System.out.printf("  Users    : %d | Qty each: %d%n", numUsers, qtyPerUser);
        System.out.printf("  Expected max success: %d user(s)%n%n",
                product.getAvailableStock() / qtyPerUser);

        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch startLatch = new CountDownLatch(1); // all threads start at same time
        CountDownLatch doneLatch = new CountDownLatch(numUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> results = new CopyOnWriteArrayList<>();

        for (int i = 1; i <= numUsers; i++) {
            final String userId = "USER_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for gun
                    boolean reserved = product.reserveStock(qtyPerUser);
                    if (reserved) {
                        successCount.incrementAndGet();
                        String msg = userId + " ✅ SUCCESS - reserved " + qtyPerUser + " units";
                        results.add(msg);
                        auditService.log("CONCURRENT: " + msg);
                    } else {
                        failCount.incrementAndGet();
                        String msg = userId + " ❌ FAILED  - insufficient stock";
                        results.add(msg);
                        auditService.log("CONCURRENT: " + msg);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // fire!

        try {
            doneLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();

        // Print results
        results.forEach(r -> System.out.println("  " + r));
        System.out.printf("%n  Result → %d succeeded, %d failed%n", successCount.get(), failCount.get());
        System.out.printf("  Remaining available stock: %d%n", product.getAvailableStock());

        auditService.log("CONCURRENT SIM DONE: product=" + productId
                + " success=" + successCount.get() + " failed=" + failCount.get());
    }
}