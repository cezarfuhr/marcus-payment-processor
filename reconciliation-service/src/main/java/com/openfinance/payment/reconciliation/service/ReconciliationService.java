package com.openfinance.payment.reconciliation.service;

import com.openfinance.payment.common.entity.AuditLog;
import com.openfinance.payment.common.entity.Payment;
import com.openfinance.payment.common.entity.PaymentStatus;
import com.openfinance.payment.common.repository.AuditLogRepository;
import com.openfinance.payment.common.repository.PaymentRepository;
import com.openfinance.payment.reconciliation.client.MockBankClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReconciliationService {

    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final MockBankClient bankClient;
    private final Counter reconciledCounter;
    private final Counter inconsistenciesCounter;

    public ReconciliationService(PaymentRepository paymentRepository,
                                 AuditLogRepository auditLogRepository,
                                 MockBankClient bankClient,
                                 MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
        this.bankClient = bankClient;

        this.reconciledCounter = Counter.builder("payments.reconciled")
                .description("Successfully reconciled payments")
                .register(meterRegistry);

        this.inconsistenciesCounter = Counter.builder("payments.inconsistencies")
                .description("Payment status inconsistencies detected")
                .register(meterRegistry);
    }

    /**
     * Find and reconcile stuck payments (PROCESSING for more than 5 minutes)
     */
    @Transactional
    public void reconcileStuckPayments() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Payment> stuckPayments = paymentRepository.findStuckPayments(fiveMinutesAgo);

        if (stuckPayments.isEmpty()) {
            log.trace("No stuck payments found");
            return;
        }

        log.info("Found {} stuck payments to reconcile", stuckPayments.size());

        for (Payment payment : stuckPayments) {
            try {
                reconcilePayment(payment);
            } catch (Exception e) {
                log.error("Error reconciling payment: paymentId={}", payment.getPaymentId(), e);
            }
        }
    }

    /**
     * Reconcile payments with SUCCESS status to verify with bank
     */
    @Transactional
    public void verifySuccessfulPayments() {
        // For demo purposes, randomly select some successful payments to verify
        List<Payment> successfulPayments = paymentRepository.findByStatusAndUpdatedAtBefore(
                PaymentStatus.SUCCESS,
                LocalDateTime.now().minusMinutes(10)
        );

        if (successfulPayments.isEmpty()) {
            log.trace("No successful payments to verify");
            return;
        }

        // Verify a sample (up to 10 payments)
        int toVerify = Math.min(10, successfulPayments.size());
        log.info("Verifying {} successful payments", toVerify);

        for (int i = 0; i < toVerify; i++) {
            Payment payment = successfulPayments.get(i);
            try {
                verifyPaymentWithBank(payment);
            } catch (Exception e) {
                log.error("Error verifying payment: paymentId={}", payment.getPaymentId(), e);
            }
        }
    }

    private void reconcilePayment(Payment payment) {
        log.info("Reconciling stuck payment: paymentId={}, status={}, lastUpdate={}",
                payment.getPaymentId(), payment.getStatus(), payment.getUpdatedAt());

        PaymentStatus oldStatus = payment.getStatus();

        if (payment.getConfirmationCode() != null && !payment.getConfirmationCode().isEmpty()) {
            // We have a confirmation code, query bank
            MockBankClient.BankResponse bankResponse = bankClient.queryPaymentStatus(payment.getConfirmationCode());

            if (bankResponse.success() && bankResponse.status().equals("SUCCESS")) {
                // Bank confirms success, update our status
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProcessedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("previous_status", oldStatus.name());
                metadata.put("reconciliation_reason", "Bank confirmed success");
                metadata.put("confirmation_code", payment.getConfirmationCode());
                createAuditLog(payment, AuditLog.EventType.RECONCILED, oldStatus, PaymentStatus.SUCCESS, metadata);

                reconciledCounter.increment();
                inconsistenciesCounter.increment();
                log.warn("Payment status inconsistency reconciled: paymentId={}, {} -> SUCCESS",
                        payment.getPaymentId(), oldStatus);

            } else if (!bankResponse.success() || bankResponse.status().equals("FAILED")) {
                // Bank says failed
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Bank reconciliation: payment not found or failed");
                paymentRepository.save(payment);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("previous_status", oldStatus.name());
                metadata.put("reconciliation_reason", "Bank reported failure");
                createAuditLog(payment, AuditLog.EventType.RECONCILED, oldStatus, PaymentStatus.FAILED, metadata);

                reconciledCounter.increment();
                inconsistenciesCounter.increment();
                log.warn("Payment marked as failed after reconciliation: paymentId={}", payment.getPaymentId());
            }
        } else {
            // No confirmation code, mark as failed after timeout
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Processing timeout - no confirmation received");
            paymentRepository.save(payment);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("previous_status", oldStatus.name());
            metadata.put("reconciliation_reason", "Timeout without confirmation code");
            createAuditLog(payment, AuditLog.EventType.RECONCILED, oldStatus, PaymentStatus.FAILED, metadata);

            reconciledCounter.increment();
            log.warn("Payment marked as failed due to timeout: paymentId={}", payment.getPaymentId());
        }
    }

    private void verifyPaymentWithBank(Payment payment) {
        if (payment.getConfirmationCode() == null) {
            return;
        }

        MockBankClient.BankResponse bankResponse = bankClient.queryPaymentStatus(payment.getConfirmationCode());

        if (!bankResponse.success() || !bankResponse.status().equals("SUCCESS")) {
            // Inconsistency detected
            log.warn("Payment verification failed: paymentId={}, systemStatus={}, bankStatus={}",
                    payment.getPaymentId(), payment.getStatus(), bankResponse.status());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("system_status", payment.getStatus().name());
            metadata.put("bank_status", bankResponse.status());
            metadata.put("verification_time", LocalDateTime.now().toString());
            createAuditLog(payment, AuditLog.EventType.RECONCILED, payment.getStatus(), payment.getStatus(), metadata);

            inconsistenciesCounter.increment();
        } else {
            log.debug("Payment verification successful: paymentId={}", payment.getPaymentId());
        }
    }

    private void createAuditLog(Payment payment, AuditLog.EventType eventType,
                                PaymentStatus oldStatus, PaymentStatus newStatus,
                                Map<String, Object> metadata) {
        AuditLog auditLog = AuditLog.builder()
                .paymentId(payment.getId())
                .eventType(eventType)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .metadata(metadata)
                .build();
        auditLogRepository.save(auditLog);
    }
}
