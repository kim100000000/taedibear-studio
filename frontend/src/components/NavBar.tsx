import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from './ThemeToggle';
import LangToggle from './LangToggle';

// P-04~P-10 공통 상단 네비게이션 (docs/03_화면설계서.md 3. 공통 컴포넌트)
export default function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      {/* 로그인 상태에서 로고 클릭 → 대시보드 (랜딩으로 나가면 로그아웃처럼 보이는 문제 방지) */}
      <Link to="/dashboard" className="navbar-logo">
        🐻 Taedibear Studio
      </Link>
      <div className="navbar-menu">
        <Link to="/upload">{t('nav.upload')}</Link>
        <Link to="/history">{t('nav.history')}</Link>
        <Link to="/calendar">{t('nav.calendar')}</Link>
        <Link to="/analytics">{t('nav.analytics')}</Link>
        <Link to="/reviews">{t('nav.reviews')}</Link>
        <Link to="/settings">{t('nav.settings')}</Link>
      </div>
      <div className="navbar-profile">
        <LangToggle />
        <ThemeToggle />
        <span>{t('nav.greeting', { name: user?.name })}</span>
        <button type="button" onClick={handleLogout}>
          {t('common.logout')}
        </button>
      </div>
    </nav>
  );
}
