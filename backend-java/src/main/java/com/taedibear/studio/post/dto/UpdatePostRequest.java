package com.taedibear.studio.post.dto;

import java.util.List;

public record UpdatePostRequest(String caption, List<String> hashtags) {
}
