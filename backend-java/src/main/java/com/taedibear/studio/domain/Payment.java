package com.taedibear.studio.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// docs/04_DB설계서.md 결제 테이블 — Phase 2-1 (토스페이먼츠 연동)
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 토스페이먼츠 결제 키 (결제 승인 후 발급)
    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    // 클라이언트에서 생성한 주문 ID (UUID 권장)
    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    // 결제 금액 (원)
    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PAID;

    // 구독 유효 기간 (paidAt + 30일)
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
