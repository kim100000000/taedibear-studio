package com.taedibear.studio.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
