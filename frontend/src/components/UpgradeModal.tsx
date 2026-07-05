import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

interface UpgradeModalProps {
  open: boolean;
  onClose: () => void;
}

// 업로드 한도 초과 시 노출되는 Pro 업그레이드 유도 모달 (Phase 2-1)
export default function UpgradeModal({ open, onClose }: UpgradeModalProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  if (!open) return null;

  const handleUpgrade = () => {
    onClose();
    navigate('/settings#plan');
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal upgrade-modal" onClick={(e) => e.stopPropagation()}>
        <div className="upgrade-modal-icon">🚀</div>
        <h2>{t('plan.limitReachedTitle')}</h2>
        <p className="upgrade-modal-desc">{t('plan.limitReachedDesc')}</p>

        <div className="upgrade-modal-plans">
          <div className="upgrade-plan-card upgrade-plan-free">
            <span className="upgrade-plan-name">{t('plan.free.name')}</span>
            <span className="upgrade-plan-price">₩0</span>
            <span className="upgrade-plan-feature">{t('plan.limitReachedFreeFeature')}</span>
          </div>
          <div className="upgrade-plan-card upgrade-plan-pro">
            <span className="upgrade-plan-badge">PRO</span>
            <span className="upgrade-plan-name">{t('plan.pro.name')}</span>
            <span className="upgrade-plan-price">₩9,900<small>/월</small></span>
            <span className="upgrade-plan-feature">{t('plan.limitReachedProFeature')}</span>
          </div>
        </div>

        <div className="upgrade-modal-actions">
          <button type="button" className="btn-primary" onClick={handleUpgrade}>
            {t('plan.upgrade')}
          </button>
          <button type="button" className="btn-outline" onClick={onClose}>
            {t('common.cancel')}
          </button>
        </div>
      </div>
    </div>
  );
}
