package com.openfinance.payment.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Payment type is required")
    @Pattern(regexp = "PIX|TED|BOLETO", message = "Payment type must be PIX, TED, or BOLETO")
    private String type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "10000.00", message = "Amount cannot exceed 10000.00")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "BRL";

    @Valid
    @NotNull(message = "Sender information is required")
    private Sender sender;

    @Valid
    @NotNull(message = "Receiver information is required")
    private Receiver receiver;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sender {
        @NotBlank(message = "Sender document is required")
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "Document must be a valid CPF (11 digits) or CNPJ (14 digits)")
        private String document;

        @NotBlank(message = "Sender bank code is required")
        @Pattern(regexp = "\\d{3}", message = "Bank code must be 3 digits")
        private String bankCode;

        @NotBlank(message = "Sender account is required")
        private String account;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Receiver {
        @NotBlank(message = "PIX key is required")
        private String pixKey;

        @NotBlank(message = "PIX key type is required")
        @Pattern(regexp = "EMAIL|CPF|CNPJ|PHONE|RANDOM", message = "PIX key type must be EMAIL, CPF, CNPJ, PHONE, or RANDOM")
        private String pixKeyType;
    }
}
