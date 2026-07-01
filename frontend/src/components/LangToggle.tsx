import { useTranslation } from 'react-i18next';

// 언어 전환 버튼 (6단계 다국어 지원). NavBar와 랜딩 페이지 헤더에서 재사용한다.
// 선택 언어를 localStorage 'lang' 키에 저장해 새로고침 후에도 유지한다.
export default function LangToggle() {
  const { i18n, t } = useTranslation();
  const isKo = i18n.language === 'ko';

  const toggle = () => {
    const next = isKo ? 'en' : 'ko';
    void i18n.changeLanguage(next);
    localStorage.setItem('lang', next);
  };

  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={toggle}
      title={isKo ? t('lang.toEn') : t('lang.toKo')}
      aria-label={isKo ? t('lang.toEn') : t('lang.toKo')}
    >
      {isKo ? 'EN' : 'KO'}
    </button>
  );
}
