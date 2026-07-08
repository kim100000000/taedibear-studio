package com.taedibear.studio.post;

import com.taedibear.studio.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

// Node 버전 services/s3.service.js와 동일한 key/URL 규칙을 사용한다.
@Service
public class S3Service {

	private static final Map<String, String> EXT_BY_MIME = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp"
	);

	private final S3Client s3Client;
	private final String bucket;
	private final String region;
	private final String urlPrefix;

	public S3Service(
			@Value("${app.aws.region}") String region,
			@Value("${app.aws.access-key-id}") String accessKeyId,
			@Value("${app.aws.secret-access-key}") String secretAccessKey,
			@Value("${app.aws.s3-bucket}") String bucket) {
		this.region = region;
		this.bucket = bucket;
		this.urlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
		this.s3Client = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
				.build();
	}

	public static boolean isSupportedMimeType(String mimeType) {
		return EXT_BY_MIME.containsKey(mimeType);
	}

	// docs/05_API명세서.md: POST /api/posts/upload, JPG/PNG/WEBP만 허용, 최대 10MB(컨트롤러/설정에서 검사)
	public String uploadImage(MultipartFile file) {
		String ext = EXT_BY_MIME.get(file.getContentType());
		if (ext == null) {
			throw ApiException.badRequest("지원하지 않는 파일 형식이에요. JPG, PNG, WEBP만 가능해요.");
		}

		String key = "images/" + UUID.randomUUID() + "." + ext;

		try {
			s3Client.putObject(
					PutObjectRequest.builder()
							.bucket(bucket)
							.key(key)
							.contentType(file.getContentType())
							.build(),
					RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		} catch (IOException e) {
			throw ApiException.internal("이미지 업로드에 실패했어요.");
		}

		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
	}

	// GeminiService가 캡션 생성을 위해 업로드된 이미지를 다시 읽어올 때 사용.
	// 버킷이 public-read가 아니어도(기본값=비공개) 서버 자격증명으로 바로 읽어온다 —
	// 공개 URL로 재요청하면 버킷 ACL/정책에 따라 403이 나서 캡션 생성이 500으로 실패하던 문제 수정.
	public byte[] downloadImage(String imageUrl) {
		if (imageUrl == null || !imageUrl.startsWith(urlPrefix)) {
			throw ApiException.badRequest("이미지 URL이 올바르지 않아요. 업로드 후 발급된 이미지 주소를 사용해주세요.");
		}
		String key = imageUrl.substring(urlPrefix.length());
		try {
			return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build()).readAllBytes();
		} catch (IOException | S3Exception e) {
			throw ApiException.internal("이미지를 불러오는 데 실패했어요.");
		}
	}
}
