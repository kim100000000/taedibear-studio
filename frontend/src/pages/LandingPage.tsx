import { Link } from 'react-router-dom';
import ThemeToggle from '../components/ThemeToggle';

// P-01 랜딩 페이지 (docs/03_화면설계서.md)
export default function LandingPage() {
  return (
    <div className="landing-page">
      <header className="landing-header">
        <div className="landing-logo">🐻 Taedibear Studio</div>
        <div className="landing-header-actions">
          <ThemeToggle />
          <Link to="/login" className="btn-outline">
            로그인
          </Link>
        </div>
      </header>

      <section className="landing-hero">
        <h1>사진 한 장으로, 인스타그램 업로드까지 한 번에</h1>
        <p>AI가 캡션과 해시태그를 만들어주고, 원하는 시간에 자동으로 업로드해드려요.</p>
        <Link to="/signup" className="btn-primary btn-large">
          무료로 시작하기
        </Link>
      </section>

      <section className="landing-steps">
        <div className="step-card">
          <span className="step-number">1</span>
          <h3>사진 업로드</h3>
          <p>오늘 찍은 사진을 올려주세요.</p>
        </div>
        <div className="step-card">
          <span className="step-number">2</span>
          <h3>AI 캡션 생성</h3>
          <p>업종과 분위기에 맞는 캡션과 해시태그를 AI가 만들어드려요.</p>
        </div>
        <div className="step-card">
          <span className="step-number">3</span>
          <h3>자동 업로드</h3>
          <p>지금 바로, 또는 원하는 시간에 인스타그램에 자동으로 올라가요.</p>
        </div>
      </section>

      <section className="landing-audience">
        <h2>이런 분들께 추천해요</h2>
        <div className="audience-list">
          <span>☕ 카페</span>
          <span>🍽️ 식당</span>
          <span>🥐 베이커리</span>
        </div>
      </section>

      <footer className="landing-footer">
        <span>이용약관</span>
        <span>개인정보처리방침</span>
      </footer>
    </div>
  );
}
