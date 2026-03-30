package com.ecommerce.models;

public class Event {
    public enum Type {
        ORDER_CREATED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        INVENTORY_UPDATED,
        ORDER_CANCELLED,
        ORDER_RETURNED
    }

    private final Type type;
    private final String payload;

    public Event(Type type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public Type getType() { return type; }
    public String getPayload() { return payload; }

    @Override
    public String toString() {
        return String.format("EVENT[%s]: %s", type, payload);
    }
}