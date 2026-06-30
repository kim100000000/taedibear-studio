package com.taedibear.studio.post.dto;

import java.util.List;

public record CaptionResponse(String caption, List<String> hashtags) {
}
