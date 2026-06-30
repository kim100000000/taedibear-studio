const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const passport = require('passport');
const User = require('../models/User');
const naverService = require('../services/naver.service');
const { success, fail } = require('../utils/response');

function signToken(user) {
  return jwt.sign({ sub: user.id, email: user.email }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || '7d',
  });
}

function toUserDto(user) {
  return { id: user.id, name: user.name, email: user.email, plan: user.plan };
}

// POST /api/auth/register
exports.register = async (req, res, next) => {
  try {
    const { name, email, password } = req.body;

    const existing = await User.findOne({ where: { email } });
    if (existing) {
      return fail(res, 400, '이미 사용 중인 이메일이에요.');
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({ name, email, passwordHash });

    const token = signToken(user);
    return success(res, { token, user: toUserDto(user) }, 201);
  } catch (err) {
    next(err);
  }
};

// POST /api/auth/login
exports.login = (req, res, next) => {
  passport.authenticate('local', { session: false }, (err, user, info) => {
    if (err) return next(err);
    if (!user) {
      return fail(res, 401, info?.message || '이메일 또는 비밀번호가 올바르지 않아요.');
    }

    const token = signToken(user);
    return success(res, { token, user: toUserDto(user) });
  })(req, res, next);
};

// POST /api/auth/refresh
// 만료된 토큰도 허용해서 새 토큰을 발급한다 (서명만 검증, 만료는 무시).
exports.refresh = async (req, res) => {
  const authHeader = req.headers.authorization || '';
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;

  if (!token) {
    return fail(res, 401, '토큰이 필요해요.');
  }

  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET, { ignoreExpiration: true });
    const user = await User.findByPk(payload.sub);
    if (!user) return fail(res, 401, '유효하지 않은 토큰이에요.');

    const newToken = signToken(user);
    return success(res, { token: newToken });
  } catch (err) {
    return fail(res, 401, '유효하지 않은 토큰이에요.');
  }
};

// POST /api/auth/logout
// 서버는 JWT를 저장하지 않으므로(stateless) 클라이언트에서 토큰을 삭제하도록 안내만 한다.
exports.logout = (req, res) => {
  res.json({ success: true });
};

// GET /api/auth/google/callback
// passport.authenticate('google', ...) 미들웨어가 먼저 실행되어 req.user에 사용자가 채워진 뒤 호출된다.
// 성공 시 JWT를 발급해 프론트엔드로 리다이렉트한다 (docs/05_API명세서.md: /auth?token=<JWT>).
exports.googleCallback = (req, res) => {
  const token = signToken(req.user);
  const clientUrl = process.env.CLIENT_URL || 'http://localhost:5173';
  res.redirect(`${clientUrl}/auth?token=${token}`);
};

// GET /api/auth/kakao/callback
// 구글과 동일하게 JWT를 발급해 프론트엔드로 리다이렉트한다 (/auth?token=<JWT>).
exports.kakaoCallback = (req, res) => {
  const token = signToken(req.user);
  const clientUrl = process.env.CLIENT_URL || 'http://localhost:5173';
  res.redirect(`${clientUrl}/auth?token=${token}`);
};

// GET /api/auth/naver
// 네이버는 passport 전략이 없어 직접 OAuth URL을 만들어 리다이렉트한다.
// state는 CSRF 방지용 임시 토큰으로, 콜백에서 그대로 돌려받는다(별도 세션 저장 없이 단순 통과 방식).
exports.naverLogin = (req, res) => {
  const state = Math.random().toString(36).slice(2);
  res.redirect(naverService.getLoginUrl(state));
};

// GET /api/auth/naver/callback
exports.naverCallback = async (req, res) => {
  const clientUrl = process.env.CLIENT_URL || 'http://localhost:5173';
  try {
    const { code, state } = req.query;
    if (!code) {
      return res.redirect(`${clientUrl}/login?error=naver`);
    }

    const accessToken = await naverService.exchangeCodeForToken(code, state);
    const { naverId, email: naverEmail, name: naverName } = await naverService.getProfile(accessToken);

    // 네이버 앱이 이메일 제공 동의 항목을 받지 못한 경우를 대비한 fallback (구글/카카오와 동일한 방식)
    const email = naverEmail || `naver_${naverId}@naver.taedibear.local`;
    const name = naverName || 'Taedibear User';

    // 1) 이미 네이버 ID로 연결된 계정이 있으면 그대로 로그인
    let user = await User.findOne({ where: { naverId } });

    // 2) 실제 이메일을 받았고, 같은 이메일로 가입된 계정이 있으면 네이버 ID만 연결
    if (!user && naverEmail) {
      user = await User.findOne({ where: { email } });
      if (user) {
        user.naverId = naverId;
        await user.save();
      }
    }

    // 3) 신규 사용자는 자동으로 계정 생성, 비밀번호 없는 소셜 전용 계정
    if (!user) {
      user = await User.create({ name, email, naverId, passwordHash: null });
    }

    const token = signToken(user);
    return res.redirect(`${clientUrl}/auth?token=${token}`);
  } catch (err) {
    console.error('[Naver login error]', err?.response?.data || err.message);
    return res.redirect(`${clientUrl}/login?error=naver`);
  }
};
