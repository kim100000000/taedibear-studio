package com.taedibear.studio.post.dto;

import java.util.List;

public record PostListResponse(List<PostListItemResponse> posts, long total, int page, int limit) {
}
