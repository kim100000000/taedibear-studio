package com.taedibear.studio.post;

import java.util.Arrays;
import java.util.List;

// hashtags는 DB(TEXT, 쉼표 구분)와 API(JSON 배열) 간 형태가 달라 변환이 필요하다.
// Node 버전 controllers/posts.controller.js의 hashtagsToString/hashtagsToArray와 동일한 규칙.
public class HashtagUtil {

	private HashtagUtil() {
	}

	public static String toStorageString(List<String> hashtags) {
		if (hashtags == null || hashtags.isEmpty()) {
			return null;
		}
		return String.join(",", hashtags);
	}

	public static List<String> toList(String hashtagsStr) {
		if (hashtagsStr == null || hashtagsStr.isBlank()) {
			return List.of();
		}
		return Arrays.stream(hashtagsStr.split(",")).filter(s -> !s.isBlank()).toList();
	}
}
