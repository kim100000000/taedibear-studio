import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ThemeToggle from '../components/ThemeToggle';
import LangToggle from '../components/LangToggle';

// P-01 랜딩 페이지 (docs/03_화면설계서.md)
export default function LandingPage() {
  const { t } = useTranslation();

  return (
    <div className="landing-page">
      <header className="landing-header">
        <div className="landing-logo">🐻 Taedibear Studio</div>
        <div className="landing-header-actions">
          <LangToggle />
          <ThemeToggle />
          <Link to="/login" className="btn-outline">
            {t('landing.login')}
          </Link>
        </div>
      </header>

      <section className="landing-hero">
        <h1>{t('landing.hero.title')}</h1>
        <p>{t('landing.hero.subtitle')}</p>
        <Link to="/signup" className="btn-primary btn-large">
          {t('landing.hero.cta')}
        </Link>
      </section>

      <section className="landing-steps">
        <div className="step-card">
          <span className="step-number">1</span>
          <h3>{t('landing.step1.title')}</h3>
          <p>{t('landing.step1.desc')}</p>
        </div>
        <div className="step-card">
          <span className="step-number">2</span>
          <h3>{t('landing.step2.title')}</h3>
          <p>{t('landing.step2.desc')}</p>
        </div>
        <div className="step-card">
          <span className="step-number">3</span>
          <h3>{t('landing.step3.title')}</h3>
          <p>{t('landing.step3.desc')}</p>
        </div>
      </section>

      <section className="landing-audience">
        <h2>{t('landing.audience.title')}</h2>
        <div className="audience-list">
          <span>{t('landing.audience.cafe')}</span>
          <span>{t('landing.audience.restaurant')}</span>
          <span>{t('landing.audience.bakery')}</span>
        </div>
      </section>

      <footer className="landing-footer">
        <span>{t('landing.terms')}</span>
        <span>{t('landing.privacy')}</span>
      </footer>
    </div>
  );
}
