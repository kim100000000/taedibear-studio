const axios = require('axios');

// 네이버는 passport 전략 패키지 없이, Meta 연동(meta.service.js)과 같은 방식으로
// axios로 직접 OAuth 흐름(인증 URL → 토큰 교환 → 프로필 조회)을 구현한다.

function getLoginUrl(state) {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: process.env.NAVER_CLIENT_ID,
    redirect_uri: process.env.NAVER_CALLBACK_URL,
    state,
  });
  return `https://nid.naver.com/oauth2.0/authorize?${params.toString()}`;
}

async function exchangeCodeForToken(code, state) {
  const { data } = await axios.get('https://nid.naver.com/oauth2.0/token', {
    params: {
      grant_type: 'authorization_code',
      client_id: process.env.NAVER_CLIENT_ID,
      client_secret: process.env.NAVER_CLIENT_SECRET,
      code,
      state,
    },
  });
  return data.access_token;
}

async function getProfile(accessToken) {
  const { data } = await axios.get('https://openapi.naver.com/v1/nid/me', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  if (data.resultcode !== '00') {
    throw new Error(data.message || '네이버 프로필 조회에 실패했어요.');
  }

  const { id, email, name, nickname } = data.response;
  return { naverId: id, email, name: name || nickname };
}

module.exports = { getLoginUrl, exchangeCodeForToken, getProfile };
