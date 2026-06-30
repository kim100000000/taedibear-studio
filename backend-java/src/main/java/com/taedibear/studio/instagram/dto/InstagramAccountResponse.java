package com.taedibear.studio.instagram.dto;

import java.time.LocalDateTime;

public record InstagramAccountResponse(Long id, String instagram_user_id, String username, LocalDateTime connected_at) {
}
