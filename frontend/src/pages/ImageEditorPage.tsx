import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import Toast from '../components/Toast';
import { uploadImage } from '../api/posts';
import type { ToastData } from '../types';

// P-12 AI 이미지 편집 (/upload/edit) — Phase 1-2
// 백엔드 API 추가 없음. Canvas API로 편집 후 기존 POST /api/posts/upload 활용.

interface EditorPageState {
  fileUrl: string;   // URL.createObjectURL() 값
  accountId: number;
}

interface FilterPreset {
  key: string;
  labelKey: string;
  brightness: number;
  contrast: number;
  saturation: number;
}

const FILTER_PRESETS: FilterPreset[] = [
  { key: 'emotional', labelKey: 'editor.filter.emotional', brightness: 110, contrast: 90,  saturation: 75  },
  { key: 'bright',    labelKey: 'editor.filter.bright',    brightness: 130, contrast: 105, saturation: 110 },
  { key: 'vintage',   labelKey: 'editor.filter.vintage',   brightness: 105, contrast: 85,  saturation: 55  },
  { key: 'vivid',     labelKey: 'editor.filter.vivid',     brightness: 100, contrast: 125, saturation: 145 },
  { key: 'warm',      labelKey: 'editor.filter.warm',      brightness: 115, contrast: 100, saturation: 105 },
];

type CropRatio = 'original' | '1:1' | '4:5' | '1.91:1';

const CROP_RATIOS: { key: CropRatio; labelKey: string }[] = [
  { key: 'original', labelKey: 'editor.crop.original'  },
  { key: '1:1',      labelKey: 'editor.crop.square'    },
  { key: '4:5',      labelKey: 'editor.crop.portrait'  },
  { key: '1.91:1',   labelKey: 'editor.crop.landscape' },
];

const RATIO_VALUES: Record<CropRatio, number | null> = {
  'original': null,
  '1:1':      1,
  '4:5':      4 / 5,
  '1.91:1':   1.91,
};

const DEFAULTS = { brightness: 100, contrast: 100, saturation: 100 };

function getCropBox(
  imgW: number,
  imgH: number,
  ratio: CropRatio,
): { sx: number; sy: number; sw: number; sh: number } {
  const r = RATIO_VALUES[ratio];
  if (!r) return { sx: 0, sy: 0, sw: imgW, sh: imgH };
  const imgRatio = imgW / imgH;
  if (imgRatio > r) {
    const sw = imgH * r;
    return { sx: (imgW - sw) / 2, sy: 0, sw, sh: imgH };
  }
  const sh = imgW / r;
  return { sx: 0, sy: (imgH - sh) / 2, sw: imgW, sh };
}

// 서버 multipart 한도(application.yml max-file-size: 10MB)와 동일하게 유지
const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

export default function ImageEditorPage() {
  const { state } = useLocation() as { state: EditorPageState | null };
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [brightness, setBrightness] = useState(DEFAULTS.brightness);
  const [contrast, setContrast]     = useState(DEFAULTS.contrast);
  const [saturation, setSaturation] = useState(DEFAULTS.saturation);
  const [cropRatio, setCropRatio]   = useState<CropRatio>('1:1');
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [imgLoaded, setImgLoaded]   = useState(false);
  const [uploading, setUploading]   = useState(false);
  const [uploadPercent, setUploadPercent] = useState(0);
  const [toast, setToast]           = useState<ToastData | null>(null);

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imgRef    = useRef<HTMLImageElement | null>(null);

  // 잘못된 진입 처리
  useEffect(() => {
    if (!state?.fileUrl) navigate('/upload', { replace: true });
  }, [state, navigate]);

  // 원본 이미지 로드
  useEffect(() => {
    if (!state?.fileUrl) return;
    const img = new Image();
    img.onload = () => {
      imgRef.current = img;
      setImgLoaded(true);
    };
    img.src = state.fileUrl;
  }, [state?.fileUrl]);

  // ── 캔버스 렌더링 ────────────────────────────────────────────────────────────
  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    const img = imgRef.current;
    if (!canvas || !img) return;

    const { sx, sy, sw, sh } = getCropBox(img.naturalWidth, img.naturalHeight, cropRatio);

    // 미리보기 크기 (최대 480px)
    const MAX = 480;
    const displayRatio = sw / sh;
    let displayW: number, displayH: number;
    if (displayRatio >= 1) {
      displayW = Math.min(sw, MAX);
      displayH = displayW / displayRatio;
    } else {
      displayH = Math.min(sh, MAX);
      displayW = displayH * displayRatio;
    }

    canvas.width  = displayW;
    canvas.height = displayH;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.filter = `brightness(${brightness}%) contrast(${contrast}%) saturate(${saturation}%)`;
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, displayW, displayH);
  }, [brightness, contrast, saturation, cropRatio]);

  useEffect(() => {
    if (imgLoaded) draw();
  }, [imgLoaded, draw]);

  // ── 필터 프리셋 적용 ──────────────────────────────────────────────────────────
  const applyPreset = (preset: FilterPreset) => {
    setBrightness(preset.brightness);
    setContrast(preset.contrast);
    setSaturation(preset.saturation);
    setActiveFilter(preset.key);
  };

  const reset = () => {
    setBrightness(DEFAULTS.brightness);
    setContrast(DEFAULTS.contrast);
    setSaturation(DEFAULTS.saturation);
    setActiveFilter(null);
  };

  // ── 다음: 편집본 내보내기 → 업로드 → CaptionPage 이동 ──────────────────────
  const handleNext = async () => {
    const img = imgRef.current;
    if (!img || !state) return;

    // 원본 해상도 캔버스로 내보내기
    const { sx, sy, sw, sh } = getCropBox(img.naturalWidth, img.naturalHeight, cropRatio);
    const exportCanvas = document.createElement('canvas');
    exportCanvas.width  = sw;
    exportCanvas.height = sh;
    const ctx = exportCanvas.getContext('2d');
    if (!ctx) return;
    ctx.filter = `brightness(${brightness}%) contrast(${contrast}%) saturate(${saturation}%)`;
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, sw, sh);

    setUploading(true);
    setUploadPercent(0);
    try {
      const toJpeg = (quality: number) =>
        new Promise<Blob>((resolve, reject) => {
          exportCanvas.toBlob(
            (b) => (b ? resolve(b) : reject(new Error('toBlob failed'))),
            'image/jpeg',
            quality,
          );
        });

      // 개선백로그 🟠: 서버 multipart 한도(10MB) 초과로 즉시 실패하던 문제 —
      // 편집본이 10MB를 넘으면 품질을 낮춰 재인코딩, 그래도 크면 명확한 안내
      let blob = await toJpeg(0.92);
      if (blob.size > MAX_UPLOAD_BYTES) blob = await toJpeg(0.75);
      if (blob.size > MAX_UPLOAD_BYTES) {
        setToast({ type: 'error', message: t('editor.err.tooLarge') });
        return;
      }

      const editedFile = new File([blob], 'edited.jpg', { type: 'image/jpeg' });
      const { data } = await uploadImage(editedFile, setUploadPercent);
      navigate('/upload/caption', {
        state: { imageUrl: data.data.image_url, instagramAccountId: state.accountId },
      });
    } catch (err: any) {
      // 개선백로그 🟠: 실패 원인을 구분해 안내 (네트워크 / 파일 크기 / 서버 메시지)
      const message = err.isNetworkError
        ? t('error.network')
        : err.response?.status === 413
          ? t('editor.err.tooLarge')
          : err.response?.data?.error ?? t('editor.err.uploadFailed');
      setToast({ type: 'error', message });
    } finally {
      setUploading(false);
    }
  };

  if (!state?.fileUrl) return null;

  const SLIDERS = [
    { key: 'brightness', labelKey: 'editor.brightness', value: brightness, setter: (v: number) => { setBrightness(v); setActiveFilter(null); } },
    { key: 'contrast',   labelKey: 'editor.contrast',   value: contrast,   setter: (v: number) => { setContrast(v);   setActiveFilter(null); } },
    { key: 'saturation', labelKey: 'editor.saturation', value: saturation, setter: (v: number) => { setSaturation(v); setActiveFilter(null); } },
  ];

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content editor-page">
        <h1>{t('editor.title')}</h1>

        <div className="editor-layout">
          {/* ── 캔버스 미리보기 ── */}
          <div className="editor-canvas-wrap">
            {!imgLoaded && <Spinner label={t('common.loading')} />}
            <canvas
              ref={canvasRef}
              className="editor-canvas"
              style={{ display: imgLoaded ? 'block' : 'none' }}
            />
          </div>

          {/* ── 컨트롤 패널 ── */}
          <div className="editor-controls">

            {/* 비율 크롭 */}
            <section className="editor-section">
              <p className="editor-section-label">{t('editor.cropLabel')}</p>
              <div className="editor-chip-group">
                {CROP_RATIOS.map(({ key, labelKey }) => (
                  <button
                    key={key}
                    type="button"
                    className={`editor-chip${cropRatio === key ? ' editor-chip-active' : ''}`}
                    onClick={() => setCropRatio(key)}
                  >
                    {t(labelKey)}
                  </button>
                ))}
              </div>
            </section>

            {/* 필터 프리셋 */}
            <section className="editor-section">
              <p className="editor-section-label">{t('editor.filterLabel')}</p>
              <div className="editor-chip-group">
                {FILTER_PRESETS.map((preset) => (
                  <button
                    key={preset.key}
                    type="button"
                    className={`editor-chip${activeFilter === preset.key ? ' editor-chip-active' : ''}`}
                    onClick={() => applyPreset(preset)}
                  >
                    {t(preset.labelKey)}
                  </button>
                ))}
              </div>
            </section>

            {/* 조정 슬라이더 */}
            <section className="editor-section">
              <p className="editor-section-label">{t('editor.adjustLabel')}</p>
              {SLIDERS.map(({ key, labelKey, value, setter }) => (
                <div key={key} className="editor-slider-row">
                  <span className="editor-slider-label">{t(labelKey)}</span>
                  <input
                    type="range"
                    min={0}
                    max={200}
                    value={value}
                    onChange={(e) => setter(Number(e.target.value))}
                    className="editor-slider"
                  />
                  <span className="editor-slider-value">{value}</span>
                </div>
              ))}
            </section>

            {/* 액션 버튼 */}
            <div className="editor-actions">
              <button type="button" className="btn-outline" onClick={reset}>
                {t('editor.reset')}
              </button>
              <button
                type="button"
                className="btn-primary"
                disabled={uploading || !imgLoaded}
                onClick={handleNext}
              >
                {uploading ? t('editor.uploading') : t('editor.next')}
              </button>
            </div>
            {/* 개선백로그 🟠: 업로드 진행률 표시 */}
            {uploading && (
              <div className="upload-progress" role="progressbar" aria-valuenow={uploadPercent}>
                <div className="upload-progress-track">
                  <div className="upload-progress-fill" style={{ width: `${uploadPercent}%` }} />
                </div>
                <span className="upload-progress-label">
                  {t('editor.uploading')} {uploadPercent}%
                </span>
              </div>
            )}
          </div>
        </div>
      </div>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
