package com.taedibear.studio.repository;

import com.taedibear.studio.domain.Payment;
import com.taedibear.studio.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    // 유효한 Pro 구독이 존재하는지 확인 (validUntil > now, status = PAID)
    boolean existsByUserIdAndStatusAndValidUntilAfter(Long userId, PaymentStatus status, LocalDateTime now);

    // 가장 최근 PAID 결제 조회
    Optional<Payment> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);
}
