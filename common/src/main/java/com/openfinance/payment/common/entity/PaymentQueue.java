package com.openfinance.payment.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_queue", indexes = {
    @Index(name = "idx_next_retry", columnList = "next_retry_at"),
    @Index(name = "idx_queue_payment_id", columnList = "payment_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (nextRetryAt == null) {
            nextRetryAt = LocalDateTime.now();
        }
    }

    public void incrementRetry() {
        this.retryCount++;
        // Exponential backoff: 2^retryCount seconds
        this.nextRetryAt = LocalDateTime.now().plusSeconds((long) Math.pow(2, retryCount));
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public boolean isReadyForProcessing() {
        return nextRetryAt != null && LocalDateTime.now().isAfter(nextRetryAt);
    }
}
