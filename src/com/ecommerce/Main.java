package com.ecommerce;

import com.ecommerce.cli.InputHelper;
import com.ecommerce.engine.ServiceRegistry;
import com.ecommerce.models.Order;
import com.ecommerce.models.OrderStatus;

import java.util.Scanner;

/**
 * Main CLI entry point.
 * Menu:
 *  1. Add Product
 *  2. View Products
 *  3. Add to Cart
 *  4. Remove from Cart
 *  5. View Cart
 *  6. Apply Coupon
 *  7. Place Order
 *  8. Cancel Order
 *  9. View Orders
 * 10. Low Stock Alert
 * 11. Return Product
 * 12. Simulate Concurrent Users
 * 13. View Logs
 * 14. Trigger Failure Mode
 *  0. Exit
 */
public class Main {

    private static final ServiceRegistry registry = new ServiceRegistry();
    private static String currentUser = "USER_1"; // default active user

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        InputHelper input = new InputHelper(sc);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Distributed E-Commerce Order Engine    ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // Seed some default data so the system is ready to demo
        seedData();

        boolean running = true;
        while (running) {
            printMenu();
            System.out.printf("  [Active User: %s]%n", currentUser);
            int choice = input.readInt("  Enter choice: ");
            System.out.println();

            switch (choice) {
                case 1  -> handleAddProduct(input);
                case 2  -> registry.product().viewAllProducts();
                case 3  -> handleAddToCart(input);
                case 4  -> handleRemoveFromCart(input);
                case 5  -> handleViewCart(input);
                case 6  -> handleApplyCoupon(input);
                case 7  -> handlePlaceOrder(input);
                case 8  -> handleCancelOrder(input);
                case 9  -> handleViewOrders(input);
                case 10 -> registry.product().showLowStockAlert();
                case 11 -> handleReturnProduct(input);
                case 12 -> handleConcurrentSimulation(input);
                case 13 -> registry.audit().viewLogs();
                case 14 -> handleFailureMode(input);
                case 15 -> handleSwitchUser(input);
                case 0  -> {
                    registry.shutdown();
                    System.out.println("\n  Goodbye! 👋");
                    running = false;
                }
                default -> System.out.println("  Invalid option. Try again.");
            }
            System.out.println();
        }
        sc.close();
    }

    // ─────────────────────────────────────────────
    // Menu Handlers
    // ─────────────────────────────────────────────

    private static void handleAddProduct(InputHelper input) {
        System.out.println("  ── Add Product ──");
        String id    = input.readLine("  Product ID   : ");
        String name  = input.readLine("  Product Name : ");
        double price = input.readDouble("  Price (₹)    : ");
        int stock    = input.readInt("  Stock qty    : ");
        registry.product().addProduct(id, name, price, stock);
    }

    private static void handleAddToCart(InputHelper input) {
        System.out.println("  ── Add to Cart ──");
        String userId = input.readLine("  User ID     [" + currentUser + "]: ");
        if (userId.isEmpty()) userId = currentUser;
        String pid = input.readLine("  Product ID  : ");
        int qty    = input.readInt("  Quantity    : ");
        registry.cart().addToCart(userId, pid, qty);
    }

    private static void handleRemoveFromCart(InputHelper input) {
        System.out.println("  ── Remove from Cart ──");
        String userId = input.readLine("  User ID    [" + currentUser + "]: ");
        if (userId.isEmpty()) userId = currentUser;
        String pid = input.readLine("  Product ID : ");
        registry.cart().removeFromCart(userId, pid);
    }

    private static void handleViewCart(InputHelper input) {
        System.out.println("  ── View Cart ──");
        String userId = input.readLine("  User ID [" + currentUser + "]: ");
        if (userId.isEmpty()) userId = currentUser;
        registry.cart().viewCart(userId);
    }

    private static void handleApplyCoupon(InputHelper input) {
        System.out.println("  ── Apply Coupon ──");
        System.out.println("  Available coupons: SAVE10 (10% off) | FLAT200 (₹200 flat off)");
        String userId = input.readLine("  User ID     [" + currentUser + "]: ");
        if (userId.isEmpty()) userId = currentUser;
        var cart = registry.cart().getCart(userId);
        if (cart == null || cart.isEmpty()) {
            System.out.println("  Cart is empty.");
            return;
        }
        String coupon = input.readLine("  Coupon Code : ");
        registry.discount().applyCoupon(cart, coupon);
    }

    private static void handlePlaceOrder(InputHelper input) {
        System.out.println("  ── Place Order ──");
        String userId = input.readLine("  User ID         [" + currentUser + "]: ");
        if (userId.isEmpty()) userId = currentUser;
        String idem = input.readLine("  Idempotency key (press Enter to auto): ");
        if (idem.isEmpty()) idem = userId + "_" + System.currentTimeMillis();
        Order order = registry.order().placeOrder(userId, idem);
        if (order != null) {
            System.out.println("\n  ── Order Summary ──");
            System.out.println(order);
        }
    }

    private static void handleCancelOrder(InputHelper input) {
        System.out.println("  ── Cancel Order ──");
        String orderId = input.readLine("  Order ID: ");
        registry.order().cancelOrder(orderId);
    }

    private static void handleViewOrders(InputHelper input) {
        System.out.println("  ── View Orders ──");
        System.out.println("  1) All orders");
        System.out.println("  2) Search by Order ID");
        System.out.println("  3) Filter by status");
        int sub = input.readInt("  Choice: ");
        switch (sub) {
            case 1 -> registry.order().viewAllOrders();
            case 2 -> {
                String id = input.readLine("  Order ID: ");
                registry.order().searchOrder(id);
            }
            case 3 -> {
                System.out.println("  Statuses: CREATED, PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, FAILED, CANCELLED");
                String s = input.readLine("  Status: ").toUpperCase();
                try {
                    registry.order().filterOrders(OrderStatus.valueOf(s));
                } catch (IllegalArgumentException e) {
                    System.out.println("  Invalid status.");
                }
            }
            default -> System.out.println("  Invalid choice.");
        }
    }

    private static void handleReturnProduct(InputHelper input) {
        System.out.println("  ── Return Product ──");
        String orderId = input.readLine("  Order ID   : ");
        String pid     = input.readLine("  Product ID : ");
        int qty        = input.readInt("  Quantity   : ");
        registry.order().returnProduct(orderId, pid, qty);
    }

    private static void handleConcurrentSimulation(InputHelper input) {
        System.out.println("  ── Simulate Concurrent Users ──");
        String pid  = input.readLine("  Product ID     : ");
        int users   = input.readInt("  Number of users: ");
        int qtyEach = input.readInt("  Qty per user   : ");
        registry.concurrency().simulateConcurrentAccess(pid, users, qtyEach);
    }

    private static void handleFailureMode(InputHelper input) {
        System.out.println("  ── Failure Injection ──");
        System.out.println("  1) Enable failure injection");
        System.out.println("  2) Disable failure injection");
        System.out.println("  3) View fraud-flagged users");
        int sub = input.readInt("  Choice: ");
        switch (sub) {
            case 1 -> {
                double prob = input.readDouble("  Failure probability (0.0 - 1.0): ");
                registry.payment().enableFailureInjection(prob);
            }
            case 2 -> registry.payment().disableFailureInjection();
            case 3 -> registry.fraud().viewFlaggedUsers();
            default -> System.out.println("  Invalid choice.");
        }
    }

    private static void handleSwitchUser(InputHelper input) {
        String u = input.readLine("  Enter User ID to switch to: ");
        if (!u.isEmpty()) {
            currentUser = u;
            System.out.println("  Switched to user: " + currentUser);
        }
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    private static void printMenu() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│            MAIN MENU                    │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1.  Add Product                        │");
        System.out.println("│  2.  View Products                      │");
        System.out.println("│  3.  Add to Cart                        │");
        System.out.println("│  4.  Remove from Cart                   │");
        System.out.println("│  5.  View Cart                          │");
        System.out.println("│  6.  Apply Coupon                       │");
        System.out.println("│  7.  Place Order                        │");
        System.out.println("│  8.  Cancel Order                       │");
        System.out.println("│  9.  View Orders                        │");
        System.out.println("│ 10.  Low Stock Alert                    │");
        System.out.println("│ 11.  Return Product                     │");
        System.out.println("│ 12.  Simulate Concurrent Users          │");
        System.out.println("│ 13.  View Logs                          │");
        System.out.println("│ 14.  Trigger Failure Mode               │");
        System.out.println("│ 15.  Switch User                        │");
        System.out.println("│  0.  Exit                               │");
        System.out.println("└─────────────────────────────────────────┘");
    }

    // ─────────────────────────────────────────────
    // Seed Data
    // ─────────────────────────────────────────────

    private static void seedData() {
        System.out.println("\n  [Seeding demo data...]");
        registry.product().addProduct("P001", "iPhone 15",       79999, 10);
        registry.product().addProduct("P002", "Samsung Galaxy",  59999, 5);
        registry.product().addProduct("P003", "Boat Earphones",  1999,  2);
        registry.product().addProduct("P004", "USB-C Cable",     499,   50);
        registry.product().addProduct("P005", "Laptop Stand",    1299,  8);
        System.out.println("  [Seed complete — 5 products loaded]\n");
    }
}