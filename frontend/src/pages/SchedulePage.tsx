import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
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
      setError('날짜와 시간을 모두 선택해주세요.');
      return;
    }
    const scheduledAt = new Date(`${date}T${time}:00`);
    if (Number.isNaN(scheduledAt.getTime())) {
      setError('날짜/시간이 올바르지 않아요.');
      return;
    }
    if (scheduledAt.getTime() - Date.now() < MIN_LEAD_MS) {
      setError('예약은 현재 시각보다 최소 10분 이후로 설정해주세요.');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      await createSchedule({ post_id: postId, scheduled_at: scheduledAt.toISOString() });
      navigate('/upload/done', { state: { mode: 'scheduled', imageUrl, scheduledAt: scheduledAt.toISOString() } });
    } catch (err: any) {
      setError(err.response?.data?.error || '예약에 실패했어요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content schedule-page">
        <h1>예약 설정</h1>

        <div className="schedule-layout">
          <img src={imageUrl} alt="업로드한 이미지" className="caption-preview-image" />
          <div className="schedule-form">
            <p className="post-caption-preview">{caption}</p>

            <div className="form-row">
              <div className="form-field">
                <label>날짜</label>
                <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
              </div>
              <div className="form-field">
                <label>시간</label>
                <input type="time" value={time} onChange={(e) => setTime(e.target.value)} />
              </div>
            </div>

            {error && <p className="error">{error}</p>}

            <div className="caption-actions">
              <button type="button" className="btn-primary" disabled={submitting} onClick={handleConfirm}>
                예약 확정
              </button>
              <button type="button" className="btn-outline" onClick={() => navigate(-1)}>
                취소
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
