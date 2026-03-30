package com.ecommerce.models;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    DELIVERED,
    FAILED,
    CANCELLED
}