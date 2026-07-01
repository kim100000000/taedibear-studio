import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import UpgradeModal from '../components/UpgradeModal';
import { generateCaption, createPost } from '../api/posts';

// API로 전달되는 값은 한국어 그대로 유지 (Gemini 프롬프트와 맞춰야 함).
// labelKey만 i18n 키로 매핑해서 UI 표시를 번역한다.
const BUSINESS_TYPES: { value: string; labelKey: string }[] = [
  { value: '카페', labelKey: 'caption.businessTypes.cafe' },
  { value: '식당', labelKey: 'caption.businessTypes.restaurant' },
  { value: '베이커리', labelKey: 'caption.businessTypes.bakery' },
  { value: '기타', labelKey: 'caption.businessTypes.other' },
];

const MOODS: { value: string; labelKey: string }[] = [
  { value: '감성적인', labelKey: 'caption.moods.emotional' },
  { value: '발랄한', labelKey: 'caption.moods.lively' },
  { value: '고급스러운', labelKey: 'caption.moods.luxury' },
  { value: '친근한', labelKey: 'caption.moods.friendly' },
];

interface CaptionPageState {
  imageUrl: string;
  instagramAccountId: number;
}

// P-06 AI 캡션 생성 (docs/03_화면설계서.md)
export default function CaptionPage() {
  const { state } = useLocation() as { state: CaptionPageState | null };
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [businessType, setBusinessType] = useState(BUSINESS_TYPES[0].value);
  const [mood, setMood] = useState(MOODS[0].value);
  const [caption, setCaption] = useState('');
  const [hashtags, setHashtags] = useState<string[]>([]);
  const [generating, setGenerating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [hasGenerated, setHasGenerated] = useState(false);
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);

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
      setError(err.response?.data?.error || t('caption.err.generateFailed'));
    } finally {
      setGenerating(false);
    }
  };

  const removeHashtag = (tag: string) => {
    setHashtags((prev) => prev.filter((h) => h !== tag));
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
      if (err.response?.status === 403) {
        setShowUpgradeModal(true);
      } else {
        setError(err.response?.data?.error || t('caption.err.saveFailed'));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <UpgradeModal open={showUpgradeModal} onClose={() => setShowUpgradeModal(false)} />
      <div className="page-content caption-page">
        <h1>{t('caption.title')}</h1>

        <div className="caption-layout">
          <img src={imageUrl} alt="" className="caption-preview-image" />

          <div className="caption-editor">
            <div className="form-row">
              <div className="form-field">
                <label>{t('caption.businessTypeLabel')}</label>
                <select value={businessType} onChange={(e) => setBusinessType(e.target.value)}>
                  {BUSINESS_TYPES.map((bt) => (
                    <option key={bt.value} value={bt.value}>
                      {t(bt.labelKey)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-field">
                <label>{t('caption.moodLabel')}</label>
                <select value={mood} onChange={(e) => setMood(e.target.value)}>
                  {MOODS.map((m) => (
                    <option key={m.value} value={m.value}>
                      {t(m.labelKey)}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {generating ? (
              <Spinner label={t('caption.generating')} />
            ) : (
              <>
                <button type="button" className="btn-outline" onClick={handleGenerate}>
                  {hasGenerated ? t('caption.regenerate') : t('caption.generate')}
                </button>

                <textarea
                  rows={6}
                  placeholder={t('caption.placeholder')}
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
                {t('caption.immediate')}
              </button>
              <button
                type="button"
                className="btn-outline"
                disabled={submitting || !caption}
                onClick={() => goNext('schedule')}
              >
                {t('caption.schedule')}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
