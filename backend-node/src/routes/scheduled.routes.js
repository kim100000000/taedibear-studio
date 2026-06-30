const express = require('express');
const router = express.Router();
const { requireAuth } = require('../middleware/auth');
const scheduledController = require('../controllers/scheduled.controller');

// POST /api/scheduled — 업로드 예약
router.post('/', requireAuth, scheduledController.createSchedule);

// GET /api/scheduled — 예약 목록 조회
router.get('/', requireAuth, scheduledController.listSchedules);

// PUT /api/scheduled/:id — 예약 시간 변경
router.put('/:id', requireAuth, scheduledController.updateSchedule);

// DELETE /api/scheduled/:id — 예약 취소
router.delete('/:id', requireAuth, scheduledController.deleteSchedule);

module.exports = router;
