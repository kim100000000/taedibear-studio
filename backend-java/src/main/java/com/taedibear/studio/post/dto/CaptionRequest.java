package com.taedibear.studio.post.dto;

// Phase 2-3: special_menu / event_promotion / keywords 는 선택 입력 (null 허용)
public record CaptionRequest(
        String image_url,
        String business_type,
        String mood,
        String special_menu,       // 오늘의 특별 메뉴 (선택)
        String event_promotion,    // 이벤트/프로모션 내용 (선택)
        String keywords            // 강조할 키워드 (선택, 쉼표 구분 가능)
) {}
