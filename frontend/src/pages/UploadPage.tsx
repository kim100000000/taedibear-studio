import { useEffect, useRef, useState, type DragEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import { uploadImage } from '../api/posts';
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
  const [uploading, setUploading] = useState(false);
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

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
      setError('지원하지 않는 파일 형식이에요. JPG, PNG, WEBP만 가능해요.');
      return;
    }
    if (selected.size > MAX_SIZE) {
      setError('파일 크기는 최대 10MB까지 업로드할 수 있어요.');
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

  const handleNext = async () => {
    if (!file) {
      setError('이미지를 선택해주세요.');
      return;
    }
    if (!accountId) {
      setError('업로드할 인스타그램 계정을 선택해주세요.');
      return;
    }

    setUploading(true);
    setError('');
    try {
      const { data } = await uploadImage(file);
      navigate('/upload/caption', {
        state: { imageUrl: data.data.image_url, instagramAccountId: Number(accountId) },
      });
    } catch (err: any) {
      setError(err.response?.data?.error || '업로드에 실패했어요.');
    } finally {
      setUploading(false);
    }
  };

  if (loadingAccounts) return <Spinner label="불러오는 중..." />;

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content upload-page">
        <h1>사진 업로드</h1>

        {accounts.length === 0 ? (
          <div className="banner">
            먼저 인스타그램 계정을 연동해야 게시물을 만들 수 있어요.{' '}
            <Link to="/settings">설정에서 연동하기</Link>
          </div>
        ) : (
          <div className="form-field">
            <label>업로드할 인스타그램 계정</label>
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
            <img src={previewUrl} alt="미리보기" className="dropzone-preview" />
          ) : (
            <>
              <p>이미지를 드래그하거나 클릭해서 선택해주세요</p>
              <p className="muted">JPG, PNG, WEBP / 최대 10MB / 1080x1080 권장</p>
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
          disabled={uploading || accounts.length === 0}
          onClick={handleNext}
        >
          {uploading ? '업로드 중...' : '다음'}
        </button>
      </div>
    </div>
  );
}
