import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Toast from '../components/Toast';
import { listInstagramAccounts } from '../api/instagram';
import { listComments, replyToComment, skipComment } from '../api/reviews';
import type { ReviewComment } from '../api/reviews';
import type { InstagramAccount, ToastData } from '../types';

// Phase 4-3: 리뷰(댓글) 자동 답글 관리 페이지
export default function ReviewsPage() {
  const { t } = useTranslation();
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [accountId, setAccountId] = useState<number | null>(null);
  const [comments, setComments] = useState<ReviewComment[]>([]);
  const [drafts, setDrafts] = useState<Record<number, string>>({});
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
    listComments(accountId)
      .then((res) => {
        setComments(res.data.data);
        const nextDrafts: Record<number, string> = {};
        res.data.data.forEach((c) => {
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
          <ul className="history-list">
            {comments.map((comment) => (
              <li key={comment.id} className="history-card">
                <div className="history-card-body">
                  <p className="post-caption-preview">
                    <strong>@{comment.username || '?'}</strong> — {comment.comment_text}
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
      </div>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
