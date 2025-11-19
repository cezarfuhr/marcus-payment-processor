package com.openfinance.payment.processing.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

/**
 * Mock bank client to simulate communication with banking institutions
 * In production, this would be replaced with real HTTP clients to bank APIs
 */
@Component
@Slf4j
public class MockBankClient {

    private static final Random RANDOM = new SecureRandom();
    private static final double SUCCESS_RATE = 0.85; // 85% success rate

    /**
     * Simulates processing a PIX payment with a bank
     * @param paymentId The payment ID
     * @param amount The payment amount
     * @param pixKey The receiver PIX key
     * @return BankResponse with status and confirmation code
     */
    public BankResponse processPixPayment(UUID paymentId, String amount, String pixKey) {
        log.info("Processing PIX payment with bank: paymentId={}, amount={}, pixKey={}",
                paymentId, amount, maskPixKey(pixKey));

        try {
            // Simulate network delay
            Thread.sleep(RANDOM.nextInt(1000) + 500); // 500-1500ms

            // Simulate success/failure based on success rate
            boolean success = RANDOM.nextDouble() < SUCCESS_RATE;

            if (success) {
                String confirmationCode = generateConfirmationCode();
                log.info("Bank processing successful: paymentId={}, confirmationCode={}",
                        paymentId, confirmationCode);

                return BankResponse.builder()
                        .success(true)
                        .confirmationCode(confirmationCode)
                        .message("Payment processed successfully")
                        .build();
            } else {
                String errorMessage = getRandomErrorMessage();
                log.warn("Bank processing failed: paymentId={}, error={}",
                        paymentId, errorMessage);

                return BankResponse.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .message("Payment processing failed")
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted: paymentId={}", paymentId, e);

            return BankResponse.builder()
                    .success(false)
                    .errorMessage("Processing interrupted")
                    .build();
        }
    }

    /**
     * Queries bank for payment status
     * @param confirmationCode The confirmation code from previous processing
     * @return BankResponse with current status
     */
    public BankResponse queryPaymentStatus(String confirmationCode) {
        log.info("Querying payment status with bank: confirmationCode={}", confirmationCode);

        try {
            // Simulate network delay
            Thread.sleep(RANDOM.nextInt(300) + 100); // 100-400ms

            // Most queries return success (95%)
            boolean success = RANDOM.nextDouble() < 0.95;

            return BankResponse.builder()
                    .success(success)
                    .confirmationCode(confirmationCode)
                    .message(success ? "Payment confirmed" : "Payment not found")
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Status query interrupted: confirmationCode={}", confirmationCode, e);

            return BankResponse.builder()
                    .success(false)
                    .errorMessage("Query interrupted")
                    .build();
        }
    }

    private String generateConfirmationCode() {
        // E + 11 digits + date + sequence
        // Format: E12345678202511191900001234567890
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        long sequence = RANDOM.nextLong(1000000000L);

        return String.format("E%011d%s%09d", RANDOM.nextLong(100000000000L), date, sequence);
    }

    private String getRandomErrorMessage() {
        String[] errors = {
                "Insufficient funds",
                "Invalid PIX key",
                "Bank system temporarily unavailable",
                "Daily limit exceeded",
                "Account blocked",
                "Timeout communicating with bank"
        };
        return errors[RANDOM.nextInt(errors.length)];
    }

    private String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.length() <= 4) {
            return "****";
        }
        return pixKey.substring(0, 2) + "****" + pixKey.substring(pixKey.length() - 2);
    }

    public record BankResponse(
            boolean success,
            String confirmationCode,
            String message,
            String errorMessage
    ) {
        public static BankResponseBuilder builder() {
            return new BankResponseBuilder();
        }
    }

    public static class BankResponseBuilder {
        private boolean success;
        private String confirmationCode;
        private String message;
        private String errorMessage;

        public BankResponseBuilder success(boolean success) {
            this.success = success;
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

        public BankResponseBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public BankResponse build() {
            return new BankResponse(success, confirmationCode, message, errorMessage);
        }
    }
}
