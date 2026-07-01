import { useTranslation } from 'react-i18next';
import { useTheme } from '../context/ThemeContext';

// 다크모드 전환 버튼 (공통 컴포넌트). NavBar와 랜딩 페이지 헤더에서 재사용한다.
export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const { t } = useTranslation();
  const isDark = theme === 'dark';

  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={toggleTheme}
      aria-label={isDark ? t('theme.toLight') : t('theme.toDark')}
      title={isDark ? t('theme.toLight') : t('theme.toDark')}
    >
      {isDark ? '☀️' : '🌙'}
    </button>
  );
}
