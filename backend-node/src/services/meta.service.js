const axios = require('axios');

const GRAPH_VERSION = process.env.META_GRAPH_API_VERSION || 'v19.0';
const BASE_URL = `https://graph.facebook.com/${GRAPH_VERSION}`;

// docs/05_API명세서.md: GET /api/instagram/connect → Meta 권한 승인 페이지로 리다이렉트
// state에는 호출한 사용자를 식별할 JWT를 그대로 담아 보낸다. Meta 콜백은 일반 브라우저
// 리다이렉트라 Authorization 헤더를 못 보내기 때문에, state 왕복으로 사용자를 식별한다.
function getLoginUrl(state) {
  const params = new URLSearchParams({
    client_id: process.env.META_APP_ID,
    redirect_uri: process.env.META_REDIRECT_URI,
    scope: [
      'instagram_basic',
      'instagram_content_publish',
      'pages_show_list',
      'pages_read_engagement',
      'business_management',
    ].join(','),
    response_type: 'code',
    state,
  });
  return `https://www.facebook.com/${GRAPH_VERSION}/dialog/oauth?${params.toString()}`;
}

// 인증 코드를 단기 액세스 토큰으로 교환
async function exchangeCodeForToken(code) {
  const { data } = await axios.get(`${BASE_URL}/oauth/access_token`, {
    params: {
      client_id: process.env.META_APP_ID,
      client_secret: process.env.META_APP_SECRET,
      redirect_uri: process.env.META_REDIRECT_URI,
      code,
    },
  });
  return data.access_token;
}

// 단기 토큰을 장기(약 60일) 토큰으로 교환
async function getLongLivedToken(shortLivedToken) {
  const { data } = await axios.get(`${BASE_URL}/oauth/access_token`, {
    params: {
      grant_type: 'fb_exchange_token',
      client_id: process.env.META_APP_ID,
      client_secret: process.env.META_APP_SECRET,
      fb_exchange_token: shortLivedToken,
    },
  });
  return { accessToken: data.access_token, expiresIn: data.expires_in };
}

// 사용자가 관리하는 페이지들 중 연결된 Instagram 비즈니스 계정 정보를 가져온다.
async function getInstagramBusinessAccount(accessToken) {
  const { data } = await axios.get(`${BASE_URL}/me/accounts`, {
    params: { access_token: accessToken, fields: 'instagram_business_account{id,username}' },
  });

  const pageWithIg = (data.data || []).find((page) => page.instagram_business_account);
  if (!pageWithIg) {
    throw new Error('연결된 페이지에 Instagram 비즈니스 계정이 없어요.');
  }

  return {
    instagramUserId: pageWithIg.instagram_business_account.id,
    username: pageWithIg.instagram_business_account.username,
  };
}

/**
 * Instagram Business 계정에 게시물을 발행한다. (1. 미디어 컨테이너 생성 -> 2. 게시)
 */
async function publishToInstagram({ igUserId, accessToken, imageUrl, caption }) {
  const createRes = await axios.post(`${BASE_URL}/${igUserId}/media`, {
    image_url: imageUrl,
    caption,
    access_token: accessToken,
  });

  const creationId = createRes.data.id;

  const publishRes = await axios.post(`${BASE_URL}/${igUserId}/media_publish`, {
    creation_id: creationId,
    access_token: accessToken,
  });

  return publishRes.data; // { id: '<instagram_post_id>' }
}

module.exports = {
  getLoginUrl,
  exchangeCodeForToken,
  getLongLivedToken,
  getInstagramBusinessAccount,
  publishToInstagram,
};
