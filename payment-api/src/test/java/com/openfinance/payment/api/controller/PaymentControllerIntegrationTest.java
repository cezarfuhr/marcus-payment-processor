package com.openfinance.payment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openfinance.payment.common.dto.PaymentRequest;
import com.openfinance.payment.common.entity.Payment;
import com.openfinance.payment.common.entity.PaymentStatus;
import com.openfinance.payment.common.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Payment Controller Integration Tests")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @WithMockUser
    @DisplayName("Should create payment successfully")
    void shouldCreatePaymentSuccessfully() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .type("PIX")
                .amount(new BigDecimal("150.00"))
                .currency("BRL")
                .sender(PaymentRequest.Sender.builder()
                        .document("12345678909")
                        .bankCode("001")
                        .account("12345-6")
                        .build())
                .receiver(PaymentRequest.Receiver.builder()
                        .pixKey("user@example.com")
                        .pixKeyType("EMAIL")
                        .build())
                .build();

        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.amount", is(150.00)))
                .andExpect(jsonPath("$.currency", is("BRL")));

        // Verify payment was saved
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("Should enforce idempotency")
    void shouldEnforceIdempotency() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .type("PIX")
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .sender(PaymentRequest.Sender.builder()
                        .document("12345678909")
                        .bankCode("001")
                        .account("12345-6")
                        .build())
                .receiver(PaymentRequest.Receiver.builder()
                        .pixKey("user@example.com")
                        .pixKeyType("EMAIL")
                        .build())
                .build();

        String idempotencyKey = UUID.randomUUID().toString();

        // First request
        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second request with same idempotency key
        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_REQUEST")))
                .andExpect(jsonPath("$.existingPaymentId", notNullValue()));

        // Verify only one payment was created
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("Should validate payment request")
    void shouldValidatePaymentRequest() throws Exception {
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .type("INVALID_TYPE")
                .amount(new BigDecimal("-10.00")) // Negative amount
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should get payment by ID")
    void shouldGetPaymentById() throws Exception {
        // Create a payment first
        Payment payment = Payment.builder()
                .paymentId("PAY-2025-TEST001")
                .idempotencyKey(UUID.randomUUID())
                .type(com.openfinance.payment.common.entity.PaymentType.PIX)
                .amount(new BigDecimal("200.00"))
                .currency("BRL")
                .status(PaymentStatus.SUCCESS)
                .senderDocument("12345678909")
                .senderBankCode("001")
                .receiverPixKey("test@example.com")
                .receiverPixKeyType("EMAIL")
                .confirmationCode("E12345678202511191900001234567890")
                .build();
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payments/{paymentId}", "PAY-2025-TEST001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is("PAY-2025-TEST001")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.amount", is(200.00)))
                .andExpect(jsonPath("$.confirmationCode", is("E12345678202511191900001234567890")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 for non-existent payment")
    void shouldReturn404ForNonExistentPayment() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{paymentId}", "PAY-NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("PAYMENT_NOT_FOUND")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should list payments with pagination")
    void shouldListPaymentsWithPagination() throws Exception {
        // Create multiple payments
        for (int i = 0; i < 5; i++) {
            Payment payment = Payment.builder()
                    .paymentId("PAY-2025-TEST" + String.format("%03d", i))
                    .idempotencyKey(UUID.randomUUID())
                    .type(com.openfinance.payment.common.entity.PaymentType.PIX)
                    .amount(new BigDecimal("100.00"))
                    .currency("BRL")
                    .status(PaymentStatus.PENDING)
                    .senderDocument("12345678909")
                    .senderBankCode("001")
                    .receiverPixKey("test@example.com")
                    .receiverPixKeyType("EMAIL")
                    .build();
            paymentRepository.save(payment);
        }

        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(3)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    @DisplayName("Should require authentication")
    void shouldRequireAuthentication() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .type("PIX")
                .amount(new BigDecimal("150.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
