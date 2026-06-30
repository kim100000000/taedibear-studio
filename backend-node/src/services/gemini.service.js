const { GoogleGenerativeAI } = require('@google/generative-ai');
const axios = require('axios');

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);

// image_url의 이미지를 내려받아 Gemini에 넘길 수 있는 base64 inline data로 변환한다.
async function fetchImageAsInlineData(imageUrl) {
  const response = await axios.get(imageUrl, { responseType: 'arraybuffer' });
  const mimeType = response.headers['content-type'] || 'image/jpeg';
  const base64 = Buffer.from(response.data).toString('base64');
  return { inlineData: { mimeType, data: base64 } };
}

// 모델 응답에서 마크다운 코드펜스(```json ... ```)를 제거하고 JSON으로 파싱한다.
function parseJsonResponse(text) {
  const cleaned = text.replace(/```json\s*|```\s*/g, '').trim();
  return JSON.parse(cleaned);
}

/**
 * 이미지 + 업종 + 무드를 바탕으로 SNS 캡션과 해시태그를 생성한다.
 * docs/05_API명세서.md: POST /api/posts/caption
 * @param {string} imageUrl - S3에 업로드된 이미지 URL
 * @param {string} businessType - 업종 (예: 카페)
 * @param {string} mood - 분위기/톤 (예: 감성적인)
 * @returns {Promise<{caption: string, hashtags: string[]}>}
 */
async function generateCaption(imageUrl, businessType, mood) {
  const model = genAI.getGenerativeModel({
    model: process.env.GEMINI_MODEL || 'gemini-2.5-flash',
    systemInstruction:
      '당신은 소상공인을 위한 SNS 마케팅 카피라이터입니다. 첨부된 이미지를 보고 매력적인 인스타그램 게시물 캡션과 해시태그를 작성하세요. ' +
      '반드시 다른 설명 없이 아래 형식의 순수 JSON으로만 응답하세요: ' +
      '{"caption": "게시물 캡션 (이모지 포함 가능, 2~4문장)", "hashtags": ["#태그1", "#태그2", ... 10개]}',
  });

  const imagePart = await fetchImageAsInlineData(imageUrl);
  const prompt = `업종: ${businessType}\n분위기: ${mood}\n위 정보와 이미지를 참고해서 캡션과 해시태그 10개를 만들어줘.`;

  const result = await model.generateContent([prompt, imagePart]);
  const text = result.response.text();
  const parsed = parseJsonResponse(text);

  return {
    caption: parsed.caption,
    hashtags: Array.isArray(parsed.hashtags) ? parsed.hashtags : [],
  };
}

module.exports = { generateCaption };
