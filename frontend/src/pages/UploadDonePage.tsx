import { useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
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

  useEffect(() => {
    if (!state?.mode) {
      navigate('/upload', { replace: true });
    }
  }, [state, navigate]);

  if (!state?.mode) return null;

  const { mode, imageUrl, scheduledAt } = state;

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content done-page">
        <img src={imageUrl} alt="업로드한 이미지" className="done-image" />

        {mode === 'immediate' ? (
          <>
            <h1>업로드가 완료되었어요!</h1>
            <p className="muted">인스타그램에 게시물이 바로 올라갔어요.</p>
          </>
        ) : (
          <>
            <h1>예약이 완료되었어요!</h1>
            <p className="muted">
              {scheduledAt && new Date(scheduledAt).toLocaleString('ko-KR')} 에 자동으로 업로드될 거예요.
            </p>
          </>
        )}

        <div className="caption-actions">
          <Link to="/upload" className="btn-primary">
            새 게시물 만들기
          </Link>
          <Link to="/history" className="btn-outline">
            히스토리 보기
          </Link>
        </div>
      </div>
    </div>
  );
}
