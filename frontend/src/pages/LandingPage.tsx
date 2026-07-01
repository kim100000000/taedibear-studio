import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ThemeToggle from '../components/ThemeToggle';
import LangToggle from '../components/LangToggle';

// P-01 랜딩 페이지 (Phase 2-4 완성판) — docs/03_화면설계서.md
export default function LandingPage() {
  const { t } = useTranslation();

  const freeFeatures: string[] = t('plan.free.features', { returnObjects: true }) as string[];
  const proFeatures: string[] = t('plan.pro.features', { returnObjects: true }) as string[];

  return (
    <div className="landing-page">
      {/* 헤더 */}
      <header className="landing-header">
        <div className="landing-logo">🐻 Taedibear Studio</div>
        <div className="landing-header-actions">
          <LangToggle />
          <ThemeToggle />
          <Link to="/login" className="btn-outline landing-login-btn">
            {t('landing.login')}
          </Link>
        </div>
      </header>

      {/* 히어로 */}
      <section className="landing-hero">
        <div className="landing-hero-badge">{t('landing.heroBadge')}</div>
        <h1>{t('landing.hero.title')}</h1>
        <p>{t('landing.hero.subtitle')}</p>
        <div className="landing-hero-actions">
          <Link to="/signup" className="btn-primary btn-large">
            {t('landing.hero.cta')}
          </Link>
          <Link to="/login" className="btn-outline btn-large">
            {t('landing.hero.login')}
          </Link>
        </div>
        <p className="landing-hero-hint">{t('landing.hero.hint')}</p>

        {/* 앱 미리보기 mock */}
        <div className="landing-mock-browser">
          <div className="landing-mock-bar">
            <span className="mock-dot" />
            <span className="mock-dot" />
            <span className="mock-dot" />
            <span className="mock-url">taedibear.studio/upload/caption</span>
          </div>
          <div className="landing-mock-screen">
            <div className="mock-caption-layout">
              <div className="mock-image-box">🖼️</div>
              <div className="mock-caption-right">
                <div className="mock-row">
                  <div className="mock-label">{t('caption.businessTypeLabel')}</div>
                  <div className="mock-chip">☕ {t('caption.businessTypes.cafe')}</div>
                </div>
                <div className="mock-row">
                  <div className="mock-label">{t('caption.moodLabel')}</div>
                  <div className="mock-chip">✨ {t('caption.moods.emotional')}</div>
                </div>
                <div className="mock-generate-btn">{t('landing.mockGenerate')}</div>
                <div className="mock-caption-text">{t('landing.mockCaptionText')}</div>
                <div className="mock-hashtags">
                  <span>#카페스타그램</span>
                  <span>#분위기맛집</span>
                  <span>#감성카페</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3단계 흐름 */}
      <section className="landing-steps">
        <div className="landing-section-title">{t('landing.howTitle')}</div>
        <div className="landing-steps-grid">
          <div className="step-card">
            <span className="step-icon">📸</span>
            <span className="step-number">1</span>
            <h3>{t('landing.step1.title')}</h3>
            <p>{t('landing.step1.desc')}</p>
          </div>
          <div className="step-arrow">→</div>
          <div className="step-card">
            <span className="step-icon">🤖</span>
            <span className="step-number">2</span>
            <h3>{t('landing.step2.title')}</h3>
            <p>{t('landing.step2.desc')}</p>
          </div>
          <div className="step-arrow">→</div>
          <div className="step-card">
            <span className="step-icon">🚀</span>
            <span className="step-number">3</span>
            <h3>{t('landing.step3.title')}</h3>
            <p>{t('landing.step3.desc')}</p>
          </div>
        </div>
      </section>

      {/* 타깃 업종 */}
      <section className="landing-audience">
        <div className="landing-section-title">{t('landing.audience.title')}</div>
        <div className="audience-list">
          <div className="audience-chip">{t('landing.audience.cafe')}</div>
          <div className="audience-chip">{t('landing.audience.restaurant')}</div>
          <div className="audience-chip">{t('landing.audience.bakery')}</div>
          <div className="audience-chip">{t('landing.audience.beauty')}</div>
          <div className="audience-chip">{t('landing.audience.fitness')}</div>
        </div>
      </section>

      {/* 요금제 */}
      <section className="landing-pricing">
        <div className="landing-section-title">{t('landing.pricing.title')}</div>
        <p className="landing-section-sub">{t('landing.pricing.sub')}</p>
        <div className="landing-pricing-cards">
          <div className="landing-plan-card">
            <div className="landing-plan-name">{t('plan.free.name')}</div>
            <div className="landing-plan-price">
              {t('plan.free.price')}
              <span className="landing-plan-period">{t('plan.priceSuffix')}</span>
            </div>
            <ul className="landing-plan-features">
              {freeFeatures.map((f) => (
                <li key={f}>✓ {f}</li>
              ))}
            </ul>
            <Link to="/signup" className="btn-outline">
              {t('landing.pricing.freeCta')}
            </Link>
          </div>
          <div className="landing-plan-card landing-plan-card--pro">
            <div className="landing-plan-badge">{t('landing.pricing.recommended')}</div>
            <div className="landing-plan-name">{t('plan.pro.name')}</div>
            <div className="landing-plan-price">
              {t('plan.pro.price')}
              <span className="landing-plan-period">{t('plan.priceSuffix')}</span>
            </div>
            <ul className="landing-plan-features">
              {proFeatures.map((f) => (
                <li key={f}>✓ {f}</li>
              ))}
            </ul>
            <Link to="/signup" className="btn-primary">
              {t('landing.pricing.proCta')}
            </Link>
          </div>
        </div>
      </section>

      {/* 푸터 */}
      <footer className="landing-footer">
        <div className="landing-footer-brand">
          🐻 Taedibear Studio &copy; {new Date().getFullYear()}
        </div>
        <div className="landing-footer-links">
          <a href="/terms">{t('landing.terms')}</a>
          <a href="/privacy">{t('landing.privacy')}</a>
          <a href="mailto:support@taedibear.studio">{t('landing.contact')}</a>
        </div>
      </footer>
    </div>
  );
}
