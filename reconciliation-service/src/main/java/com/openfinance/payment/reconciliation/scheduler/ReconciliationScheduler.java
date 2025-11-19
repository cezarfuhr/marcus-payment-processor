package com.openfinance.payment.reconciliation.scheduler;

import com.openfinance.payment.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

    /**
     * Reconcile stuck payments every 30 seconds
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 5000)
    public void reconcileStuckPayments() {
        try {
            log.debug("Starting stuck payments reconciliation");
            reconciliationService.reconcileStuckPayments();
            log.debug("Finished stuck payments reconciliation");
        } catch (Exception e) {
            log.error("Error in stuck payments reconciliation", e);
        }
    }

    /**
     * Verify successful payments every 2 minutes
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 30000)
    public void verifySuccessfulPayments() {
        try {
            log.debug("Starting successful payments verification");
            reconciliationService.verifySuccessfulPayments();
            log.debug("Finished successful payments verification");
        } catch (Exception e) {
            log.error("Error in successful payments verification", e);
        }
    }
}
