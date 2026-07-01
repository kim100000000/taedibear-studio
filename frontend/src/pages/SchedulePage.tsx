import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import { createSchedule } from '../api/scheduled';

const MIN_LEAD_MS = 10 * 60 * 1000;

interface SchedulePageState {
  postId: number;
  imageUrl: string;
  caption: string;
}

// P-07 예약 설정 (docs/03_화면설계서.md)
export default function SchedulePage() {
  const { state } = useLocation() as { state: SchedulePageState | null };
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!state?.postId) {
      navigate('/upload', { replace: true });
    }
  }, [state, navigate]);

  if (!state?.postId) return null;

  const { postId, imageUrl, caption } = state;

  const handleConfirm = async () => {
    if (!date || !time) {
      setError(t('schedule.err.required'));
      return;
    }
    const scheduledAt = new Date(`${date}T${time}:00`);
    if (Number.isNaN(scheduledAt.getTime())) {
      setError(t('schedule.err.invalid'));
      return;
    }
    if (scheduledAt.getTime() - Date.now() < MIN_LEAD_MS) {
      setError(t('schedule.err.tooSoon'));
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      await createSchedule({ post_id: postId, scheduled_at: scheduledAt.toISOString() });
      navigate('/upload/done', { state: { mode: 'scheduled', imageUrl, scheduledAt: scheduledAt.toISOString() } });
    } catch (err: any) {
      setError(err.response?.data?.error || t('schedule.err.failed'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content schedule-page">
        <h1>{t('schedule.title')}</h1>

        <div className="schedule-layout">
          <img src={imageUrl} alt="" className="caption-preview-image" />
          <div className="schedule-form">
            <p className="post-caption-preview">{caption}</p>

            <div className="form-row">
              <div className="form-field">
                <label>{t('schedule.dateLabel')}</label>
                <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
              </div>
              <div className="form-field">
                <label>{t('schedule.timeLabel')}</label>
                <input type="time" value={time} onChange={(e) => setTime(e.target.value)} />
              </div>
            </div>

            {error && <p className="error">{error}</p>}

            <div className="caption-actions">
              <button type="button" className="btn-primary" disabled={submitting} onClick={handleConfirm}>
                {t('schedule.confirm')}
              </button>
              <button type="button" className="btn-outline" onClick={() => navigate(-1)}>
                {t('schedule.cancel')}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
