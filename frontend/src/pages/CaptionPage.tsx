import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import { generateCaption, createPost } from '../api/posts';

const BUSINESS_TYPES = ['카페', '식당', '베이커리', '기타'];
const MOODS = ['감성적인', '발랄한', '고급스러운', '친근한'];

interface CaptionPageState {
  imageUrl: string;
  instagramAccountId: number;
}

// P-06 AI 캡션 생성 (docs/03_화면설계서.md)
export default function CaptionPage() {
  const { state } = useLocation() as { state: CaptionPageState | null };
  const navigate = useNavigate();

  const [businessType, setBusinessType] = useState(BUSINESS_TYPES[0]);
  const [mood, setMood] = useState(MOODS[0]);
  const [caption, setCaption] = useState('');
  const [hashtags, setHashtags] = useState<string[]>([]);
  const [generating, setGenerating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [hasGenerated, setHasGenerated] = useState(false);

  useEffect(() => {
    if (!state?.imageUrl || !state?.instagramAccountId) {
      navigate('/upload', { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!state?.imageUrl) return null;

  const { imageUrl, instagramAccountId } = state;

  const handleGenerate = async () => {
    setGenerating(true);
    setError('');
    try {
      const { data } = await generateCaption(imageUrl, businessType, mood);
      setCaption(data.data.caption);
      setHashtags(data.data.hashtags);
      setHasGenerated(true);
    } catch (err: any) {
      setError(err.response?.data?.error || '캡션 생성에 실패했어요.');
    } finally {
      setGenerating(false);
    }
  };

  const removeHashtag = (tag: string) => {
    setHashtags((prev) => prev.filter((t) => t !== tag));
  };

  const goNext = async (mode: 'immediate' | 'schedule') => {
    setSubmitting(true);
    setError('');
    try {
      const { data } = await createPost({
        instagram_account_id: instagramAccountId,
        image_url: imageUrl,
        caption,
        hashtags,
      });
      const postId = data.data.id;

      if (mode === 'immediate') {
        navigate('/upload/done', { state: { mode: 'immediate', imageUrl, postId } });
      } else {
        navigate('/upload/schedule', { state: { postId, imageUrl, caption } });
      }
    } catch (err: any) {
      setError(err.response?.data?.error || '저장에 실패했어요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content caption-page">
        <h1>AI 캡션 생성</h1>

        <div className="caption-layout">
          <img src={imageUrl} alt="업로드한 이미지" className="caption-preview-image" />

          <div className="caption-editor">
            <div className="form-row">
              <div className="form-field">
                <label>업종</label>
                <select value={businessType} onChange={(e) => setBusinessType(e.target.value)}>
                  {BUSINESS_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-field">
                <label>분위기</label>
                <select value={mood} onChange={(e) => setMood(e.target.value)}>
                  {MOODS.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {generating ? (
              <Spinner label="캡션을 만들고 있어요..." />
            ) : (
              <>
                <button type="button" className="btn-outline" onClick={handleGenerate}>
                  {hasGenerated ? '다시 생성' : 'AI 캡션 생성'}
                </button>

                <textarea
                  rows={6}
                  placeholder="캡션을 입력하거나 AI로 생성해보세요."
                  value={caption}
                  onChange={(e) => setCaption(e.target.value)}
                />

                <div className="hashtag-list">
                  {hashtags.map((tag) => (
                    <span key={tag} className="hashtag-chip">
                      {tag}
                      <button type="button" onClick={() => removeHashtag(tag)}>
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              </>
            )}

            {error && <p className="error">{error}</p>}

            <div className="caption-actions">
              <button
                type="button"
                className="btn-primary"
                disabled={submitting || !caption}
                onClick={() => goNext('immediate')}
              >
                즉시 업로드
              </button>
              <button
                type="button"
                className="btn-outline"
                disabled={submitting || !caption}
                onClick={() => goNext('schedule')}
              >
                예약 설정
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
