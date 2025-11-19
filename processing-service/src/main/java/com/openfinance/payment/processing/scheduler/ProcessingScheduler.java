package com.openfinance.payment.processing.scheduler;

import com.openfinance.payment.common.entity.PaymentQueue;
import com.openfinance.payment.common.repository.PaymentQueueRepository;
import com.openfinance.payment.processing.service.PaymentProcessingService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ProcessingScheduler {

    private final PaymentQueueRepository queueRepository;
    private final PaymentProcessingService processingService;
    private final AtomicInteger queueSize = new AtomicInteger(0);

    public ProcessingScheduler(PaymentQueueRepository queueRepository,
                               PaymentProcessingService processingService,
                               MeterRegistry meterRegistry) {
        this.queueRepository = queueRepository;
        this.processingService = processingService;

        // Register gauge for queue size
        Gauge.builder("payments.queue.size", queueSize, AtomicInteger::get)
                .description("Current payment queue size")
                .register(meterRegistry);
    }

    /**
     * Process pending payments every 5 seconds
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    public void processPendingPayments() {
        try {
            List<PaymentQueue> readyItems = queueRepository.findReadyForProcessing(LocalDateTime.now());
            queueSize.set(readyItems.size());

            if (readyItems.isEmpty()) {
                log.trace("No payments ready for processing");
                return;
            }

            log.info("Found {} payments ready for processing", readyItems.size());

            for (PaymentQueue queueItem : readyItems) {
                try {
                    log.debug("Processing payment from queue: paymentId={}, retryCount={}",
                            queueItem.getPaymentId(), queueItem.getRetryCount());

                    processingService.processPayment(queueItem.getPaymentId());

                } catch (Exception e) {
                    log.error("Error processing payment from queue: paymentId={}",
                            queueItem.getPaymentId(), e);
                    // Continue with next payment
                }
            }

            log.info("Finished processing batch of {} payments", readyItems.size());

        } catch (Exception e) {
            log.error("Error in processing scheduler", e);
        }
    }

    /**
     * Log queue statistics every minute
     */
    @Scheduled(fixedDelay = 60000)
    public void logQueueStatistics() {
        try {
            long totalPending = queueRepository.countPendingItems();
            long totalInQueue = queueRepository.count();

            log.info("Queue statistics: totalInQueue={}, pendingItems={}", totalInQueue, totalPending);

            if (totalInQueue > 1000) {
                log.warn("High queue size detected: {} items in queue", totalInQueue);
            }

        } catch (Exception e) {
            log.error("Error logging queue statistics", e);
        }
    }
}
