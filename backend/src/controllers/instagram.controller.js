const jwt = require('jsonwebtoken');
const { success, fail } = require('../utils/response');
const metaService = require('../services/meta.service');
const { InstagramAccount } = require('../models');

// GET /api/instagram/connect
// 브라우저의 전체 페이지 리다이렉트라 Authorization 헤더로 인증할 수 없으므로,
// 프론트엔드가 ?token=<JWT>로 호출하고, 그 JWT를 Meta OAuth의 state 파라미터로 그대로 실어 보낸다.
exports.connect = (req, res) => {
  const { token } = req.query;
  if (!token) {
    return fail(res, 401, '토큰이 필요해요.');
  }

  try {
    jwt.verify(token, process.env.JWT_SECRET);
  } catch (err) {
    return fail(res, 401, '유효하지 않은 토큰이에요.');
  }

  res.redirect(metaService.getLoginUrl(token));
};

// GET /api/instagram/callback
exports.callback = async (req, res) => {
  const clientUrl = process.env.CLIENT_URL || 'http://localhost:5173';
  try {
    const { code, state } = req.query;
    if (!code || !state) {
      return res.redirect(`${clientUrl}/settings?connected=false`);
    }

    const payload = jwt.verify(state, process.env.JWT_SECRET);
    const userId = payload.sub;

    const shortLivedToken = await metaService.exchangeCodeForToken(code);
    const { accessToken, expiresIn } = await metaService.getLongLivedToken(shortLivedToken);
    const { instagramUserId, username } = await metaService.getInstagramBusinessAccount(accessToken);

    const tokenExpiresAt = expiresIn ? new Date(Date.now() + expiresIn * 1000) : null;

    const [account] = await InstagramAccount.findOrCreate({
      where: { userId, instagramUserId },
      defaults: { username, accessToken, tokenExpiresAt },
    });

    account.username = username;
    account.accessToken = accessToken;
    account.tokenExpiresAt = tokenExpiresAt;
    await account.save();

    return res.redirect(`${clientUrl}/settings?connected=true`);
  } catch (err) {
    console.error('[Meta connect error]', err?.response?.data || err.message);
    return res.redirect(`${clientUrl}/settings?connected=false`);
  }
};

// GET /api/instagram/accounts
exports.listAccounts = async (req, res, next) => {
  try {
    const accounts = await InstagramAccount.findAll({ where: { userId: req.user.id } });
    return success(
      res,
      accounts.map((a) => ({
        id: a.id,
        instagram_user_id: a.instagramUserId,
        username: a.username,
        connected_at: a.connectedAt,
      }))
    );
  } catch (err) {
    next(err);
  }
};

// DELETE /api/instagram/accounts/:id
exports.disconnectAccount = async (req, res, next) => {
  try {
    const account = await InstagramAccount.findByPk(req.params.id);
    if (!account) {
      return fail(res, 404, '계정을 찾을 수 없어요.');
    }
    if (account.userId !== req.user.id) {
      return fail(res, 403, '본인 계정만 해제할 수 있어요.');
    }

    await account.destroy();
    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};
