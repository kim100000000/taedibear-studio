import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import ConfirmModal from '../components/ConfirmModal';
import Toast from '../components/Toast';
import PlanSection from '../components/PlanSection';
import { useAuth } from '../context/AuthContext';
import { updateMe } from '../api/users';
import { listInstagramAccounts, disconnectInstagramAccount, getInstagramConnectUrl } from '../api/instagram';
import type { InstagramAccount, ToastData } from '../types';

// P-10 설정 (docs/03_화면설계서.md)
export default function SettingsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [name, setName] = useState(user?.name || '');
  const [savingName, setSavingName] = useState(false);
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [disconnectTarget, setDisconnectTarget] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);

  useEffect(() => {
    listInstagramAccounts()
      .then((res) => setAccounts(res.data.data))
      .finally(() => setLoadingAccounts(false));
  }, []);

  useEffect(() => {
    const connected = searchParams.get('connected');
    if (connected === 'true') {
      setToast({ type: 'success', message: '인스타그램 계정을 연동했어요.' });
    } else if (connected === 'false') {
      setToast({ type: 'error', message: '인스타그램 연동에 실패했어요.' });
    }
    if (connected !== null) {
      searchParams.delete('connected');
      setSearchParams(searchParams, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const saveName = async () => {
    setSavingName(true);
    try {
      await updateMe(name);
      setToast({ type: 'success', message: '이름을 변경했어요.' });
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || '변경에 실패했어요.' });
    } finally {
      setSavingName(false);
    }
  };

  const confirmDisconnect = async () => {
    if (!disconnectTarget) return;
    try {
      await disconnectInstagramAccount(disconnectTarget);
      setAccounts((prev) => prev.filter((a) => a.id !== disconnectTarget));
      setToast({ type: 'success', message: '연동을 해제했어요.' });
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || '해제에 실패했어요.' });
    } finally {
      setDisconnectTarget(null);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content settings-page">
        <h1>설정</h1>

        <section className="settings-section">
          <h2>프로필</h2>
          <div className="form-field">
            <label>이메일</label>
            <input type="text" value={user?.email || ''} disabled />
          </div>
          <div className="form-field">
            <label>이름</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <button type="button" className="btn-outline" disabled={savingName} onClick={saveName}>
            저장
          </button>
        </section>

        <PlanSection onToast={setToast} />

        <section className="settings-section">
          <h2>인스타그램 연동</h2>
          {loadingAccounts ? (
            <Spinner label="불러오는 중..." />
          ) : accounts.length === 0 ? (
            <p className="muted">연동된 계정이 없어요.</p>
          ) : (
            <ul className="account-list">
              {accounts.map((acc) => (
                <li key={acc.id} className="account-list-item">
                  <span>@{acc.username}</span>
                  <button
                    type="button"
                    className="btn-danger"
                    onClick={() => setDisconnectTarget(acc.id)}
                  >
                    연동 해제
                  </button>
                </li>
              ))}
            </ul>
          )}
          <button
            type="button"
            className="btn-outline"
            onClick={() => {
              window.location.href = getInstagramConnectUrl();
            }}
          >
            새 계정 연동
          </button>
        </section>

        <section className="settings-section">
          <h2>계정 관리</h2>
          <button type="button" className="btn-outline" onClick={handleLogout}>
            로그아웃
          </button>
          <button type="button" className="btn-outline" disabled title="아직 지원하지 않아요.">
            비밀번호 변경
          </button>
          <button type="button" className="btn-danger" disabled title="아직 지원하지 않아요.">
            회원 탈퇴
          </button>
        </section>
      </div>

      <ConfirmModal
        open={!!disconnectTarget}
        title="연동 해제"
        message="이 인스타그램 계정 연동을 해제할까요?"
        onConfirm={confirmDisconnect}
        onCancel={() => setDisconnectTarget(null)}
      />
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
