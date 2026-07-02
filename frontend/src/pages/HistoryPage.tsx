import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import ConfirmModal from '../components/ConfirmModal';
import Toast from '../components/Toast';
import { listPosts, updatePost, deletePost, publishPost } from '../api/posts';
import type { Post, PostStatus, ToastData } from '../types';

// P-09 히스토리 (docs/03_화면설계서.md)
export default function HistoryPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [tab, setTab] = useState('');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editCaption, setEditCaption] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const TABS = [
    { key: '', label: t('history.tabs.all') },
    { key: 'posted', label: t('history.tabs.posted') },
    { key: 'scheduled', label: t('history.tabs.scheduled') },
    { key: 'failed', label: t('history.tabs.failed') },
  ];

  const STATUS_LABEL: Record<PostStatus, string> = {
    draft: t('history.status.draft'),
    scheduled: t('history.status.scheduled'),
    posted: t('history.status.posted'),
    failed: t('history.status.failed'),
  };

  const load = (status: string) => {
    setLoading(true);
    listPosts(status ? { status, limit: 100 } : { limit: 100 })
      .then((res) => setPosts(res.data.data.posts))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load(tab);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  const startEdit = (post: Post) => {
    setEditingId(post.id);
    setEditCaption(post.caption || '');
  };

  const saveEdit = async (id: number) => {
    try {
      await updatePost(id, { caption: editCaption });
      setEditingId(null);
      setToast({ type: 'success', message: t('history.toast.captionSaved') });
      load(tab);
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('history.toast.saveFailed') });
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deletePost(deleteTarget);
      setToast({ type: 'success', message: t('history.toast.deleted') });
      load(tab);
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
      load(tab);
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
