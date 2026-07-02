import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { completeOnboarding } from '../api/users';

interface OnboardingStep {
  emoji: string;
  title: string;
  desc: string;
}

interface OnboardingModalProps {
  onClose: () => void;
}

// 신규 사용자 온보딩 (5단계 부가 기능). 대시보드 첫 방문 시 1회 노출.
export default function OnboardingModal({ onClose }: OnboardingModalProps) {
  const { t } = useTranslation();
  const [stepIndex, setStepIndex] = useState(0);

  const steps = t('onboarding.steps', { returnObjects: true }) as OnboardingStep[];
  const step = steps[stepIndex];
  const isLastStep = stepIndex === steps.length - 1;

  const handleNext = () => {
    if (isLastStep) {
      // Phase 2-1: 온보딩 완료 시 +2 크레딧 지급 (실패해도 UX 방해 안 함)
      completeOnboarding().catch(() => {});
      onClose();
      return;
    }
    setStepIndex((prev) => prev + 1);
  };

  const handlePrev = () => {
    setStepIndex((prev) => Math.max(0, prev - 1));
  };

  return (
    <div className="onboarding-overlay">
      <div className="onboarding-box">
        <div className="onboarding-emoji">{step.emoji}</div>
        <h2>{step.title}</h2>
        <p>{step.desc}</p>

        <div className="onboarding-dots">
          {steps.map((s, i) => (
            <span
              key={s.title}
              className={`onboarding-dot ${i === stepIndex ? 'onboarding-dot-active' : ''}`}
            />
          ))}
        </div>

        <div className="onboarding-actions">
          {stepIndex > 0 && (
            <button type="button" className="btn-outline" onClick={handlePrev}>
              {t('onboarding.prev')}
            </button>
          )}
          <button type="button" className="btn-primary" onClick={handleNext}>
            {isLastStep ? t('onboarding.start') : t('onboarding.next')}
          </button>
        </div>

        {!isLastStep && (
          <button type="button" className="onboarding-skip" onClick={onClose}>
            {t('onboarding.skip')}
          </button>
        )}
      </div>
    </div>
  );
}
