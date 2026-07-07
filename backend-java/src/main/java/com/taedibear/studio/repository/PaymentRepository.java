package com.taedibear.studio.repository;

import com.taedibear.studio.domain.Payment;
import com.taedibear.studio.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentKey(String paymentKey);

    // 유효한 Pro 구독이 존재하는지 확인 (validUntil > now, status = PAID)
    boolean existsByUserIdAndStatusAndValidUntilAfter(Long userId, PaymentStatus status, LocalDateTime now);

    // 가장 최근 PAID 결제 조회
    Optional<Payment> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);

    // ── Phase 5-1 관리자 대시보드 ────────────────────────────────────────────
    /** 기간 내 PAID 결제 금액 합계 (이번 달 매출) */
    @Query("select coalesce(sum(p.amount), 0) from Payment p "
            + "where p.status = :status and p.createdAt >= :since")
    long sumAmountByStatusSince(@Param("status") PaymentStatus status,
                                @Param("since") LocalDateTime since);

    /** 현재 유효한 Pro 구독 수 (status = PAID, validUntil > now) */
    long countByStatusAndValidUntilAfter(PaymentStatus status, LocalDateTime now);

    /** 최근 결제 내역 (관리자 결제 목록) */
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
