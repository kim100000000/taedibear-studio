import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import OnboardingModal from '../components/OnboardingModal';
import Toast from '../components/Toast';
import { listPosts } from '../api/posts';
import { listInstagramAccounts } from '../api/instagram';
import { getUsage, getMe } from '../api/users';
import { resendVerification } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import type { Post, InstagramAccount, PostStatus, ToastData } from '../types';
import type { UsageInfo } from '../api/payments';

// P-04 대시보드 (docs/03_화면설계서.md)
export default function DashboardPage() {
  const { user } = useAuth();
  const { t } = useTranslation();
  const [posts, setPosts] = useState<Post[]>([]);
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [usage, setUsage] = useState<UsageInfo | null>(null);
  const [credits, setCredits] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<ToastData | null>(null);
  const [showOnboarding, setShowOnboarding] = useState(false);
  // Phase 6: 이메일 미인증 배너 + 재발송
  const [emailVerified, setEmailVerified] = useState(true);
  const [resending, setResending] = useState(false);

  useEffect(() => {
    Promise.all([listPosts({ limit: 100 }), listInstagramAccounts(), getUsage(), getMe()])
      .then(([postsRes, accountsRes, usageRes, meRes]) => {
        setPosts(postsRes.data.data.posts);
        setAccounts(accountsRes.data.data);
        setUsage(usageRes.data.data);
        setCredits(meRes.data.data.credits ?? null);
        setEmailVerified(meRes.data.data.email_verified ?? true);
      })
      .catch((err: any) => {
        const msg = err.isNetworkError
          ? t('error.network')
          : err.response?.data?.error || t('error.loadFailed');
        setToast({ type: 'error', message: msg });
      })
      .finally(() => setLoading(false));
  }, [t]);

  // 사용자별로 대시보드 첫 방문에만 온보딩을 1회 노출한다.
  useEffect(() => {
    if (!user) return;
    const seenKey = `onboarding_seen_${user.id}`;
    if (!localStorage.getItem(seenKey)) {
      setShowOnboarding(true);
    }
  }, [user]);

  const closeOnboarding = () => {
    if (user) localStorage.setItem(`onboarding_seen_${user.id}`, 'true');
    setShowOnboarding(false);
  };

  // Phase 6: 인증 메일 재발송
  const handleResend = async () => {
    setResending(true);
    try {
      await resendVerification();
      setToast({ type: 'success', message: t('dashboard.verifyEmail.resent') });
    } catch (err: any) {
      setToast({
        type: 'error',
        message: err.isNetworkError ? t('error.network') : err.response?.data?.error || t('error.loadFailed'),
      });
    } finally {
      setResending(false);
    }
  };

  if (loading) return <Spinner label={t('common.loading')} />;


  const STATUS_LABEL: Record<PostStatus, string> = {
    draft: t('dashboard.status.draft'),
    scheduled: t('dashboard.status.scheduled'),
    posted: t('dashboard.status.posted'),
    failed: t('dashboard.status.failed'),
  };

  const scheduledCount = posts.filter((p) => p.status === 'scheduled').length;
  const recentPosts = posts.slice(0, 5);

  // 사용량 표시 (서버 usage API 우선, 없으면 클라이언트 카운트)
  const usedThisMonth = usage?.used ?? 0;
  const usageLimit = usage?.limit ?? null;
  const usageLabel = usageLimit != null
    ? `${usedThisMonth} / ${usageLimit}`
    : `${usedThisMonth}`;

  return (
    <div className="page-with-nav">
      {showOnboarding && <OnboardingModal onClose={closeOnboarding} />}
      <Toast toast={toast} onClose={() => setToast(null)} />
      <NavBar />
      <div className="page-content">
        <h1>{t('dashboard.greeting', { name: user?.name })}</h1>

        {/* Phase 6: 이메일 미인증 안내 — 인증 전엔 캡션 생성 불가 */}
        {!emailVerified && (
          <div className="banner banner-warn">
            {t('dashboard.verifyEmail.notice')}{' '}
            <button
              type="button"
              className="banner-link-btn"
              disabled={resending}
              onClick={handleResend}
            >
              {resending ? t('dashboard.verifyEmail.resending') : t('dashboard.verifyEmail.resend')}
            </button>
          </div>
        )}

        {accounts.length === 0 && (
          <div className="banner">
            {t('dashboard.connectBanner')}{' '}
            <Link to="/settings">{t('dashboard.connectBannerLink')}</Link>
          </div>
        )}

        <div className="summary-cards">
          <div className={`summary-card${usageLimit != null && usedThisMonth >= usageLimit ? ' summary-card-warn' : ''}`}>
            <span className="summary-value">{usageLabel}</span>
            <span className="summary-label">{t('dashboard.thisMonthUploads')}</span>
            {usageLimit != null && usage?.plan === 'free' && (
              <span className="summary-plan-badge">{t('plan.free.name')}</span>
            )}
          </div>
          <div className="summary-card">
            <span className="summary-value">{scheduledCount}</span>
            <span className="summary-label">{t('dashboard.scheduled')}</span>
          </div>
          <div className="summary-card">
            <span className="summary-value">{accounts.length}</span>
            <span className="summary-label">{t('dashboard.connectedAccounts')}</span>
          </div>
          {usage?.plan === 'free' && credits !== null && (
            <div className={`summary-card${credits === 0 ? ' summary-card-warn' : ''}`}>
              <span className="summary-value">{credits}</span>
              <span className="summary-label">{t('dashboard.credits')}</span>
              {credits === 0 && (
                <span className="summary-hint">{t('dashboard.creditsEmpty')}</span>
              )}
            </div>
          )}
        </div>

        <Link to="/upload" className="btn-primary btn-large quick-upload-btn">
          {t('dashboard.newPost')}
        </Link>

        <h2>{t('dashboard.recentPosts')}</h2>
        {recentPosts.length === 0 ? (
          <p className="muted">{t('dashboard.noPosts')}</p>
        ) : (
          <ul className="recent-post-list">
            {recentPosts.map((post) => (
              <li key={post.id}>
                <img src={post.image_url} alt="" />
                <div>
                  <p className="post-caption-preview">{post.caption || t('dashboard.noCaption')}</p>
                  <span className={`status-badge status-${post.status}`}>
                    {STATUS_LABEL[post.status]}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
