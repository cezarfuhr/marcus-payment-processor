package com.openfinance.payment.api.service;

import com.openfinance.payment.api.exception.DuplicatePaymentException;
import com.openfinance.payment.api.exception.InvalidPaymentException;
import com.openfinance.payment.api.exception.PaymentNotFoundException;
import com.openfinance.payment.common.dto.PageResponse;
import com.openfinance.payment.common.dto.PaymentRequest;
import com.openfinance.payment.common.dto.PaymentResponse;
import com.openfinance.payment.common.entity.*;
import com.openfinance.payment.common.repository.AuditLogRepository;
import com.openfinance.payment.common.repository.PaymentQueueRepository;
import com.openfinance.payment.common.repository.PaymentRepository;
import com.openfinance.payment.common.util.PaymentIdGenerator;
import com.openfinance.payment.common.validator.CNPJValidator;
import com.openfinance.payment.common.validator.CPFValidator;
import com.openfinance.payment.common.validator.PixKeyValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentQueueRepository queueRepository;
    private final AuditLogRepository auditLogRepository;
    private final Counter paymentsCreatedCounter;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentQueueRepository queueRepository,
                          AuditLogRepository auditLogRepository,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.queueRepository = queueRepository;
        this.auditLogRepository = auditLogRepository;
        this.paymentsCreatedCounter = Counter.builder("payments.created")
                .description("Total payments created")
                .register(meterRegistry);
    }

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, UUID idempotencyKey) {
        log.info("Creating payment with idempotency key: {}", idempotencyKey);

        // Check idempotency
        if (idempotencyKey != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.warn("Duplicate payment request detected with idempotency key: {}", idempotencyKey);
                throw new DuplicatePaymentException(
                        "Payment already exists with this idempotency key",
                        existing.get().getPaymentId()
                );
            }
        }

        // Validate payment
        validatePayment(request);

        // Create payment entity
        Payment payment = Payment.builder()
                .paymentId(PaymentIdGenerator.generate())
                .idempotencyKey(idempotencyKey)
                .type(PaymentType.valueOf(request.getType()))
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .senderDocument(request.getSender().getDocument())
                .senderBankCode(request.getSender().getBankCode())
                .senderAccount(request.getSender().getAccount())
                .receiverPixKey(request.getReceiver().getPixKey())
                .receiverPixKeyType(request.getReceiver().getPixKeyType())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", payment.getPaymentId());

        // Add to processing queue
        PaymentQueue queueItem = PaymentQueue.builder()
                .paymentId(payment.getId())
                .retryCount(0)
                .maxRetries(3)
                .nextRetryAt(LocalDateTime.now())
                .build();
        queueRepository.save(queueItem);

        // Create audit log
        createAuditLog(payment.getId(), AuditLog.EventType.CREATED, null, PaymentStatus.PENDING);

        // Increment metrics
        paymentsCreatedCounter.increment();

        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        PaymentResponse response = toPaymentResponse(payment);

        // Add timeline from audit logs
        List<AuditLog> auditLogs = auditLogRepository.findByPaymentIdOrderByCreatedAtAsc(payment.getId());
        List<PaymentResponse.TimelineEvent> timeline = auditLogs.stream()
                .filter(log -> log.getNewStatus() != null)
                .map(log -> PaymentResponse.TimelineEvent.builder()
                        .status(log.getNewStatus().name())
                        .timestamp(log.getCreatedAt())
                        .description(log.getEventType().name())
                        .build())
                .collect(Collectors.toList());

        response.setTimeline(timeline);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listPayments(String status, Pageable pageable) {
        Page<Payment> paymentPage;

        if (status != null && !status.isEmpty()) {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            paymentPage = paymentRepository.findAllByStatus(paymentStatus, pageable);
        } else {
            paymentPage = paymentRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<PaymentResponse> content = paymentPage.getContent().stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());

        return PageResponse.<PaymentResponse>builder()
                .content(content)
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .build();
    }

    private void validatePayment(PaymentRequest request) {
        // Validate sender document (CPF or CNPJ)
        String document = request.getSender().getDocument();
        if (document.length() == 11) {
            if (!CPFValidator.isValid(document)) {
                throw new InvalidPaymentException("Invalid CPF: " + document);
            }
        } else if (document.length() == 14) {
            if (!CNPJValidator.isValid(document)) {
                throw new InvalidPaymentException("Invalid CNPJ: " + document);
            }
        } else {
            throw new InvalidPaymentException("Document must be 11 (CPF) or 14 (CNPJ) digits");
        }

        // Validate PIX key
        if (!PixKeyValidator.isValid(request.getReceiver().getPixKey(), request.getReceiver().getPixKeyType())) {
            throw new InvalidPaymentException(
                    String.format("Invalid PIX key '%s' for type '%s'",
                            request.getReceiver().getPixKey(),
                            request.getReceiver().getPixKeyType())
            );
        }
    }

    private void createAuditLog(UUID paymentId, AuditLog.EventType eventType,
                                PaymentStatus oldStatus, PaymentStatus newStatus) {
        AuditLog auditLog = AuditLog.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .metadata(new HashMap<>())
                .build();
        auditLogRepository.save(auditLog);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .confirmationCode(payment.getConfirmationCode())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .processedAt(payment.getProcessedAt())
                .estimatedCompletion(payment.getCreatedAt().plusSeconds(30))
                .build();
    }
}
