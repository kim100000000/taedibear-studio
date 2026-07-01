import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import OnboardingModal from '../components/OnboardingModal';
import { listPosts } from '../api/posts';
import { listInstagramAccounts } from '../api/instagram';
import { useAuth } from '../context/AuthContext';
import type { Post, InstagramAccount, PostStatus } from '../types';

// P-04 대시보드 (docs/03_화면설계서.md)
export default function DashboardPage() {
  const { user } = useAuth();
  const { t } = useTranslation();
  const [posts, setPosts] = useState<Post[]>([]);
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [showOnboarding, setShowOnboarding] = useState(false);

  useEffect(() => {
    Promise.all([listPosts({ limit: 100 }), listInstagramAccounts()])
      .then(([postsRes, accountsRes]) => {
        setPosts(postsRes.data.data.posts);
        setAccounts(accountsRes.data.data);
      })
      .finally(() => setLoading(false));
  }, []);

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

  if (loading) return <Spinner label={t('common.loading')} />;

  const STATUS_LABEL: Record<PostStatus, string> = {
    draft: t('dashboard.status.draft'),
    scheduled: t('dashboard.status.scheduled'),
    posted: t('dashboard.status.posted'),
    failed: t('dashboard.status.failed'),
  };

  const now = new Date();
  const thisMonthUploads = posts.filter(
    (p) =>
      p.status === 'posted' &&
      p.posted_at &&
      new Date(p.posted_at).getMonth() === now.getMonth() &&
      new Date(p.posted_at).getFullYear() === now.getFullYear()
  ).length;
  const scheduledCount = posts.filter((p) => p.status === 'scheduled').length;
  const recentPosts = posts.slice(0, 5);

  return (
    <div className="page-with-nav">
      {showOnboarding && <OnboardingModal onClose={closeOnboarding} />}
      <NavBar />
      <div className="page-content">
        <h1>{t('dashboard.greeting', { name: user?.name })}</h1>

        {accounts.length === 0 && (
          <div className="banner">
            {t('dashboard.connectBanner')}{' '}
            <Link to="/settings">{t('dashboard.connectBannerLink')}</Link>
          </div>
        )}

        <div className="summary-cards">
          <div className="summary-card">
            <span className="summary-value">{thisMonthUploads}</span>
            <span className="summary-label">{t('dashboard.thisMonthUploads')}</span>
          </div>
          <div className="summary-card">
            <span className="summary-value">{scheduledCount}</span>
            <span className="summary-label">{t('dashboard.scheduled')}</span>
          </div>
          <div className="summary-card">
            <span className="summary-value">{accounts.length}</span>
            <span className="summary-label">{t('dashboard.connectedAccounts')}</span>
          </div>
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
