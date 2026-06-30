import { useEffect, useState } from 'react';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import ConfirmModal from '../components/ConfirmModal';
import Toast from '../components/Toast';
import { listPosts, updatePost, deletePost, publishPost } from '../api/posts';
import type { Post, PostStatus, ToastData } from '../types';

const TABS: { key: string; label: string }[] = [
  { key: '', label: '전체' },
  { key: 'posted', label: '완료' },
  { key: 'scheduled', label: '예약중' },
  { key: 'failed', label: '실패' },
];

const STATUS_LABEL: Record<PostStatus, string> = {
  draft: '임시저장',
  scheduled: '예약중',
  posted: '완료',
  failed: '실패',
};

// P-09 히스토리 (docs/03_화면설계서.md)
export default function HistoryPage() {
  const [tab, setTab] = useState('');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editCaption, setEditCaption] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

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
      setToast({ type: 'success', message: '캡션을 수정했어요.' });
      load(tab);
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || '수정에 실패했어요.' });
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deletePost(deleteTarget);
      setToast({ type: 'success', message: '삭제했어요.' });
      load(tab);
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || '삭제에 실패했어요.' });
    } finally {
      setDeleteTarget(null);
    }
  };

  const retry = async (id: number) => {
    setBusyId(id);
    try {
      await publishPost(id);
      setToast({ type: 'success', message: '다시 업로드를 시도했어요.' });
      load(tab);
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || '재시도에 실패했어요.' });
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content history-page">
        <h1>히스토리</h1>

        <div className="history-tabs">
          {TABS.map((t) => (
            <button
              key={t.key}
              type="button"
              className={`history-tab ${tab === t.key ? 'history-tab-active' : ''}`}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {loading ? (
          <Spinner label="불러오는 중..." />
        ) : posts.length === 0 ? (
          <EmptyState message="게시물이 없어요." />
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
                    <p className="post-caption-preview">{post.caption || '(캡션 없음)'}</p>
                  )}
                  <span className={`status-badge status-${post.status}`}>
                    {STATUS_LABEL[post.status]}
                  </span>
                </div>
                <div className="history-card-actions">
                  {editingId === post.id ? (
                    <>
                      <button type="button" className="btn-outline" onClick={() => saveEdit(post.id)}>
                        저장
                      </button>
                      <button type="button" className="btn-outline" onClick={() => setEditingId(null)}>
                        취소
                      </button>
                    </>
                  ) : (
                    <>
                      <button type="button" className="btn-outline" onClick={() => startEdit(post)}>
                        수정
                      </button>
                      {post.status === 'failed' && (
                        <button
                          type="button"
                          className="btn-outline"
                          disabled={busyId === post.id}
                          onClick={() => retry(post.id)}
                        >
                          재시도
                        </button>
                      )}
                      <button
                        type="button"
                        className="btn-danger"
                        onClick={() => setDeleteTarget(post.id)}
                      >
                        삭제
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
        title="게시물 삭제"
        message="이 게시물을 삭제할까요? 삭제하면 되돌릴 수 없어요."
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
