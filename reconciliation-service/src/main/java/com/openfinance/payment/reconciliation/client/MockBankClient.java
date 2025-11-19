package com.openfinance.payment.reconciliation.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Mock bank client for reconciliation service
 * Simulates querying bank APIs for payment status
 */
@Component
@Slf4j
public class MockBankClient {

    private static final Random RANDOM = new SecureRandom();
    private static final double CONFIRMED_RATE = 0.95; // 95% of queries return confirmed

    /**
     * Query bank for payment status using confirmation code
     */
    public BankResponse queryPaymentStatus(String confirmationCode) {
        log.debug("Querying bank for payment status: confirmationCode={}", confirmationCode);

        try {
            // Simulate network delay
            Thread.sleep(RANDOM.nextInt(300) + 100); // 100-400ms

            boolean confirmed = RANDOM.nextDouble() < CONFIRMED_RATE;
            String status = confirmed ? "SUCCESS" : "FAILED";

            log.debug("Bank query result: confirmationCode={}, status={}", confirmationCode, status);

            return BankResponse.builder()
                    .success(confirmed)
                    .status(status)
                    .confirmationCode(confirmationCode)
                    .message(confirmed ? "Payment confirmed" : "Payment not found or failed")
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Bank query interrupted: confirmationCode={}", confirmationCode, e);

            return BankResponse.builder()
                    .success(false)
                    .status("ERROR")
                    .message("Query interrupted")
                    .build();
        }
    }

    public record BankResponse(
            boolean success,
            String status,
            String confirmationCode,
            String message
    ) {
        public static BankResponseBuilder builder() {
            return new BankResponseBuilder();
        }
    }

    public static class BankResponseBuilder {
        private boolean success;
        private String status;
        private String confirmationCode;
        private String message;

        public BankResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public BankResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public BankResponseBuilder confirmationCode(String confirmationCode) {
            this.confirmationCode = confirmationCode;
            return this;
        }

        public BankResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public BankResponse build() {
            return new BankResponse(success, status, confirmationCode, message);
        }
    }
}
