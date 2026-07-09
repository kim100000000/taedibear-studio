import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import UpgradeModal from '../components/UpgradeModal';
import { generateCaption, createPost, publishPost } from '../api/posts';
import { getMe, watchAd } from '../api/users';
import { listInstagramAccounts } from '../api/instagram';
import { saveCaption, listSavedCaptions, deleteSavedCaption } from '../api/captions';
import type { SavedCaption } from '../api/captions';

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
  const [specialMenu, setSpecialMenu] = useState('');
  const [eventPromotion, setEventPromotion] = useState('');
  const [keywords, setKeywords] = useState('');
  const [showOptional, setShowOptional] = useState(false);
  const [caption, setCaption] = useState('');
  const [hashtags, setHashtags] = useState<string[]>([]);
  const [generating, setGenerating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [hasGenerated, setHasGenerated] = useState(false);
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);
  const [credits, setCredits] = useState<number | null>(null);
  const [adWatching, setAdWatching] = useState(false);
  const [userPlan, setUserPlan] = useState<'free' | 'pro'>('free');
  // 개선백로그 🟠: 인스타 스타일 미리보기에 표시할 계정명
  const [accountUsername, setAccountUsername] = useState<string | null>(null);
  // 개선백로그 🟡: 캡션 보관함
  const [vaultOpen, setVaultOpen] = useState(false);
  const [vault, setVault] = useState<SavedCaption[]>([]);
  const [vaultLoading, setVaultLoading] = useState(false);
  const [savingCaption, setSavingCaption] = useState(false);

  useEffect(() => {
    if (!state?.imageUrl || !state?.instagramAccountId) {
      navigate('/upload', { replace: true });
      return;
    }
    // Phase 2-3: 저장된 업종/분위기 자동 로드 / Phase 2-1: 크레딧 로드
    getMe().then(({ data }) => {
      if (data.data.business_type) setBusinessType(data.data.business_type);
      if (data.data.mood) setMood(data.data.mood);
      if (data.data.credits !== undefined) setCredits(data.data.credits);
      if (data.data.plan) setUserPlan(data.data.plan as 'free' | 'pro');
    }).catch(() => { /* 실패해도 기본값 사용 */ });
    // 미리보기용 계정명 — 실패해도 미리보기는 계정명 없이 동작
    listInstagramAccounts().then(({ data }) => {
      const acc = data.data.find((a) => a.id === state.instagramAccountId);
      if (acc) setAccountUsername(acc.username);
    }).catch(() => { /* 무시 */ });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!state?.imageUrl) return null;

  const { imageUrl, instagramAccountId } = state;

  const handleWatchAd = async () => {
    setAdWatching(true);
    setError('');
    try {
      // 실제 광고 연동 전: 2초 딜레이로 광고 시청 시뮬레이션
      await new Promise((r) => setTimeout(r, 2000));
      const { data } = await watchAd();
      setCredits(data.data.credits);
    } catch (err: any) {
      setError(err.response?.data?.error || t('caption.err.adFailed'));
    } finally {
      setAdWatching(false);
    }
  };

  const handleGenerate = async () => {
    setGenerating(true);
    setError('');
    try {
      const { data } = await generateCaption({
        image_url: imageUrl,
        business_type: businessType,
        mood,
        special_menu: specialMenu || undefined,
        event_promotion: eventPromotion || undefined,
        keywords: keywords || undefined,
      });
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

  // 개선백로그 🟡: 캡션 보관함 — 저장 / 열기 / 사용 / 삭제
  const handleSaveCaption = async () => {
    if (!caption.trim()) return;
    setSavingCaption(true);
    setError('');
    try {
      await saveCaption(caption.trim(), hashtags);
      setError('');
      setVaultOpen(false);
      // 저장 성공 안내는 버튼 라벨 변화로 충분 — 간단히 처리
    } catch (err: any) {
      setError(err.response?.data?.error || t('caption.vault.saveFailed'));
    } finally {
      setSavingCaption(false);
    }
  };

  const openVault = async () => {
    setVaultOpen(true);
    setVaultLoading(true);
    try {
      const { data } = await listSavedCaptions();
      setVault(data.data);
    } catch {
      setVault([]);
    } finally {
      setVaultLoading(false);
    }
  };

  const useSaved = (item: SavedCaption) => {
    setCaption(item.caption);
    setHashtags(item.hashtags);
    setHasGenerated(true);
    setVaultOpen(false);
  };

  const removeSaved = async (id: number) => {
    try {
      await deleteSavedCaption(id);
      setVault((prev) => prev.filter((v) => v.id !== id));
    } catch { /* 무시 */ }
  };

  // 개선백로그 🟠: 임시 저장 — draft로만 저장하고 히스토리의 "임시 저장" 탭으로 이동
  const saveDraft = async () => {
    setSubmitting(true);
    setError('');
    try {
      await createPost({
        instagram_account_id: instagramAccountId,
        image_url: imageUrl,
        caption,
        hashtags,
      });
      navigate('/history', { state: { initialTab: 'draft' } });
    } catch (err: any) {
      setError(err.response?.data?.error || t('caption.err.saveFailed'));
    } finally {
      setSubmitting(false);
    }
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
        // draft로 저장된 게시물을 실제로 인스타그램에 발행 (POST /api/posts/:id/publish)
        try {
          await publishPost(postId);
        } catch (publishErr: any) {
          setError(publishErr.response?.data?.error || t('caption.err.publishFailed'));
          return;
        }
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
          {/* 개선백로그 🟠: 인스타그램 피드 스타일 실시간 미리보기 */}
          <div className="ig-preview">
            <div className="ig-preview-header">
              <span className="ig-preview-avatar" aria-hidden="true" />
              <strong>{accountUsername ? `@${accountUsername}` : t('caption.previewAccount')}</strong>
            </div>
            <img src={imageUrl} alt="" className="ig-preview-image" />
            <div className="ig-preview-actions" aria-hidden="true">♡ 💬 ↗</div>
            <p className="ig-preview-caption">
              {caption || <span className="muted">{t('caption.previewEmpty')}</span>}
              {hashtags.length > 0 && (
                <span className="ig-preview-hashtags"> {hashtags.join(' ')}</span>
              )}
            </p>
          </div>

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

            {/* Phase 2-3: 선택 입력 필드 */}
            <button
              type="button"
              className="caption-optional-toggle"
              onClick={() => setShowOptional((v) => !v)}
            >
              {showOptional ? '▲' : '▼'} {t('caption.optionalToggle')}
            </button>
            {showOptional && (
              <div className="caption-optional-fields">
                <div className="form-field">
                  <label>{t('caption.specialMenuLabel')}</label>
                  <input
                    type="text"
                    placeholder={t('caption.specialMenuPlaceholder')}
                    value={specialMenu}
                    onChange={(e) => setSpecialMenu(e.target.value)}
                  />
                </div>
                <div className="form-field">
                  <label>{t('caption.eventPromotionLabel')}</label>
                  <input
                    type="text"
                    placeholder={t('caption.eventPromotionPlaceholder')}
                    value={eventPromotion}
                    onChange={(e) => setEventPromotion(e.target.value)}
                  />
                </div>
                <div className="form-field">
                  <label>{t('caption.keywordsLabel')}</label>
                  <input
                    type="text"
                    placeholder={t('caption.keywordsPlaceholder')}
                    value={keywords}
                    onChange={(e) => setKeywords(e.target.value)}
                  />
                </div>
              </div>
            )}

            {/* Phase 2-1: 크레딧 표시 및 광고 충전 (Free 플랜만) */}
            {userPlan === 'free' && credits !== null && (
              <div className="credit-status">
                <span className="credit-badge">
                  ⚡ {t('caption.credits', { count: credits })}
                </span>
                {credits === 0 && (
                  <button
                    type="button"
                    className="btn-outline credit-ad-btn"
                    disabled={adWatching}
                    onClick={handleWatchAd}
                  >
                    {adWatching ? t('caption.adWatching') : t('caption.watchAd')}
                  </button>
                )}
              </div>
            )}

            {generating ? (
              <Spinner label={t('caption.generating')} />
            ) : (
              <>
                <div className="caption-generate-row">
                  <button
                    type="button"
                    className="btn-outline"
                    disabled={userPlan === 'free' && credits !== null && credits <= 0}
                    onClick={handleGenerate}
                  >
                    {hasGenerated ? t('caption.regenerate') : t('caption.generate')}
                  </button>
                  {/* 개선백로그 🟡: 캡션 보관함 */}
                  <button type="button" className="btn-outline" onClick={openVault}>
                    {t('caption.vault.open')}
                  </button>
                  {caption.trim() && (
                    <button
                      type="button"
                      className="btn-outline"
                      disabled={savingCaption}
                      onClick={handleSaveCaption}
                    >
                      {savingCaption ? t('caption.vault.saving') : t('caption.vault.save')}
                    </button>
                  )}
                </div>

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
              {/* 개선백로그 🟠: 임시 저장 (draft 보관함) */}
              <button
                type="button"
                className="btn-outline"
                disabled={submitting || !caption}
                onClick={saveDraft}
              >
                {t('caption.saveDraft')}
              </button>
            </div>
          </div>
        </div>

        {/* 개선백로그 🟡: 캡션 보관함 모달 */}
        {vaultOpen && (
          <div
            className="calendar-modal-overlay"
            role="presentation"
            onClick={() => setVaultOpen(false)}
          >
            <div
              className="calendar-modal caption-vault-modal"
              role="dialog"
              aria-modal="true"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="calendar-modal-body">
                <h2 className="caption-vault-title">{t('caption.vault.title')}</h2>
                {vaultLoading ? (
                  <Spinner label={t('common.loading')} />
                ) : vault.length === 0 ? (
                  <p className="muted">{t('caption.vault.empty')}</p>
                ) : (
                  <ul className="caption-vault-list">
                    {vault.map((item) => (
                      <li key={item.id} className="caption-vault-item">
                        <p className="caption-vault-text">
                          {item.caption.slice(0, 80)}
                          {item.hashtags.length > 0 && (
                            <span className="ig-preview-hashtags"> {item.hashtags.slice(0, 3).join(' ')}{item.hashtags.length > 3 ? ' …' : ''}</span>
                          )}
                        </p>
                        <div className="caption-vault-actions">
                          <button type="button" className="btn-primary" onClick={() => useSaved(item)}>
                            {t('caption.vault.use')}
                          </button>
                          <button type="button" className="btn-danger" onClick={() => removeSaved(item.id)}>
                            {t('common.delete')}
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
                <div className="calendar-modal-actions">
                  <button type="button" className="btn-outline" onClick={() => setVaultOpen(false)}>
                    {t('common.cancel')}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
