package com.openfinance.payment.api.controller;

import com.openfinance.payment.api.service.PaymentService;
import com.openfinance.payment.common.dto.PageResponse;
import com.openfinance.payment.common.dto.PaymentRequest;
import com.openfinance.payment.common.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader) {

        log.info("Received payment creation request: type={}, amount={}",
                request.getType(), request.getAmount());

        UUID idempotencyKey = idempotencyKeyHeader != null ? UUID.fromString(idempotencyKeyHeader) : null;
        PaymentResponse response = paymentService.createPayment(request, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        log.info("Fetching payment: {}", paymentId);
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> listPayments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        log.info("Listing payments: status={}, page={}, size={}", status, page, size);

        // Parse sort parameter
        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = sort[0];

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        PageResponse<PaymentResponse> response = paymentService.listPayments(status, pageable);

        return ResponseEntity.ok(response);
    }
}
