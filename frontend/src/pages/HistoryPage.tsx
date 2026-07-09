import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import ConfirmModal from '../components/ConfirmModal';
import Toast from '../components/Toast';
import { listPosts, updatePost, deletePost, publishPost, getPostInsights } from '../api/posts';
import type { PostInsights, ListPostsParams } from '../api/posts';
import { listInstagramAccounts } from '../api/instagram';
import type { Post, PostStatus, ToastData, InstagramAccount } from '../types';

// P-09 히스토리 (docs/03_화면설계서.md)
export default function HistoryPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  // 개선백로그 🟠: 캡션 화면 "임시 저장" 후 draft 탭으로 바로 진입할 수 있게 초기 탭 지원
  const { state: navState } = useLocation() as { state: { initialTab?: string } | null };
  const [tab, setTab] = useState(navState?.initialTab ?? '');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editCaption, setEditCaption] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  // Phase 4-1: 계정별 히스토리 필터링
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [accountFilter, setAccountFilter] = useState<number | ''>('');
  // Phase 4-2: 게시물 인사이트 (post.id -> 조회 결과, null이면 조회 실패)
  const [insightsMap, setInsightsMap] = useState<Record<number, PostInsights | null>>({});
  const [insightsLoadingId, setInsightsLoadingId] = useState<number | null>(null);

  const TABS = [
    { key: '', label: t('history.tabs.all') },
    { key: 'posted', label: t('history.tabs.posted') },
    { key: 'scheduled', label: t('history.tabs.scheduled') },
    { key: 'draft', label: t('history.tabs.draft') }, // 개선백로그 🟠: 임시 저장 보관함
    { key: 'failed', label: t('history.tabs.failed') },
  ];

  const STATUS_LABEL: Record<PostStatus, string> = {
    draft: t('history.status.draft'),
    scheduled: t('history.status.scheduled'),
    posted: t('history.status.posted'),
    failed: t('history.status.failed'),
  };

  const load = (status: string, accountId: number | '') => {
    setLoading(true);
    const params: ListPostsParams = { limit: 100 };
    if (status) params.status = status;
    if (accountId !== '') params.instagram_account_id = accountId;
    listPosts(params)
      .then((res) => setPosts(res.data.data.posts))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    listInstagramAccounts().then((res) => setAccounts(res.data.data));
  }, []);

  useEffect(() => {
    load(tab, accountFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, accountFilter]);

  const viewInsights = async (postId: number) => {
    setInsightsLoadingId(postId);
    try {
      const { data } = await getPostInsights(postId);
      setInsightsMap((prev) => ({ ...prev, [postId]: data.data }));
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('history.toast.insightsFailed') });
      setInsightsMap((prev) => ({ ...prev, [postId]: null }));
    } finally {
      setInsightsLoadingId(null);
    }
  };

  const startEdit = (post: Post) => {
    setEditingId(post.id);
    setEditCaption(post.caption || '');
  };

  const saveEdit = async (id: number) => {
    try {
      await updatePost(id, { caption: editCaption });
      setEditingId(null);
      setToast({ type: 'success', message: t('history.toast.captionSaved') });
      load(tab, accountFilter);
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('history.toast.saveFailed') });
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deletePost(deleteTarget);
      setToast({ type: 'success', message: t('history.toast.deleted') });
      load(tab, accountFilter);
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('history.toast.deleteFailed') });
    } finally {
      setDeleteTarget(null);
    }
  };

  const retry = async (id: number) => {
    setBusyId(id);
    try {
      await publishPost(id);
      setToast({ type: 'success', message: t('history.toast.retried') });
      load(tab, accountFilter);
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('history.toast.retryFailed') });
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content history-page">
        <h1>{t('history.title')}</h1>

        <div className="history-tabs">
          {TABS.map((tabItem) => (
            <button
              key={tabItem.key}
              type="button"
              className={`history-tab ${tab === tabItem.key ? 'history-tab-active' : ''}`}
              onClick={() => setTab(tabItem.key)}
            >
              {tabItem.label}
            </button>
          ))}
        </div>

        {/* Phase 4-1: 계정별 히스토리 필터링 */}
        {accounts.length > 1 && (
          <div className="form-field">
            <select
              value={accountFilter}
              onChange={(e) => setAccountFilter(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <option value="">{t('history.accountFilterAll')}</option>
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  @{acc.username}
                </option>
              ))}
            </select>
          </div>
        )}

        {loading ? (
          <Spinner label={t('common.loading')} />
        ) : posts.length === 0 ? (
          <EmptyState message={t('history.empty')} />
        ) : (
          <ul className="history-list">
            {posts.map((post) => (
              <li key={post.id} className="history-card">
                <img src={post.image_url} alt="" />
                <div className="history-card-body">
                  {editingId === post.id ? (
                    <textarea
                      rows={3}
                      value={editCaption}
                      onChange={(e) => setEditCaption(e.target.value)}
                    />
                  ) : (
                    <p className="post-caption-preview">{post.caption || t('history.noCaption')}</p>
                  )}
                  <span className={`status-badge status-${post.status}`}>
                    {STATUS_LABEL[post.status]}
                  </span>
                  {/* Phase 4-2: 게시물 인사이트 */}
                  {post.status === 'posted' && insightsMap[post.id] && (
                    <ul className="post-insights">
                      <li>{t('history.insightsModal.engagement')}: {insightsMap[post.id]!.engagement}</li>
                      <li>{t('history.insightsModal.impressions')}: {insightsMap[post.id]!.impressions}</li>
                      <li>{t('history.insightsModal.reach')}: {insightsMap[post.id]!.reach}</li>
                      <li>{t('history.insightsModal.likeCount')}: {insightsMap[post.id]!.like_count}</li>
                      <li>{t('history.insightsModal.commentsCount')}: {insightsMap[post.id]!.comments_count}</li>
                    </ul>
                  )}
                </div>
                <div className="history-card-actions">
                  {editingId === post.id ? (
                    <>
                      <button type="button" className="btn-outline" onClick={() => saveEdit(post.id)}>
                        {t('common.save')}
                      </button>
                      <button type="button" className="btn-outline" onClick={() => setEditingId(null)}>
                        {t('common.cancel')}
                      </button>
                    </>
                  ) : (
                    <>
                      <button type="button" className="btn-outline" onClick={() => startEdit(post)}>
                        {t('common.edit')}
                      </button>
                      {post.status === 'failed' && (
                        <button
                          type="button"
                          className="btn-outline"
                          disabled={busyId === post.id}
                          onClick={() => retry(post.id)}
                        >
                          {t('common.retry')}
                        </button>
                      )}
                      {/* 개선백로그 🟠: 임시 저장(draft) 게시물을 보관함에서 바로 발행 */}
                      {post.status === 'draft' && (
                        <button
                          type="button"
                          className="btn-primary"
                          disabled={busyId === post.id}
                          onClick={() => retry(post.id)}
                        >
                          {t('history.publishDraft')}
                        </button>
                      )}
                      {/* Phase 2-4: 이 스타일로 다시 생성 */}
                      {post.status === 'posted' && (
                        <button
                          type="button"
                          className="btn-outline"
                          onClick={() =>
                            navigate('/upload/caption', {
                              state: {
                                imageUrl: post.image_url,
                                instagramAccountId: post.instagram_account_id,
                              },
                            })
                          }
                        >
                          {t('history.regenerate')}
                        </button>
                      )}
                      {/* Phase 4-2: 게시물 인사이트 보기 */}
                      {post.status === 'posted' && !insightsMap[post.id] && (
                        <button
                          type="button"
                          className="btn-outline"
                          disabled={insightsLoadingId === post.id}
                          onClick={() => viewInsights(post.id)}
                        >
                          {t('history.insights')}
                        </button>
                      )}
                      <button
                        type="button"
                        className="btn-danger"
                        onClick={() => setDeleteTarget(post.id)}
                      >
                        {t('common.delete')}
                      </button>
                    </>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <ConfirmModal
        open={!!deleteTarget}
        title={t('history.modal.title')}
        message={t('history.modal.message')}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
