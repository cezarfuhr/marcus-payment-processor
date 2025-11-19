package com.openfinance.payment.processing.service;

import com.openfinance.payment.common.entity.*;
import com.openfinance.payment.common.repository.AuditLogRepository;
import com.openfinance.payment.common.repository.PaymentQueueRepository;
import com.openfinance.payment.common.repository.PaymentRepository;
import com.openfinance.payment.processing.client.MockBankClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final PaymentQueueRepository queueRepository;
    private final AuditLogRepository auditLogRepository;
    private final MockBankClient bankClient;
    private final Counter paymentsSuccessCounter;
    private final Counter paymentsFailedCounter;
    private final Timer processingDurationTimer;

    public PaymentProcessingService(PaymentRepository paymentRepository,
                                    PaymentQueueRepository queueRepository,
                                    AuditLogRepository auditLogRepository,
                                    MockBankClient bankClient,
                                    MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.queueRepository = queueRepository;
        this.auditLogRepository = auditLogRepository;
        this.bankClient = bankClient;

        this.paymentsSuccessCounter = Counter.builder("payments.success")
                .description("Successfully processed payments")
                .register(meterRegistry);

        this.paymentsFailedCounter = Counter.builder("payments.failed")
                .description("Failed payments")
                .register(meterRegistry);

        this.processingDurationTimer = Timer.builder("payments.processing.duration")
                .description("Payment processing duration")
                .register(meterRegistry);
    }

    @Transactional
    public void processPayment(UUID paymentId) {
        Timer.Sample sample = Timer.start();

        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

            log.info("Processing payment: paymentId={}, status={}", payment.getPaymentId(), payment.getStatus());

            // Update status to PROCESSING
            PaymentStatus oldStatus = payment.getStatus();
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);
            createAuditLog(paymentId, AuditLog.EventType.STATUS_CHANGED, oldStatus, PaymentStatus.PROCESSING);

            // Process with bank
            MockBankClient.BankResponse bankResponse = bankClient.processPixPayment(
                    paymentId,
                    payment.getAmount().toString(),
                    payment.getReceiverPixKey()
            );

            if (bankResponse.success()) {
                // Success - update payment
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setConfirmationCode(bankResponse.confirmationCode());
                payment.setProcessedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                // Remove from queue
                queueRepository.findByPaymentId(paymentId)
                        .ifPresent(queueRepository::delete);

                // Create audit log
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("confirmation_code", bankResponse.confirmationCode());
                metadata.put("message", bankResponse.message());
                createAuditLog(paymentId, AuditLog.EventType.STATUS_CHANGED,
                        PaymentStatus.PROCESSING, PaymentStatus.SUCCESS, metadata);

                paymentsSuccessCounter.increment();
                log.info("Payment processed successfully: paymentId={}, confirmationCode={}",
                        payment.getPaymentId(), bankResponse.confirmationCode());

            } else {
                // Failure - check if we should retry
                PaymentQueue queueItem = queueRepository.findByPaymentId(paymentId)
                        .orElseThrow(() -> new RuntimeException("Queue item not found: " + paymentId));

                queueItem.setLastError(bankResponse.errorMessage());

                if (queueItem.canRetry()) {
                    // Retry later
                    queueItem.incrementRetry();
                    queueRepository.save(queueItem);

                    payment.setStatus(PaymentStatus.PENDING);
                    payment.setFailureReason(String.format("Retry %d/%d: %s",
                            queueItem.getRetryCount(), queueItem.getMaxRetries(), bankResponse.errorMessage()));
                    paymentRepository.save(payment);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("retry_count", queueItem.getRetryCount());
                    metadata.put("next_retry_at", queueItem.getNextRetryAt().toString());
                    metadata.put("error", bankResponse.errorMessage());
                    createAuditLog(paymentId, AuditLog.EventType.RETRY_ATTEMPTED,
                            PaymentStatus.PROCESSING, PaymentStatus.PENDING, metadata);

                    log.warn("Payment processing failed, will retry: paymentId={}, retryCount={}, error={}",
                            payment.getPaymentId(), queueItem.getRetryCount(), bankResponse.errorMessage());

                } else {
                    // Max retries reached - mark as failed
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Max retries exceeded: " + bankResponse.errorMessage());
                    paymentRepository.save(payment);

                    queueRepository.delete(queueItem);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("error", bankResponse.errorMessage());
                    metadata.put("retry_count", queueItem.getRetryCount());
                    createAuditLog(paymentId, AuditLog.EventType.FAILED,
                            PaymentStatus.PROCESSING, PaymentStatus.FAILED, metadata);

                    paymentsFailedCounter.increment();
                    log.error("Payment processing failed permanently: paymentId={}, error={}",
                            payment.getPaymentId(), bankResponse.errorMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error processing payment: paymentId={}", paymentId, e);

            // Update queue for retry
            queueRepository.findByPaymentId(paymentId).ifPresent(queueItem -> {
                queueItem.setLastError(e.getMessage());
                if (queueItem.canRetry()) {
                    queueItem.incrementRetry();
                    queueRepository.save(queueItem);
                }
            });

            throw e;
        } finally {
            sample.stop(processingDurationTimer);
        }
    }

    private void createAuditLog(UUID paymentId, AuditLog.EventType eventType,
                                PaymentStatus oldStatus, PaymentStatus newStatus) {
        createAuditLog(paymentId, eventType, oldStatus, newStatus, new HashMap<>());
    }

    private void createAuditLog(UUID paymentId, AuditLog.EventType eventType,
                                PaymentStatus oldStatus, PaymentStatus newStatus,
                                Map<String, Object> metadata) {
        AuditLog auditLog = AuditLog.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .metadata(metadata)
                .build();
        auditLogRepository.save(auditLog);
    }
}
