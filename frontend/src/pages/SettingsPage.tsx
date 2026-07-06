import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../components/NavBar';
import Spinner from '../components/Spinner';
import ConfirmModal from '../components/ConfirmModal';
import Toast from '../components/Toast';
import PlanSection from '../components/PlanSection';
import { useAuth } from '../context/AuthContext';
import { updateMe, deleteAccount } from '../api/users';
import { listInstagramAccounts, disconnectInstagramAccount, getInstagramConnectUrl, setAutoReply } from '../api/instagram';
import type { InstagramAccount, ToastData } from '../types';

// P-10 설정 (docs/03_화면설계서.md)
export default function SettingsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { t } = useTranslation();

  const [name, setName] = useState(user?.name || '');
  const [savingName, setSavingName] = useState(false);
  const [accounts, setAccounts] = useState<InstagramAccount[]>([]);
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [disconnectTarget, setDisconnectTarget] = useState<number | null>(null);
  const [toast, setToast] = useState<ToastData | null>(null);
  const [planRefresh, setPlanRefresh] = useState(0);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    listInstagramAccounts()
      .then((res) => setAccounts(res.data.data))
      .catch((err: any) => {
        const msg = err.isNetworkError
          ? t('error.network')
          : err.response?.data?.error || t('error.loadFailed');
        setToast({ type: 'error', message: msg });
      })
      .finally(() => setLoadingAccounts(false));
  }, [t]);

  // 결제 완료 후 리다이렉트 (?upgraded=true)
  useEffect(() => {
    if (searchParams.get('upgraded') === 'true') {
      setToast({ type: 'success', message: t('plan.upgradeSuccess') });
      setPlanRefresh((n) => n + 1);
      searchParams.delete('upgraded');
      setSearchParams(searchParams, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const connected = searchParams.get('connected');
    if (connected === 'true') {
      setToast({ type: 'success', message: t('settings.instagram.connected') });
    } else if (connected === 'false') {
      setToast({ type: 'error', message: t('settings.instagram.connectFailed') });
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
      setToast({ type: 'success', message: t('settings.profile.saved') });
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('settings.profile.saveFailed') });
    } finally {
      setSavingName(false);
    }
  };

  // Phase 4-3: 계정별 리뷰 자동 답글 사용 여부 토글
  const toggleAutoReply = async (account: InstagramAccount) => {
    const next = !account.auto_reply_enabled;
    try {
      await setAutoReply(account.id, next);
      setAccounts((prev) => prev.map((a) => (a.id === account.id ? { ...a, auto_reply_enabled: next } : a)));
      setToast({ type: 'success', message: t('settings.instagram.autoReplyChanged') });
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('settings.instagram.autoReplyChangeFailed') });
    }
  };

  const confirmDisconnect = async () => {
    if (!disconnectTarget) return;
    try {
      await disconnectInstagramAccount(disconnectTarget);
      setAccounts((prev) => prev.filter((a) => a.id !== disconnectTarget));
      setToast({ type: 'success', message: t('settings.instagram.disconnected') });
    } catch (err: any) {
      setToast({ type: 'error', message: err.response?.data?.error || t('settings.instagram.disconnectFailed') });
    } finally {
      setDisconnectTarget(null);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const confirmDeleteAccount = async () => {
    setDeleting(true);
    try {
      await deleteAccount();
      // 탈퇴 성공 → 토큰 제거 후 로그인 페이지로 (탈퇴 안내 플래그 전달)
      logout();
      navigate('/login?withdrawn=true', { replace: true });
    } catch (err: any) {
      const msg = err.isNetworkError
        ? t('error.network')
        : err.response?.data?.error || t('settings.account.deleteFailed');
      setToast({ type: 'error', message: msg });
      setShowDeleteModal(false);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="page-with-nav">
      <NavBar />
      <div className="page-content settings-page">
        <h1>{t('settings.title')}</h1>

        <section className="settings-section">
          <h2>{t('settings.profile.title')}</h2>
          <div className="form-field">
            <label>{t('settings.profile.email')}</label>
            <input type="text" value={user?.email || ''} disabled />
          </div>
          <div className="form-field">
            <label>{t('settings.profile.name')}</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <button type="button" className="btn-outline" disabled={savingName} onClick={saveName}>
            {t('settings.profile.save')}
          </button>
        </section>

        <PlanSection onToast={setToast} refreshTrigger={planRefresh} />

        <section className="settings-section">
          <h2>{t('settings.instagram.title')}</h2>
          {loadingAccounts ? (
            <Spinner label={t('common.loading')} />
          ) : accounts.length === 0 ? (
            <p className="muted">{t('settings.instagram.noAccounts')}</p>
          ) : (
            <ul className="account-list">
              {accounts.map((acc) => (
                <li key={acc.id} className="account-list-item">
                  <span>@{acc.username}</span>
                  <button
                    type="button"
                    className="btn-outline"
                    title={t('settings.instagram.autoReplyLabel')}
                    onClick={() => toggleAutoReply(acc)}
                  >
                    {t('settings.instagram.autoReplyLabel')}:{' '}
                    {acc.auto_reply_enabled
                      ? t('settings.instagram.autoReplyOn')
                      : t('settings.instagram.autoReplyOff')}
                  </button>
                  <button
                    type="button"
                    className="btn-danger"
                    onClick={() => setDisconnectTarget(acc.id)}
                  >
                    {t('settings.instagram.disconnect')}
                  </button>
                </li>
              ))}
            </ul>
          )}
          <button
            type="button"
            className="btn-outline"
            onClick={async () => {
              try {
                // C6: 인증된 API로 연동 URL(nonce state)을 받아 이동 — JWT를 URL에 노출하지 않는다
                const res = await getInstagramConnectUrl();
                window.location.href = res.data.data.url;
              } catch (err: any) {
                const msg = err.isNetworkError
                  ? t('error.network')
                  : err.response?.data?.error || t('error.loadFailed');
                setToast({ type: 'error', message: msg });
              }
            }}
          >
            {t('settings.instagram.connect')}
          </button>
        </section>

        <section className="settings-section">
          <h2>{t('settings.account.title')}</h2>
          <button type="button" className="btn-outline" onClick={handleLogout}>
            {t('settings.account.logout')}
          </button>
          <button type="button" className="btn-outline" disabled title={t('settings.account.notSupported')}>
            {t('settings.account.changePassword')}
          </button>
          <button type="button" className="btn-danger" onClick={() => setShowDeleteModal(true)}>
            {t('settings.account.deleteAccount')}
          </button>
        </section>
      </div>

      <ConfirmModal
        open={!!disconnectTarget}
        title={t('settings.instagram.modal.title')}
        message={t('settings.instagram.modal.message')}
        onConfirm={confirmDisconnect}
        onCancel={() => setDisconnectTarget(null)}
      />
      <ConfirmModal
        open={showDeleteModal}
        title={t('settings.account.deleteModal.title')}
        message={t('settings.account.deleteModal.message')}
        onConfirm={confirmDeleteAccount}
        onCancel={() => !deleting && setShowDeleteModal(false)}
      />
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
