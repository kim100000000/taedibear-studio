import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Toast from '../components/Toast';
import { listNotices } from '../api/notices';
import type { Notice } from '../api/notices';
import type { ToastData } from '../types';

const SUPPORT_EMAIL = 'kst980510@gmail.com';

// 개선백로그 🟡: 공지사항 + 문의 (P-20 /notices)
export default function NoticesPage() {
  const { t } = useTranslation();
  const [notices, setNotices] = useState<Notice[]>([]);
  const [loading, setLoading] = useState(true);
  const [openId, setOpenId] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);

  useEffect(() => {
    listNotices()
      .then((res) => {
        setNotices(res.data.data);
        if (res.data.data.length > 0) setOpenId(res.data.data[0].id);
      })
      .catch((err: any) => {
        const msg = err.isNetworkError ? t('error.network') : err.response?.data?.error || t('error.loadFailed');
        setToast({ type: 'error', message: msg });
      })
      .finally(() => setLoading(false));
  }, [t]);

  const fmtDate = (iso: string) => iso.slice(0, 10);

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content notices-page">
        <h1>{t('notices.title')}</h1>

        {loading ? (
          <Spinner label={t('common.loading')} />
        ) : notices.length === 0 ? (
          <EmptyState emoji="📢" message={t('notices.empty')} />
        ) : (
          <ul className="notice-list">
            {notices.map((n) => (
              <li key={n.id} className="notice-item">
                <button
                  type="button"
                  className="notice-item-header"
                  onClick={() => setOpenId((prev) => (prev === n.id ? null : n.id))}
                >
                  <span className="notice-item-title">{n.title}</span>
                  <span className="notice-item-meta">
                    {fmtDate(n.created_at)} {openId === n.id ? '▾' : '▸'}
                  </span>
                </button>
                {openId === n.id && (
                  <div className="notice-item-content">{n.content}</div>
                )}
              </li>
            ))}
          </ul>
        )}

        {/* 문의 — 1단계: 이메일 링크 */}
        <section className="contact-section">
          <h2>{t('notices.contact.title')}</h2>
          <p className="muted">{t('notices.contact.description')}</p>
          <a
            className="btn-outline"
            href={`mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent(t('notices.contact.subject'))}`}
          >
            ✉️ {t('notices.contact.button')}
          </a>
        </section>
      </div>
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
