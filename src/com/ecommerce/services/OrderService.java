package com.ecommerce.services;

import com.ecommerce.engine.OrderStateMachine;
import com.ecommerce.models.*;
import com.ecommerce.utils.IdGenerator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Task 5  : Order Placement Engine (atomic)
 * Task 6  : Payment Simulation (via PaymentService)
 * Task 7  : Transaction Rollback System
 * Task 11 : Order Management (view/search/filter)
 * Task 12 : Order Cancellation Engine
 * Task 13 : Return & Refund System
 * Task 19 : Idempotency Handling
 */
public class OrderService {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Set<String> idempotencyKeys = Collections.synchronizedSet(new HashSet<>());

    private final ProductService productService;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final AuditService auditService;
    private final EventService eventService;
    private final FraudDetectionService fraudService;
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    public OrderService(ProductService productService, CartService cartService,
                        PaymentService paymentService, AuditService auditService,
                        EventService eventService, FraudDetectionService fraudService) {
        this.productService = productService;
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.auditService = auditService;
        this.eventService = eventService;
        this.fraudService = fraudService;
    }

    /**
     * Task 5: Atomic order placement.
     * Steps: validate cart → calculate total → lock (deduct) stock → create order → payment → clear cart
     * Task 19: idempotency key prevents duplicate orders.
     */
    public Order placeOrder(String userId, String idempotencyKey) {
        // Task 19: Idempotency check
        if (idempotencyKey != null && idempotencyKeys.contains(idempotencyKey)) {
            System.out.println("  ⚠️  Duplicate order request detected (idempotency key already used).");
            return null;
        }

        Cart cart = cartService.getCart(userId);
        if (cart == null || cart.isEmpty()) {
            System.out.println("  ERROR: Cart is empty. Cannot place order.");
            return null;
        }

        // Build order items from cart
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            Product p = productService.getProduct(ci.getProductId());
            if (p == null) {
                System.out.println("  ERROR: Product " + ci.getProductId() + " not found.");
                return null;
            }
            orderItems.add(new OrderItem(ci.getProductId(), p.getName(),
                    ci.getQuantity(), ci.getPriceAtAdd()));
        }

        double gross = cart.getGrossTotal();
        double discount = cart.getDiscountAmount();

        String orderId = IdGenerator.nextOrderId();

        // Task 7 rollback tracking
        List<Runnable> rollbackActions = new ArrayList<>();

        // Step: Deduct stock permanently (reservation → actual deduction)
        for (CartItem ci : cart.getItems()) {
            Product p = productService.getProduct(ci.getProductId());
            p.confirmDeduction(ci.getQuantity());
            final String pid = ci.getProductId();
            final int qty = ci.getQuantity();
            rollbackActions.add(() -> {
                p.restoreStock(qty);
                auditService.log("ROLLBACK: restored " + qty + " units of " + pid);
            });
        }

        // Step: Create order
        Order order = new Order(orderId, userId, orderItems, gross, discount);
        if (idempotencyKey != null) {
            order.setIdempotencyKey(idempotencyKey);
            idempotencyKeys.add(idempotencyKey);
        }
        orders.put(orderId, order);
        rollbackActions.add(() -> {
            orders.remove(orderId);
            auditService.log("ROLLBACK: order " + orderId + " deleted");
        });

        stateMachine.transition(order, OrderStatus.PENDING_PAYMENT);
        auditService.log("ORDER CREATED: " + orderId + " user=" + userId + " total=₹" + order.getFinalTotal());
        eventService.publish(Event.Type.ORDER_CREATED, orderId);

        // Task 6: Payment
        boolean paymentSuccess = paymentService.processPayment(orderId, order.getFinalTotal());

        if (!paymentSuccess) {
            // Task 7: Rollback
            System.out.println("  ↩️  Rolling back order " + orderId + "...");
            Collections.reverse(rollbackActions);
            rollbackActions.forEach(Runnable::run);
            stateMachine.transition(order, OrderStatus.FAILED);
            cart.clear(); // clear cart even on failure (stock already rolled back)
            eventService.publish(Event.Type.PAYMENT_FAILED, orderId);
            auditService.log("ORDER FAILED + ROLLED BACK: " + orderId);
            return null;
        }

        stateMachine.transition(order, OrderStatus.PAID);
        cartService.clearCart(userId); // does nothing since stock already confirmed
        cart.clear();

        eventService.publish(Event.Type.PAYMENT_SUCCESS, orderId);
        eventService.publish(Event.Type.INVENTORY_UPDATED, "order=" + orderId);

        // Task 17: Fraud check
        fraudService.checkOrder(userId, order.getFinalTotal());

        System.out.println("  ✅ Order placed successfully! Order ID: " + orderId);
        return order;
    }

    /**
     * Task 12: Cancel an order
     */
    public boolean cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            System.out.println("  ERROR: Order not found.");
            return false;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            System.out.println("  ERROR: Order is already cancelled.");
            return false;
        }
        if (!stateMachine.transition(order, OrderStatus.CANCELLED)) {
            return false;
        }
        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product p = productService.getProduct(item.getProductId());
            if (p != null) {
                int effectiveQty = item.getQuantity() - item.getReturnedQuantity();
                p.restoreStock(effectiveQty);
            }
        }
        eventService.publish(Event.Type.ORDER_CANCELLED, orderId);
        auditService.log("ORDER CANCELLED: " + orderId + " stock restored");
        System.out.println("  Order cancelled and stock restored.");
        return true;
    }

    /**
     * Task 13: Partial return & refund
     */
    public boolean returnProduct(String orderId, String productId, int qty) {
        Order order = orders.get(orderId);
        if (order == null) {
            System.out.println("  ERROR: Order not found.");
            return false;
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.PAID) {
            System.out.println("  ERROR: Can only return items from PAID or DELIVERED orders.");
            return false;
        }
        OrderItem target = order.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst().orElse(null);
        if (target == null) {
            System.out.println("  ERROR: Product not found in order.");
            return false;
        }
        if (!target.returnQty(qty)) {
            System.out.printf("  ERROR: Cannot return %d units. Returnable: %d%n",
                    qty, target.getQuantity() - target.getReturnedQuantity());
            return false;
        }
        // Restore stock
        Product p = productService.getProduct(productId);
        if (p != null) p.restoreStock(qty);

        // Update order total
        double refund = qty * target.getUnitPrice();
        order.setFinalTotal(order.getFinalTotal() - refund);

        auditService.log("RETURN: order=" + orderId + " product=" + productId
                + " qty=" + qty + " refund=₹" + refund);
        eventService.publish(Event.Type.ORDER_RETURNED, "order=" + orderId + " product=" + productId);
        System.out.printf("  Return processed. Refund: ₹%.2f%n", refund);
        return true;
    }

    /** Task 11: view all orders */
    public void viewAllOrders() {
        if (orders.isEmpty()) {
            System.out.println("  No orders found.");
            return;
        }
        orders.values().forEach(o -> System.out.println(o + "---"));
    }

    /** Task 11: search by order ID */
    public void searchOrder(String orderId) {
        Order o = orders.get(orderId);
        if (o == null) System.out.println("  Order not found.");
        else System.out.println(o);
    }

    /** Task 11: filter by status */
    public void filterOrders(OrderStatus status) {
        List<Order> filtered = orders.values().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            System.out.println("  No orders with status: " + status);
        } else {
            filtered.forEach(o -> System.out.println(o + "---"));
        }
    }

    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }

    public Map<String, Order> getAllOrders() {
        return Collections.unmodifiableMap(orders);
    }

    public OrderStateMachine getStateMachine() {
        return stateMachine;
    }
}