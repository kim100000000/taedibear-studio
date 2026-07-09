import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Toast from '../components/Toast';
import { useAuth } from '../context/AuthContext';
import {
  getAdminSummary,
  getAdminUsers,
  getAdminPayments,
} from '../api/admin';
import { listNotices, createNotice, deleteNotice } from '../api/notices';
import type { Notice } from '../api/notices';
import type { AdminSummary, AdminUserItem, AdminPaymentItem, AdminPage } from '../api/admin';
import type { ToastData } from '../types';

// P-19 관리자 대시보드 (/admin) — Phase 5-1
// ADMIN_EMAIL 계정만 접근 가능. 백엔드가 403을 반환하면 대시보드로 돌려보낸다.

const fmt = (n: number) => n.toLocaleString();
const fmtDate = (iso: string | null) => (iso ? iso.slice(0, 10) : '-');

export default function AdminPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [summary, setSummary] = useState<AdminSummary | null>(null);
  const [users, setUsers] = useState<AdminPage<AdminUserItem> | null>(null);
  const [payments, setPayments] = useState<AdminPage<AdminPaymentItem> | null>(null);
  const [userPage, setUserPage] = useState(0);
  const [paymentPage, setPaymentPage] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastData | null>(null);
  // 개선백로그 🟡: 공지사항 관리
  const [notices, setNotices] = useState<Notice[]>([]);
  const [noticeTitle, setNoticeTitle] = useState('');
  const [noticeContent, setNoticeContent] = useState('');
  const [noticeSubmitting, setNoticeSubmitting] = useState(false);

  const handleError = useCallback(
    (err: any) => {
      if (err.response?.status === 403) {
        navigate('/dashboard', { replace: true });
        return;
      }
      const msg = err.isNetworkError
        ? t('error.network')
        : err.response?.data?.error || t('error.loadFailed');
      setToast({ type: 'error', message: msg });
    },
    [navigate, t],
  );

  // 프론트 가드는 UX용일 뿐 — 실제 차단은 백엔드 ROLE_ADMIN이 담당
  useEffect(() => {
    if (user && !user.is_admin) navigate('/dashboard', { replace: true });
  }, [user, navigate]);

  useEffect(() => {
    setLoading(true);
    getAdminSummary()
      .then((res) => setSummary(res.data.data))
      .catch(handleError)
      .finally(() => setLoading(false));
  }, [handleError]);

  useEffect(() => {
    getAdminUsers(userPage, search || undefined)
      .then((res) => setUsers(res.data.data))
      .catch(handleError);
  }, [userPage, search, handleError]);

  useEffect(() => {
    getAdminPayments(paymentPage)
      .then((res) => setPayments(res.data.data))
      .catch(handleError);
  }, [paymentPage, handleError]);

  useEffect(() => {
    listNotices()
      .then((res) => setNotices(res.data.data))
      .catch(handleError);
  }, [handleError]);

  const submitNotice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!noticeTitle.trim() || !noticeContent.trim()) return;
    setNoticeSubmitting(true);
    try {
      const { data } = await createNotice(noticeTitle.trim(), noticeContent.trim());
      setNotices((prev) => [data.data, ...prev]);
      setNoticeTitle('');
      setNoticeContent('');
      setToast({ type: 'success', message: t('admin.notices.created') });
    } catch (err: any) {
      handleError(err);
    } finally {
      setNoticeSubmitting(false);
    }
  };

  const removeNotice = async (id: number) => {
    try {
      await deleteNotice(id);
      setNotices((prev) => prev.filter((n) => n.id !== id));
    } catch (err: any) {
      handleError(err);
    }
  };

  const submitSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setUserPage(0);
    setSearch(searchInput.trim());
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content admin-page">
        <h1>{t('admin.title')}</h1>

        {loading && <Spinner />}

        {!loading && summary && (
          <>
            {/* 요약 지표 */}
            <section className="analytics-section analytics-section-full">
              <h2 className="analytics-section-title">{t('admin.summaryTitle')}</h2>
              <div className="analytics-stat-cards">
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{fmt(summary.users.total)}</span>
                  <span className="analytics-stat-label">{t('admin.totalUsers')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">+{fmt(summary.users.new_7d)}</span>
                  <span className="analytics-stat-label">{t('admin.newUsers7d')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">
                    {fmt(summary.users.free)} / {fmt(summary.users.pro)}
                  </span>
                  <span className="analytics-stat-label">{t('admin.planSplit')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{fmt(summary.posts.total)}</span>
                  <span className="analytics-stat-label">{t('admin.totalPosts')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{fmt(summary.schedules.pending)}</span>
                  <span className="analytics-stat-label">{t('admin.pendingSchedules')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{fmt(summary.schedules.failed)}</span>
                  <span className="analytics-stat-label">{t('admin.failedSchedules')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">
                    ₩{fmt(summary.revenue.month_revenue)}
                  </span>
                  <span className="analytics-stat-label">{t('admin.monthRevenue')}</span>
                </div>
                <div className="analytics-stat-card">
                  <span className="analytics-stat-value">{fmt(summary.revenue.active_pro)}</span>
                  <span className="analytics-stat-label">{t('admin.activePro')}</span>
                </div>
              </div>
            </section>

            {/* 유저 목록 */}
            <section className="analytics-section analytics-section-full">
              <h2 className="analytics-section-title">{t('admin.usersTitle')}</h2>
              <form className="admin-search" onSubmit={submitSearch}>
                <input
                  type="text"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder={t('admin.searchPlaceholder')}
                />
                <button type="submit" className="btn-outline">
                  {t('admin.search')}
                </button>
              </form>
              {users && users.items.length === 0 ? (
                <EmptyState message={t('admin.noUsers')} />
              ) : (
                <div className="admin-table-wrap">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>{t('admin.colName')}</th>
                        <th>{t('admin.colEmail')}</th>
                        <th>{t('admin.colPlan')}</th>
                        <th>{t('admin.colCredits')}</th>
                        <th>{t('admin.colPosts')}</th>
                        <th>{t('admin.colJoined')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users?.items.map((u) => (
                        <tr key={u.id}>
                          <td>{u.id}</td>
                          <td>{u.name}</td>
                          <td>{u.email}</td>
                          <td>
                            <span className={`status-badge ${u.plan === 'pro' ? 'status-posted' : 'status-draft'}`}>
                              {u.plan}
                            </span>
                          </td>
                          <td>{u.credits}</td>
                          <td>{fmt(u.post_count)}</td>
                          <td>{fmtDate(u.created_at)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {users && users.total_pages > 1 && (
                <div className="admin-pagination">
                  <button
                    type="button"
                    className="btn-outline"
                    disabled={userPage === 0}
                    onClick={() => setUserPage((p) => p - 1)}
                  >
                    {t('admin.prev')}
                  </button>
                  <span>
                    {userPage + 1} / {users.total_pages}
                  </span>
                  <button
                    type="button"
                    className="btn-outline"
                    disabled={userPage >= users.total_pages - 1}
                    onClick={() => setUserPage((p) => p + 1)}
                  >
                    {t('admin.next')}
                  </button>
                </div>
              )}
            </section>

            {/* 결제 내역 */}
            <section className="analytics-section analytics-section-full">
              <h2 className="analytics-section-title">{t('admin.paymentsTitle')}</h2>
              {payments && payments.items.length === 0 ? (
                <EmptyState message={t('admin.noPayments')} />
              ) : (
                <div className="admin-table-wrap">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>{t('admin.colEmail')}</th>
                        <th>{t('admin.colAmount')}</th>
                        <th>{t('admin.colStatus')}</th>
                        <th>{t('admin.colValidUntil')}</th>
                        <th>{t('admin.colPaidAt')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {payments?.items.map((p) => (
                        <tr key={p.id}>
                          <td>{p.id}</td>
                          <td>{p.user_email}</td>
                          <td>₩{fmt(p.amount)}</td>
                          <td>
                            <span className={`status-badge ${p.status === 'PAID' ? 'status-posted' : 'status-failed'}`}>
                              {p.status}
                            </span>
                          </td>
                          <td>{fmtDate(p.valid_until)}</td>
                          <td>{fmtDate(p.created_at)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {payments && payments.total_pages > 1 && (
                <div className="admin-pagination">
                  <button
                    type="button"
                    className="btn-outline"
                    disabled={paymentPage === 0}
                    onClick={() => setPaymentPage((p) => p - 1)}
                  >
                    {t('admin.prev')}
                  </button>
                  <span>
                    {paymentPage + 1} / {payments.total_pages}
                  </span>
                  <button
                    type="button"
                    className="btn-outline"
                    disabled={paymentPage >= payments.total_pages - 1}
                    onClick={() => setPaymentPage((p) => p + 1)}
                  >
                    {t('admin.next')}
                  </button>
                </div>
              )}
            </section>
            {/* 공지사항 관리 (개선백로그 🟡) */}
            <section className="analytics-section analytics-section-full">
              <h2 className="analytics-section-title">{t('admin.notices.title')}</h2>
              <form className="admin-notice-form" onSubmit={submitNotice}>
                <input
                  type="text"
                  value={noticeTitle}
                  placeholder={t('admin.notices.titlePlaceholder')}
                  onChange={(e) => setNoticeTitle(e.target.value)}
                />
                <textarea
                  rows={4}
                  value={noticeContent}
                  placeholder={t('admin.notices.contentPlaceholder')}
                  onChange={(e) => setNoticeContent(e.target.value)}
                />
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={noticeSubmitting || !noticeTitle.trim() || !noticeContent.trim()}
                >
                  {noticeSubmitting ? t('admin.notices.publishing') : t('admin.notices.publish')}
                </button>
              </form>
              {notices.length > 0 && (
                <ul className="admin-notice-list">
                  {notices.map((n) => (
                    <li key={n.id} className="admin-notice-item">
                      <span className="admin-notice-title">{n.title}</span>
                      <span className="muted">{n.created_at.slice(0, 10)}</span>
                      <button type="button" className="btn-danger" onClick={() => removeNotice(n.id)}>
                        {t('common.delete')}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        )}

        {toast && <Toast toast={toast} onClose={() => setToast(null)} />}
      </div>
    </div>
  );
}
