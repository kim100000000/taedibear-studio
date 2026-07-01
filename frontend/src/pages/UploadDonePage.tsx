import { useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import NavBar from '../components/NavBar';

interface UploadDonePageState {
  mode: 'immediate' | 'scheduled';
  imageUrl: string;
  scheduledAt?: string;
}

// P-08 업로드 완료 (docs/03_화면설계서.md)
export default function UploadDonePage() {
  const { state } = useLocation() as { state: UploadDonePageState | null };
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    if (!state?.mode) {
      navigate('/upload', { replace: true });
    }
  }, [state, navigate]);

  if (!state?.mode) return null;

  const { mode, imageUrl, scheduledAt } = state;
  // 날짜 표시는 선택된 언어의 로케일을 따른다.
  const locale = i18n.language === 'en' ? 'en-US' : 'ko-KR';

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content done-page">
        <img src={imageUrl} alt="" className="done-image" />

        {mode === 'immediate' ? (
          <>
            <h1>{t('done.immediate.title')}</h1>
            <p className="muted">{t('done.immediate.desc')}</p>
          </>
        ) : (
          <>
            <h1>{t('done.scheduled.title')}</h1>
            <p className="muted">
              {t('done.scheduled.desc', {
                time: scheduledAt ? new Date(scheduledAt).toLocaleString(locale) : '',
              })}
            </p>
          </>
        )}

        <div className="caption-actions">
          <Link to="/upload" className="btn-primary">
            {t('done.newPost')}
          </Link>
          <Link to="/history" className="btn-outline">
            {t('done.history')}
          </Link>
        </div>
      </div>
    </div>
  );
}
