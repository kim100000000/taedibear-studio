const cron = require('node-cron');
const { Op } = require('sequelize');
const { Post, ScheduledPost, InstagramAccount } = require('../models');
const metaService = require('./meta.service');

// hashtags는 posts 테이블에 쉼표 구분 문자열로 저장된다 (posts.controller.js와 동일한 변환 규칙).
function hashtagsToArray(hashtagsStr) {
  if (!hashtagsStr) return [];
  return hashtagsStr.split(',').filter(Boolean);
}

// 예약 시간이 지난 pending 건을 찾아 인스타그램에 발행한다.
async function processDueSchedules() {
  const dueSchedules = await ScheduledPost.findAll({
    where: { status: 'pending', scheduledAt: { [Op.lte]: new Date() } },
    include: [{ model: Post }],
  });

  for (const scheduledPost of dueSchedules) {
    const post = scheduledPost.Post;
    if (!post) {
      scheduledPost.status = 'failed';
      await scheduledPost.save();
      continue;
    }

    try {
      const account = await InstagramAccount.findOne({
        where: { id: post.instagramAccountId, userId: post.userId },
      });
      if (!account) {
        throw new Error('연동된 인스타그램 계정을 찾을 수 없어요.');
      }

      const captionWithHashtags = [post.caption, hashtagsToArray(post.hashtags).join(' ')]
        .filter(Boolean)
        .join('\n\n');

      const result = await metaService.publishToInstagram({
        igUserId: account.instagramUserId,
        accessToken: account.accessToken,
        imageUrl: post.imageUrl,
        caption: captionWithHashtags,
      });

      post.status = 'posted';
      post.instagramPostId = result.id;
      post.postedAt = new Date();
      await post.save();

      scheduledPost.status = 'done';
      await scheduledPost.save();

      console.log(`[Cron] scheduled post ${post.id} published as ${result.id}`);
    } catch (err) {
      console.error(`[Cron] scheduled post ${post.id} publish failed:`, err?.response?.data || err.message);

      post.status = 'failed';
      await post.save();

      scheduledPost.status = 'failed';
      scheduledPost.retryCount += 1;
      await scheduledPost.save();
    }
  }
}

// 1분마다 예약 발행 대상을 확인한다.
function startScheduledPostCron() {
  cron.schedule('* * * * *', () => {
    processDueSchedules().catch((err) => {
      console.error('[Cron] processDueSchedules error:', err.message);
    });
  });
  console.log('[Cron] Scheduled post publisher started (every 1 minute).');
}

module.exports = { startScheduledPostCron, processDueSchedules };
