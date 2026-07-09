import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Toast from '../components/Toast';
import { listInstagramAccounts } from '../api/instagram';
import { listComments, replyToComment, skipComment } from '../api/reviews';
import { listPosts } from '../api/posts';
import type { ReviewComment } from '../api/reviews';
import type { InstagramAccount, Post, ToastData } from '../types';

// Phase 4-3: 리뷰(댓글) 자동 답글 관리 페이지
// 개선백로그 🟡: 게시글(media_id) 단위 그룹핑 — 썸네일 헤더 + 미답변 뱃지 + 접기/펼치기
export default function ReviewsPage() {
  const { t } = useTranslation();
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [accountId, setAccountId] = useState<number | null>(null);
  const [comments, setComments] = useState<ReviewComment[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [drafts, setDrafts] = useState<Record<number, string>>({});
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [loadingComments, setLoadingComments] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);

  useEffect(() => {
    listInstagramAccounts()
      .then((res) => {
        setAccounts(res.data.data);
        if (res.data.data.length > 0) setAccountId(res.data.data[0].id);
      })
      .finally(() => setLoadingAccounts(false));
  }, []);

  useEffect(() => {
    if (accountId == null) return;
    setLoadingComments(true);
    // 게시물 정보(썸네일/캡션)는 그룹 헤더 표시용 — 실패해도 댓글 목록은 정상 동작
    Promise.all([
      listComments(accountId),
      listPosts({ status: 'posted', limit: 100 }).catch(() => null),
    ])
      .then(([commentsRes, postsRes]) => {
        setComments(commentsRes.data.data);
        if (postsRes) setPosts(postsRes.data.data.posts);
        const nextDrafts: Record<number, string> = {};
        commentsRes.data.data.forEach((c) => {
          nextDrafts[c.id] = c.suggested_reply || '';
        });
        setDrafts(nextDrafts);
      })
      .catch((err: any) => {
        const msg = err.isNetworkError ? t('error.network') : err.response?.data?.error || t('reviews.toast.loadFailed');
        setToast({ type: 'error', message: msg });
      })
      .finally(() => setLoadingComments(false));
  }, [accountId, t]);

  // media_id → 게시물 매핑 (instagram_post_id 기준). 서비스 밖에서 올린 게시물은 매칭 안 될 수 있음
  const postByMediaId = useMemo(() => {
    const map: Record<string, Post> = {};
    posts.forEach((p) => {
      if (p.instagram_post_id) map[p.instagram_post_id] = p;
    });
    return map;
  }, [posts]);

  // 게시글 단위 그룹 (댓글 도착 순서 유지)
  const groups = useMemo(() => {
    const ordered: { mediaId: string; items: ReviewComment[] }[] = [];
    const index: Record<string, number> = {};
    comments.forEach((c) => {
      if (index[c.media_id] === undefined) {
        index[c.media_id] = ordered.length;
        ordered.push({ mediaId: c.media_id, items: [] });
      }
      ordered[index[c.media_id]].items.push(c);
    });
    return ordered;
  }, [comments]);

  const send = async (comment: ReviewComment) => {
    const message = drafts[comment.id]?.trim();
    if (!message) return;
    setBusyId(comment.id);
    try {
      await replyToComment(comment.id, message);
      setComments((prev) => prev.map((c) => (c.id === comment.id ? { ...c, status: 'replied' } : c)));
      setToast({ type: 'success', message: t('reviews.toast.replySuccess') });
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('reviews.toast.replyFailed') });
    } finally {
      setBusyId(null);
    }
  };

  const skip = async (comment: ReviewComment) => {
    setBusyId(comment.id);
    try {
      await skipComment(comment.id);
      setComments((prev) => prev.map((c) => (c.id === comment.id ? { ...c, status: 'skipped' } : c)));
      setToast({ type: 'success', message: t('reviews.toast.skipSuccess') });
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('reviews.toast.skipFailed') });
    } finally {
      setBusyId(null);
    }
  };

  if (loadingAccounts) return <Spinner label={t('common.loading')} />;

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content reviews-page">
        <h1>{t('reviews.title')}</h1>

        {accounts.length > 0 && (
          <div className="form-field">
            <select value={accountId ?? ''} onChange={(e) => setAccountId(Number(e.target.value))}>
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  @{acc.username}
                </option>
              ))}
            </select>
          </div>
        )}

        {loadingComments ? (
          <Spinner label={t('common.loading')} />
        ) : comments.length === 0 ? (
          <EmptyState message={t('reviews.empty')} />
        ) : (
          groups.map((group, groupIndex) => {
            const post = postByMediaId[group.mediaId];
            const pendingCount = group.items.filter((c) => c.status === 'pending').length;
            const isCollapsed = collapsed[group.mediaId] ?? false;

            return (
              <section key={group.mediaId} className="review-group">
                <button
                  type="button"
                  className="review-group-header"
                  onClick={() =>
                    setCollapsed((prev) => ({ ...prev, [group.mediaId]: !isCollapsed }))
                  }
                >
                  {post?.image_url ? (
                    <img src={post.image_url} alt="" className="review-group-thumb" />
                  ) : (
                    <span className="review-group-thumb review-group-thumb-fallback">📷</span>
                  )}
                  <span className="review-group-title">
                    {post?.caption
                      ? post.caption.slice(0, 40)
                      : t('reviews.group.unknownPost', { n: groupIndex + 1 })}
                  </span>
                  <span className="review-group-meta">
                    {pendingCount > 0 && (
                      <span className="review-group-badge">
                        {t('reviews.group.pendingCount', { count: pendingCount })}
                      </span>
                    )}
                    <span className="review-group-count">
                      {t('reviews.group.commentCount', { count: group.items.length })}
                    </span>
                    <span aria-hidden="true">{isCollapsed ? '▸' : '▾'}</span>
                  </span>
                </button>

                {!isCollapsed && (
                  <ul className="history-list">
                    {group.items.map((comment) => (
                      <li key={comment.id} className="history-card">
                        <div className="history-card-body">
                          <p className="post-caption-preview">
                            {/* 개선백로그 🔴: Meta가 username을 안 주는 경우(비공개 계정 등) "@?" 대신 안내 문구 */}
                            <strong>{comment.username ? `@${comment.username}` : t('reviews.unknownUser')}</strong>
                            {' — '}{comment.comment_text}
                          </p>
                          {comment.status === 'pending' ? (
                            <>
                              <label className="muted">{t('reviews.suggestedReply')}</label>
                              <textarea
                                rows={2}
                                value={drafts[comment.id] ?? ''}
                                onChange={(e) => setDrafts((prev) => ({ ...prev, [comment.id]: e.target.value }))}
                              />
                            </>
                          ) : (
                            <span className={`status-badge status-${comment.status}`}>
                              {comment.status === 'replied' ? t('reviews.replied') : t('reviews.skipped')}
                            </span>
                          )}
                        </div>
                        {comment.status === 'pending' && (
                          <div className="history-card-actions">
                            <button
                              type="button"
                              className="btn-primary"
                              disabled={busyId === comment.id || !drafts[comment.id]?.trim()}
                              onClick={() => send(comment)}
                            >
                              {t('reviews.send')}
                            </button>
                            <button
                              type="button"
                              className="btn-outline"
                              disabled={busyId === comment.id}
                              onClick={() => skip(comment)}
                            >
                              {t('reviews.skip')}
                            </button>
                          </div>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            );
          })
        )}
      </div>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
