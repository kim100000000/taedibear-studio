import { useTheme } from '../context/ThemeContext';

// 다크모드 전환 버튼 (공통 컴포넌트). NavBar와 랜딩 페이지 헤더에서 재사용한다.
export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={toggleTheme}
      aria-label={isDark ? '라이트 모드로 전환' : '다크 모드로 전환'}
      title={isDark ? '라이트 모드로 전환' : '다크 모드로 전환'}
    >
      {isDark ? '☀️' : '🌙'}
    </button>
  );
}
