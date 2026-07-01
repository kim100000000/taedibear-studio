import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import ko from './locales/ko.json';
import en from './locales/en.json';

// 6단계 다국어 지원. 지원 언어: 한국어(기본), 영어.
// 선택 언어는 localStorage의 'lang' 키에 저장된다. (LangToggle 참고)
const savedLang = localStorage.getItem('lang');
const defaultLang: string = savedLang === 'en' || savedLang === 'ko' ? savedLang : 'ko';

i18n.use(initReactI18next).init({
  resources: {
    ko: { translation: ko },
    en: { translation: en },
  },
  lng: defaultLang,
  fallbackLng: 'ko',
  interpolation: {
    escapeValue: false, // React가 XSS 방어를 이미 처리함
  },
});

export default i18n;
