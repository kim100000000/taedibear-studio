const { success, fail } = require('../utils/response');
const { Post, ScheduledPost } = require('../models');

const MIN_LEAD_MS = 10 * 60 * 1000; // docs/05_API명세서.md: 현재 시간 + 10분 이후여야 함

// POST /api/scheduled
exports.createSchedule = async (req, res, next) => {
  try {
    const { post_id, scheduled_at } = req.body;

    if (!post_id || !scheduled_at) {
      return fail(res, 400, 'post_id, scheduled_at은 필수예요.');
    }

    const scheduledAt = new Date(scheduled_at);
    if (Number.isNaN(scheduledAt.getTime())) {
      return fail(res, 400, 'scheduled_at 형식이 올바르지 않아요.');
    }
    if (scheduledAt.getTime() < Date.now() + MIN_LEAD_MS) {
      return fail(res, 400, '예약 시간은 현재 시간으로부터 10분 이후여야 해요.');
    }

    const post = await Post.findOne({ where: { id: post_id, userId: req.user.id } });
    if (!post) {
      return fail(res, 404, '게시물을 찾을 수 없어요.');
    }
    if (post.status === 'posted') {
      return fail(res, 400, '업로드 완료된 게시물은 예약할 수 없어요.');
    }

    const scheduledPost = await ScheduledPost.create({
      postId: post.id,
      scheduledAt,
      status: 'pending',
    });

    post.status = 'scheduled';
    await post.save();

    return success(
      res,
      {
        id: scheduledPost.id,
        post_id: post.id,
        scheduled_at: scheduledPost.scheduledAt,
        status: scheduledPost.status,
      },
      201
    );
  } catch (err) {
    next(err);
  }
};

// GET /api/scheduled
exports.listSchedules = async (req, res, next) => {
  try {
    const schedules = await ScheduledPost.findAll({
      include: [
        {
          model: Post,
          where: { userId: req.user.id },
          attributes: ['imageUrl', 'caption'],
        },
      ],
      order: [['scheduledAt', 'ASC']],
    });

    return success(
      res,
      schedules.map((s) => ({
        id: s.id,
        post_id: s.postId,
        scheduled_at: s.scheduledAt,
        status: s.status,
        post: {
          image_url: s.Post.imageUrl,
          caption: s.Post.caption,
        },
      }))
    );
  } catch (err) {
    next(err);
  }
};

// PUT /api/scheduled/:id
exports.updateSchedule = async (req, res, next) => {
  try {
    const { scheduled_at } = req.body;
    if (!scheduled_at) {
      return fail(res, 400, 'scheduled_at은 필수예요.');
    }

    const scheduledAt = new Date(scheduled_at);
    if (Number.isNaN(scheduledAt.getTime())) {
      return fail(res, 400, 'scheduled_at 형식이 올바르지 않아요.');
    }
    if (scheduledAt.getTime() < Date.now() + MIN_LEAD_MS) {
      return fail(res, 400, '예약 시간은 현재 시간으로부터 10분 이후여야 해요.');
    }

    const scheduledPost = await ScheduledPost.findOne({
      where: { id: req.params.id },
      include: [{ model: Post, where: { userId: req.user.id } }],
    });
    if (!scheduledPost) {
      return fail(res, 404, '예약을 찾을 수 없어요.');
    }
    if (scheduledPost.status !== 'pending') {
      return fail(res, 400, '대기 중인 예약만 수정할 수 있어요.');
    }

    scheduledPost.scheduledAt = scheduledAt;
    await scheduledPost.save();

    return success(res, { id: scheduledPost.id, scheduled_at: scheduledPost.scheduledAt });
  } catch (err) {
    next(err);
  }
};

// DELETE /api/scheduled/:id
exports.deleteSchedule = async (req, res, next) => {
  try {
    const scheduledPost = await ScheduledPost.findOne({
      where: { id: req.params.id },
      include: [{ model: Post, where: { userId: req.user.id } }],
    });
    if (!scheduledPost) {
      return fail(res, 404, '예약을 찾을 수 없어요.');
    }

    const post = scheduledPost.Post;
    await scheduledPost.destroy();

    if (post && post.status === 'scheduled') {
      post.status = 'draft';
      await post.save();
    }

    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};
