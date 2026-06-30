const { success, fail } = require('../utils/response');
const s3Service = require('../services/s3.service');
const geminiService = require('../services/gemini.service');
const metaService = require('../services/meta.service');
const { Post, ScheduledPost, InstagramAccount } = require('../models');

// hashtags는 DB(TEXT, 쉼표 구분)와 API(JSON 배열) 간 형태가 달라 변환이 필요하다.
function hashtagsToString(hashtags) {
  if (!hashtags) return null;
  return Array.isArray(hashtags) ? hashtags.join(',') : String(hashtags);
}

function hashtagsToArray(hashtagsStr) {
  if (!hashtagsStr) return [];
  return hashtagsStr.split(',').filter(Boolean);
}

// POST /api/posts/upload
exports.uploadImage = async (req, res, next) => {
  try {
    if (!req.file) {
      return fail(res, 400, '이미지 파일이 필요해요.');
    }

    const imageUrl = await s3Service.uploadImage(req.file.buffer, req.file.mimetype);
    return success(res, { image_url: imageUrl });
  } catch (err) {
    next(err);
  }
};

// POST /api/posts/caption
// Gemini(gemini-2.5-flash)로 이미지 기반 캡션 + 해시태그 생성. 무료 한도: 분당 15회.
exports.generateCaption = async (req, res, next) => {
  try {
    const { image_url, business_type, mood } = req.body;

    if (!image_url || !business_type || !mood) {
      return fail(res, 400, 'image_url, business_type, mood는 필수예요.');
    }

    const { caption, hashtags } = await geminiService.generateCaption(image_url, business_type, mood);
    return success(res, { caption, hashtags });
  } catch (err) {
    // 어떤 에러든 일단 서버 로그에 원인을 남긴다 (429로 잘못 분류되는 경우 디버깅용)
    console.error('[Gemini caption error]', err?.status || err?.response?.status, err?.message, err?.errorDetails || err?.response?.data);

    const status = err?.status || err?.response?.status;
    if (status === 429) {
      return fail(res, 429, '잠시 후 다시 시도해주세요.');
    }
    next(err);
  }
};

// POST /api/posts
// 게시물을 draft 상태로 저장한다.
exports.createPost = async (req, res, next) => {
  try {
    const { instagram_account_id, image_url, caption, hashtags } = req.body;

    if (!instagram_account_id || !image_url) {
      return fail(res, 400, 'instagram_account_id, image_url은 필수예요.');
    }

    const post = await Post.create({
      userId: req.user.id,
      instagramAccountId: instagram_account_id,
      imageUrl: image_url,
      caption: caption || null,
      hashtags: hashtagsToString(hashtags),
      status: 'draft',
    });

    return success(
      res,
      { id: post.id, status: post.status, created_at: post.createdAt },
      201
    );
  } catch (err) {
    next(err);
  }
};

// GET /api/posts
// 게시물 목록(히스토리)을 조회한다. status/page/limit 필터 지원.
exports.listPosts = async (req, res, next) => {
  try {
    const { status } = req.query;
    const page = parseInt(req.query.page, 10) || 1;
    const limit = parseInt(req.query.limit, 10) || 20;

    const where = { userId: req.user.id };
    if (status) where.status = status;

    const { count, rows } = await Post.findAndCountAll({
      where,
      include: [{ model: ScheduledPost, attributes: ['scheduledAt'] }],
      order: [['createdAt', 'DESC']],
      offset: (page - 1) * limit,
      limit,
    });

    const posts = rows.map((p) => ({
      id: p.id,
      image_url: p.imageUrl,
      caption: p.caption,
      status: p.status,
      posted_at: p.postedAt,
      scheduled_at: p.ScheduledPost ? p.ScheduledPost.scheduledAt : null,
    }));

    return success(res, { posts, total: count, page, limit });
  } catch (err) {
    next(err);
  }
};

// GET /api/posts/:id
exports.getPost = async (req, res, next) => {
  try {
    const post = await Post.findOne({ where: { id: req.params.id, userId: req.user.id } });
    if (!post) {
      return fail(res, 404, '게시물을 찾을 수 없어요.');
    }

    return success(res, {
      id: post.id,
      image_url: post.imageUrl,
      caption: post.caption,
      hashtags: hashtagsToArray(post.hashtags),
      status: post.status,
      instagram_post_id: post.instagramPostId,
      posted_at: post.postedAt,
    });
  } catch (err) {
    next(err);
  }
};

// PUT /api/posts/:id
// posted 상태인 게시물은 수정할 수 없다.
exports.updatePost = async (req, res, next) => {
  try {
    const post = await Post.findOne({ where: { id: req.params.id, userId: req.user.id } });
    if (!post) {
      return fail(res, 404, '게시물을 찾을 수 없어요.');
    }

    if (post.status === 'posted') {
      return fail(res, 400, '업로드 완료된 게시물은 수정할 수 없어요.');
    }

    const { caption, hashtags } = req.body;
    if (caption !== undefined) post.caption = caption;
    if (hashtags !== undefined) post.hashtags = hashtagsToString(hashtags);
    await post.save();

    return success(res, { id: post.id });
  } catch (err) {
    next(err);
  }
};

// DELETE /api/posts/:id
exports.deletePost = async (req, res, next) => {
  try {
    const post = await Post.findOne({ where: { id: req.params.id, userId: req.user.id } });
    if (!post) {
      return fail(res, 404, '게시물을 찾을 수 없어요.');
    }

    await post.destroy();
    return res.json({ success: true });
  } catch (err) {
    next(err);
  }
};

// POST /api/posts/:id/publish
// 게시물을 즉시 인스타그램에 업로드한다.
exports.publishPost = async (req, res, next) => {
  try {
    const post = await Post.findOne({ where: { id: req.params.id, userId: req.user.id } });
    if (!post) {
      return fail(res, 404, '게시물을 찾을 수 없어요.');
    }

    const account = await InstagramAccount.findOne({
      where: { id: post.instagramAccountId, userId: req.user.id },
    });
    if (!account) {
      return fail(res, 404, '연동된 인스타그램 계정을 찾을 수 없어요.');
    }

    const captionWithHashtags = [post.caption, hashtagsToArray(post.hashtags).join(' ')]
      .filter(Boolean)
      .join('\n\n');

    try {
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

      return success(res, { instagram_post_id: post.instagramPostId, posted_at: post.postedAt });
    } catch (publishErr) {
      console.error('[Instagram publish error]', publishErr?.response?.data || publishErr.message);
      post.status = 'failed';
      await post.save();
      return fail(res, 500, '인스타그램 업로드에 실패했어요. 다시 시도해주세요.');
    }
  } catch (err) {
    next(err);
  }
};
