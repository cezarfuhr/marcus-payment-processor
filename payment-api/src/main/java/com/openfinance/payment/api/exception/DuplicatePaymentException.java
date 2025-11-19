package com.openfinance.payment.api.exception;

import lombok.Getter;

@Getter
public class DuplicatePaymentException extends RuntimeException {
    private final String existingPaymentId;

    public DuplicatePaymentException(String message, String existingPaymentId) {
        super(message);
        this.existingPaymentId = existingPaymentId;
    }
}
