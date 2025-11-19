package com.openfinance.payment.common.repository;

import com.openfinance.payment.common.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
