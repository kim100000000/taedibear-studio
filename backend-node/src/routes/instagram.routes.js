const express = require('express');
const router = express.Router();
const { requireAuth } = require('../middleware/auth');
const instagramController = require('../controllers/instagram.controller');

// GET /api/instagram/connect — Meta OAuth 연동 페이지로 리다이렉트
// 브라우저 전체 페이지 이동이라 Authorization 헤더를 못 쓰므로 ?token=<JWT> 쿼리로 인증한다.
router.get('/connect', instagramController.connect);

// GET /api/instagram/callback — Meta OAuth 콜백, 계정 저장 후 리다이렉트
// state로 돌아온 JWT로 사용자를 식별한다 (컨트롤러 내부에서 직접 검증).
router.get('/callback', instagramController.callback);

// GET /api/instagram/accounts — 연동된 계정 목록
router.get('/accounts', requireAuth, instagramController.listAccounts);

// DELETE /api/instagram/accounts/:id — 연동 해제
router.delete('/accounts/:id', requireAuth, instagramController.disconnectAccount);

module.exports = router;
