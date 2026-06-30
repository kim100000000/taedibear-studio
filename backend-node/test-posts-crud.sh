#!/bin/bash
# 7단계 게시물 CRUD API 테스트 스크립트
# 사용법: JWT 값을 본인 토큰으로 바꾼 뒤 ./test-posts-crud.sh 실행
# (실행 권한이 없다면 먼저: chmod +x test-posts-crud.sh)

JWT="여기에_본인_JWT_붙여넣기"
BASE_URL="http://localhost:4000"
IMAGE_URL="https://taedibear-studio-images.s3.ap-northeast-2.amazonaws.com/images/46634aa1-9e16-4e53-8d5d-25b7fde90419.png"

echo "=== 1. 게시물 생성 (POST /api/posts) ==="
CREATE_RES=$(curl -s -X POST "$BASE_URL/api/posts" \
  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d "{\"instagram_account_id\":1,\"image_url\":\"$IMAGE_URL\",\"caption\":\"테스트 캡션\",\"hashtags\":[\"#카페\",\"#커피\"]}")
echo "$CREATE_RES"

POST_ID=$(echo "$CREATE_RES" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "생성된 post id: $POST_ID"
echo

echo "=== 2. 목록 조회 (GET /api/posts) ==="
curl -s "$BASE_URL/api/posts" -H "Authorization: Bearer $JWT"
echo
echo

echo "=== 3. 상세 조회 (GET /api/posts/$POST_ID) ==="
curl -s "$BASE_URL/api/posts/$POST_ID" -H "Authorization: Bearer $JWT"
echo
echo

echo "=== 4. 수정 (PUT /api/posts/$POST_ID) ==="
curl -s -X PUT "$BASE_URL/api/posts/$POST_ID" \
  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
  -d '{"caption":"수정된 캡션","hashtags":["#카페","#수정됨"]}'
echo
echo

echo "=== 5. 삭제 (DELETE /api/posts/$POST_ID) ==="
curl -s -X DELETE "$BASE_URL/api/posts/$POST_ID" -H "Authorization: Bearer $JWT"
echo
echo

echo "=== 6. 삭제 확인 (GET /api/posts/$POST_ID, 404가 정상) ==="
curl -s "$BASE_URL/api/posts/$POST_ID" -H "Authorization: Bearer $JWT"
echo
