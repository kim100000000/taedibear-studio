const express = require('express');
const router = express.Router();
const multer = require('multer');
const { body } = require('express-validator');
const upload = require('../middleware/upload');
const { requireAuth } = require('../middleware/auth');
const validate = require('../middleware/validate');
const postsController = require('../controllers/posts.controller');

// 멀터 에러(파일 타입/크기)를 API 명세서 형식의 400 응답으로 변환
function handleUpload(req, res, next) {
  upload.single('image')(req, res, (err) => {
    if (err) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        err.status = 400;
        err.message = '파일 크기는 최대 10MB까지 업로드할 수 있어요.';
      } else if (err.message === 'UNSUPPORTED_FILE_TYPE' || err instanceof multer.MulterError) {
        err.status = 400;
        err.message = '지원하지 않는 파일 형식이에요. JPG, PNG, WEBP만 가능해요.';
      } else {
        err.status = err.status || 400;
      }
      return next(err);
    }
    next();
  });
}

// POST /api/posts/upload
router.post('/upload', requireAuth, handleUpload, postsController.uploadImage);

// POST /api/posts/caption
router.post('/caption', requireAuth, postsController.generateCaption);

// POST /api/posts — 게시물 저장 (draft)
router.post(
  '/',
  requireAuth,
  [
    body('instagram_account_id').notEmpty().withMessage('instagram_account_id는 필수예요.'),
    body('image_url').notEmpty().withMessage('image_url은 필수예요.'),
  ],
  validate,
  postsController.createPost
);

// GET /api/posts — 게시물 목록(히스토리) 조회
router.get('/', requireAuth, postsController.listPosts);

// GET /api/posts/:id — 게시물 상세 조회
router.get('/:id', requireAuth, postsController.getPost);

// PUT /api/posts/:id — 게시물 수정 (posted 상태는 불가)
router.put('/:id', requireAuth, postsController.updatePost);

// DELETE /api/posts/:id — 게시물 삭제
router.delete('/:id', requireAuth, postsController.deletePost);

// POST /api/posts/:id/publish — 즉시 인스타그램 업로드
router.post('/:id/publish', requireAuth, postsController.publishPost);

module.exports = router;
