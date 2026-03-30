package com.ecommerce.utils;

import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
    private static final AtomicLong ORDER_SEQ = new AtomicLong(1000);

    public static String nextOrderId() {
        return "ORD-" + ORDER_SEQ.incrementAndGet();
    }

    private IdGenerator() {}
}