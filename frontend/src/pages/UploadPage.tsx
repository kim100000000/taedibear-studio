import { useEffect, useRef, useState, type DragEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import { listInstagramAccounts } from '../api/instagram';
import type { InstagramAccount } from '../types';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_SIZE = 10 * 1024 * 1024;

// P-05 사진 업로드 (docs/03_화면설계서.md)
export default function UploadPage() {
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [accountId, setAccountId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const { t } = useTranslation();

  useEffect(() => {
    listInstagramAccounts()
      .then((res) => {
        setAccounts(res.data.data);
        if (res.data.data.length > 0) setAccountId(String(res.data.data[0].id));
      })
      .finally(() => setLoadingAccounts(false));
  }, []);

  const pickFile = (selected: File | null | undefined) => {
    if (!selected) return;
    if (!ALLOWED_TYPES.includes(selected.type)) {
      setError(t('upload.err.fileType'));
      return;
    }
    if (selected.size > MAX_SIZE) {
      setError(t('upload.err.fileSize'));
      return;
    }
    setError('');
    setFile(selected);
    setPreviewUrl(URL.createObjectURL(selected));
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    pickFile(e.dataTransfer.files?.[0]);
  };

  const handleNext = () => {
    if (!file) {
      setError(t('upload.err.selectImage'));
      return;
    }
    if (!accountId) {
      setError(t('upload.err.selectAccount'));
      return;
    }
    // 업로드는 ImageEditorPage에서 편집 완료 후 수행
    navigate('/upload/edit', {
      state: { fileUrl: previewUrl, accountId: Number(accountId) },
    });
  };

  if (loadingAccounts) return <Spinner label={t('common.loading')} />;

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content upload-page">
        <h1>{t('upload.title')}</h1>

        {accounts.length === 0 ? (
          <div className="banner">
            {t('upload.noAccountBanner')}{' '}
            <Link to="/settings">{t('upload.noAccountLink')}</Link>
          </div>
        ) : (
          <div className="form-field">
            <label>{t('upload.accountLabel')}</label>
            <select value={accountId} onChange={(e) => setAccountId(e.target.value)}>
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  @{acc.username}
                </option>
              ))}
            </select>
          </div>
        )}

        <div
          className={`dropzone ${dragOver ? 'dropzone-active' : ''}`}
          onDragOver={(e) => {
            e.preventDefault();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          {previewUrl ? (
            <img src={previewUrl} alt={t('upload.dropzone.preview')} className="dropzone-preview" />
          ) : (
            <>
              <p>{t('upload.dropzone.hint')}</p>
              <p className="muted">{t('upload.dropzone.spec')}</p>
            </>
          )}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            style={{ display: 'none' }}
            onChange={(e) => pickFile(e.target.files?.[0])}
          />
        </div>

        {error && <p className="error">{error}</p>}

        <button
          type="button"
          className="btn-primary"
          disabled={accounts.length === 0}
          onClick={handleNext}
        >
          {t('upload.next')}
        </button>
      </div>
    </div>
  );
}
