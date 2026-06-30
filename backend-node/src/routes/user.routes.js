const express = require('express');
const router = express.Router();
const { requireAuth } = require('../middleware/auth');
const userController = require('../controllers/user.controller');

// GET /api/users/me
router.get('/me', requireAuth, userController.getMe);

// PUT /api/users/me
router.put('/me', requireAuth, userController.updateMe);

module.exports = router;
