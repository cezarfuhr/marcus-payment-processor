package com.openfinance.payment.common.repository;

import com.openfinance.payment.common.entity.PaymentQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentQueueRepository extends JpaRepository<PaymentQueue, Long> {

    Optional<PaymentQueue> findByPaymentId(UUID paymentId);

    @Query("SELECT pq FROM PaymentQueue pq WHERE pq.nextRetryAt <= :now AND pq.retryCount < pq.maxRetries ORDER BY pq.nextRetryAt ASC")
    List<PaymentQueue> findReadyForProcessing(LocalDateTime now);

    @Query("SELECT COUNT(pq) FROM PaymentQueue pq WHERE pq.retryCount < pq.maxRetries")
    long countPendingItems();

    void deleteByPaymentId(UUID paymentId);
}
