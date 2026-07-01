package com.taedibear.studio.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

// GET /api/users/me/usage 응답 (Phase 2-1)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsageResponse(
        String plan,          // "free" | "pro"
        long used,            // 이번 달 업로드 수
        Integer limit,        // 제한 횟수 (free=10, pro=null=무제한)
        LocalDateTime reset_at // 다음 달 1일 00:00
) {}
