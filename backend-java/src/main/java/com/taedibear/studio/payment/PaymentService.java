package com.taedibear.studio.payment;

import com.taedibear.studio.common.exception.ApiException;
import com.taedibear.studio.domain.Payment;
import com.taedibear.studio.domain.PaymentStatus;
import com.taedibear.studio.domain.Plan;
import com.taedibear.studio.domain.User;
import com.taedibear.studio.payment.dto.*;
import com.taedibear.studio.repository.PaymentRepository;
import com.taedibear.studio.repository.PostRepository;
import com.taedibear.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final WebClient webClient;

    @Value("${app.toss.secret-key:}")
    private String tossSecretKey;

    // C2: true일 때만 시크릿 키 없이 토스 API 호출을 스킵 (로컬 개발 전용, 기본 false)
    @Value("${app.toss.dev-mode:false}")
    private boolean tossDevMode;

    private static final int FREE_LIMIT = 10;

    // C1: Pro 플랜 가격 (frontend PlanSection.tsx의 amount: 29000과 동일하게 유지)
    // 결제 금액은 반드시 서버가 아는 가격과 대조해야 한다 — 클라이언트가 보낸 amount를
    // 그대로 믿으면 조작된 소액 결제(예: 100원)로도 Pro 승인이 가능하다.
    private static final int PRO_PLAN_PRICE = 29000;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_CANCEL_URL = "https://api.tosspayments.com/v1/payments/{paymentKey}/cancel";

    // ── 결제 승인 ──────────────────────────────────────────────────────────────

    @Transactional
    public PaymentStatusResponse confirm(Long userId, ConfirmPaymentRequest request) {
        User user = getUser(userId);

        // C1: 결제 금액 서버 검증 — 토스 승인 API 호출 전에 반드시 수행
        if (request.amount() == null || request.amount() != PRO_PLAN_PRICE) {
            log.warn("[Payment] 금액 불일치 — userId={}, 요청 금액={}, 정상 가격={}",
                    userId, request.amount(), PRO_PLAN_PRICE);
            throw ApiException.badRequest("결제 금액이 올바르지 않아요.");
        }

        // 중복 결제 방지
        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw ApiException.badRequest("이미 처리된 주문이에요.");
        }

        // 토스 결제 승인 API 호출
        callTossConfirm(request.paymentKey(), request.orderId(), request.amount());

        // DB 저장
        LocalDateTime validUntil = LocalDateTime.now().plusDays(30);
        Payment payment = Payment.builder()
                .userId(userId)
                .paymentKey(request.paymentKey())
                .orderId(request.orderId())
                .amount(request.amount())
                .status(PaymentStatus.PAID)
                .validUntil(validUntil)
                .build();
        paymentRepository.save(payment);

        // 플랜 업그레이드
        user.setPlan(Plan.pro);

        return new PaymentStatusResponse("pro", request.paymentKey(), validUntil, request.amount());
    }

    // ── 결제 취소 (구독 해지) ───────────────────────────────────────────────────

    @Transactional
    public void cancel(Long userId, CancelPaymentRequest request) {
        Payment payment = paymentRepository.findByPaymentKey(request.paymentKey())
                .orElseThrow(() -> ApiException.notFound("결제 내역을 찾을 수 없어요."));

        if (!payment.getUserId().equals(userId)) {
            throw ApiException.forbidden("권한이 없어요.");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw ApiException.badRequest("취소할 수 없는 결제예요.");
        }

        // 토스 취소 API 호출
        callTossCancel(request.paymentKey(), request.cancelReason());

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setCanceledAt(LocalDateTime.now());

        // 플랜 다운그레이드
        getUser(userId).setPlan(Plan.free);
    }

    // ── 현재 결제 상태 조회 ─────────────────────────────────────────────────────

    public PaymentStatusResponse getStatus(Long userId) {
        User user = getUser(userId);
        if (user.getPlan() == Plan.free) {
            return new PaymentStatusResponse("free", null, null, null);
        }

        return paymentRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, PaymentStatus.PAID)
                .map(p -> new PaymentStatusResponse("pro", p.getPaymentKey(), p.getValidUntil(), p.getAmount()))
                .orElse(new PaymentStatusResponse("pro", null, null, null));
    }

    // ── 사용량 조회 ─────────────────────────────────────────────────────────────

    public UsageResponse getUsage(Long userId) {
        User user = getUser(userId);
        LocalDate today = LocalDate.now();
        long used = postRepository.countThisMonth(userId, today.getYear(), today.getMonthValue());

        // 다음 달 1일
        LocalDateTime resetAt = YearMonth.now().plusMonths(1)
                .atDay(1).atStartOfDay();

        if (user.getPlan() == Plan.pro) {
            return new UsageResponse("pro", used, null, resetAt);
        }
        return new UsageResponse("free", used, FREE_LIMIT, resetAt);
    }

    // ── 플랜 제한 체크 (PostService에서 호출) ────────────────────────────────────

    // 사용자 행 잠금(FOR UPDATE)으로 동시 요청을 직렬화 — 잠금 없이는 "카운트 조회 → 검사 → INSERT"
    // 사이에 다른 요청이 끼어들어 월 10회 제한을 초과할 수 있다.
    // MANDATORY: 호출자의 트랜잭션(PostService.createPost) 안에서만 실행되도록 강제.
    // 트랜잭션 없이 호출하면 잠금이 무의미해지므로 예외를 던지게 한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void checkPlanLimit(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없어요."));
        if (user.getPlan() == Plan.pro) return;

        LocalDate today = LocalDate.now();
        long used = postRepository.countThisMonth(userId, today.getYear(), today.getMonthValue());
        if (used >= FREE_LIMIT) {
            throw ApiException.forbidden("이번 달 무료 업로드 " + FREE_LIMIT + "회를 모두 사용했어요. Pro 플랜으로 업그레이드하면 무제한으로 업로드할 수 있어요.");
        }
    }

    // ── 내부: 토스 API 호출 ─────────────────────────────────────────────────────

    private void callTossConfirm(String paymentKey, String orderId, Integer amount) {
        if (tossSecretKey == null || tossSecretKey.isBlank()) {
            // C2: 키 누락은 설정 오류다. dev-mode가 명시적으로 켜진 경우에만 스킵을 허용하고,
            // 그 외에는 결제를 실패시켜 "승인 없이 Pro 부여"를 원천 차단한다.
            if (tossDevMode) {
                log.warn("[Toss] dev-mode — 결제 승인 API 호출 스킵 (로컬 개발 전용, 운영 금지)");
                return;
            }
            log.error("[Toss] secret key 미설정 상태에서 결제 승인 요청 — 설정 오류");
            throw ApiException.internal("결제 설정에 문제가 있어요. 관리자에게 문의해주세요.");
        }
        try {
            webClient.post()
                    .uri(TOSS_CONFIRM_URL)
                    .header(HttpHeaders.AUTHORIZATION, basicAuth(tossSecretKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                    .retrieve()
                    .onStatus(HttpStatus.BAD_REQUEST::equals,
                            r -> r.bodyToMono(String.class).map(body -> ApiException.badRequest("결제 승인에 실패했어요: " + body)))
                    .toBodilessEntity()
                    .block();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Toss] confirm error", e);
            throw ApiException.internal("결제 승인 중 오류가 발생했어요.");
        }
    }

    private void callTossCancel(String paymentKey, String reason) {
        if (tossSecretKey == null || tossSecretKey.isBlank()) {
            if (tossDevMode) {
                log.warn("[Toss] dev-mode — 결제 취소 API 호출 스킵 (로컬 개발 전용, 운영 금지)");
                return;
            }
            log.error("[Toss] secret key 미설정 상태에서 결제 취소 요청 — 설정 오류");
            throw ApiException.internal("결제 설정에 문제가 있어요. 관리자에게 문의해주세요.");
        }
        try {
            webClient.post()
                    .uri(TOSS_CANCEL_URL, paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, basicAuth(tossSecretKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("cancelReason", reason != null ? reason : "구독 해지"))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("[Toss] cancel error", e);
            throw ApiException.internal("결제 취소 중 오류가 발생했어요.");
        }
    }

    private String basicAuth(String secretKey) {
        String credentials = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없어요."));
    }
}
