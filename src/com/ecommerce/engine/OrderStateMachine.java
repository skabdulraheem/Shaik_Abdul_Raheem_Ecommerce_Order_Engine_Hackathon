package com.ecommerce.engine;

import com.ecommerce.models.Order;
import com.ecommerce.models.OrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Task 8: Order State Machine
 * Valid transitions:
 * CREATED → PENDING_PAYMENT → PAID → SHIPPED → DELIVERED
 *                           ↘ FAILED
 *                                          ↘ CANCELLED
 */
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.CREATED,
                EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PENDING_PAYMENT,
                EnumSet.of(OrderStatus.PAID, OrderStatus.FAILED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PAID,
                EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.SHIPPED,
                EnumSet.of(OrderStatus.DELIVERED));
        TRANSITIONS.put(OrderStatus.DELIVERED,
                EnumSet.noneOf(OrderStatus.class)); // terminal
        TRANSITIONS.put(OrderStatus.FAILED,
                EnumSet.noneOf(OrderStatus.class)); // terminal
        TRANSITIONS.put(OrderStatus.CANCELLED,
                EnumSet.noneOf(OrderStatus.class)); // terminal
    }

    public boolean transition(Order order, OrderStatus newStatus) {
        OrderStatus current = order.getStatus();
        Set<OrderStatus> allowed = TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(newStatus)) {
            System.out.printf("  ❌ Invalid transition: %s → %s%n", current, newStatus);
            return false;
        }
        order.setStatus(newStatus);
        System.out.printf("  Order %s: %s → %s%n", order.getOrderId(), current, newStatus);
        return true;
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to);
    }
}