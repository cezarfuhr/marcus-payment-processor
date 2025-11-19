package com.openfinance.payment.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique payment IDs in the format: PAY-YYYY-NNNNNN
 * Example: PAY-2025-000001
 */
public class PaymentIdGenerator {

    private static final AtomicLong counter = new AtomicLong(1);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

    private PaymentIdGenerator() {
        // Utility class
    }

    public static String generate() {
        String year = LocalDateTime.now().format(YEAR_FORMATTER);
        long sequence = counter.getAndIncrement();
        return String.format("PAY-%s-%06d", year, sequence);
    }

    public static void reset() {
        counter.set(1);
    }
}
