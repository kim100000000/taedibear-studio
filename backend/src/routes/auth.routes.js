const express = require('express');
const passport = require('passport');
const { body } = require('express-validator');
const router = express.Router();
const authController = require('../controllers/auth.controller');
const { requireAuth } = require('../middleware/auth');
const validate = require('../middleware/validate');

router.post(
  '/register',
  [
    body('name').trim().isLength({ min: 2 }).withMessage('이름은 2자 이상이어야 해요.'),
    body('email').isEmail().withMessage('올바른 이메일 형식이 아니에요.'),
    body('password').isLength({ min: 8 }).withMessage('비밀번호는 8자 이상이어야 해요.'),
  ],
  validate,
  authController.register
);

router.post(
  '/login',
  [
    body('email').isEmail().withMessage('올바른 이메일 형식이 아니에요.'),
    body('password').notEmpty().withMessage('비밀번호를 입력해주세요.'),
  ],
  validate,
  authController.login
);

router.post('/refresh', authController.refresh);
router.post('/logout', requireAuth, authController.logout);

// GET /api/auth/google — 구글 로그인 페이지로 리다이렉트
router.get('/google', passport.authenticate('google', { scope: ['profile', 'email'], session: false }));

// GET /api/auth/google/callback — 구글 인증 후 JWT 발급, 프론트엔드로 리다이렉트
router.get(
  '/google/callback',
  passport.authenticate('google', {
    session: false,
    failureRedirect: `${process.env.CLIENT_URL || 'http://localhost:5173'}/login?error=google`,
  }),
  authController.googleCallback
);

// GET /api/auth/kakao — 카카오 로그인 페이지로 리다이렉트
router.get('/kakao', passport.authenticate('kakao', { session: false }));

// GET /api/auth/kakao/callback — 카카오 인증 후 JWT 발급, 프론트엔드로 리다이렉트
router.get(
  '/kakao/callback',
  passport.authenticate('kakao', {
    session: false,
    failureRedirect: `${process.env.CLIENT_URL || 'http://localhost:5173'}/login?error=kakao`,
  }),
  authController.kakaoCallback
);

// GET /api/auth/naver — 네이버 로그인 페이지로 리다이렉트 (passport 전략 없이 직접 구현, naver.service.js)
router.get('/naver', authController.naverLogin);

// GET /api/auth/naver/callback — 네이버 인증 후 JWT 발급, 프론트엔드로 리다이렉트
router.get('/naver/callback', authController.naverCallback);

module.exports = router;
