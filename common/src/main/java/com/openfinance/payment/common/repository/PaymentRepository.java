package com.openfinance.payment.common.repository;

import com.openfinance.payment.common.entity.Payment;
import com.openfinance.payment.common.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByIdempotencyKey(UUID idempotencyKey);

    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Payment> findByStatusAndUpdatedAtBefore(PaymentStatus status, LocalDateTime dateTime);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' AND p.updatedAt < :dateTime")
    List<Payment> findStuckPayments(LocalDateTime dateTime);
}
