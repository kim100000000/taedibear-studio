import { useState } from 'react';

interface OnboardingStep {
  emoji: string;
  title: string;
  description: string;
}

const STEPS: OnboardingStep[] = [
  {
    emoji: '🐻',
    title: 'Taedibear Studio에 오신 걸 환영해요',
    description: '사진 한 장으로 인스타그램 업로드까지 한 번에 끝낼 수 있어요.',
  },
  {
    emoji: '📷',
    title: '1. 사진을 올려주세요',
    description: '오늘 찍은 사진을 업로드하면 AI가 캡션과 해시태그를 만들어드려요.',
  },
  {
    emoji: '⏰',
    title: '2. 원하는 시간을 정해주세요',
    description: '지금 바로 올리거나, 원하는 시간에 자동으로 업로드되도록 예약할 수 있어요.',
  },
  {
    emoji: '🎉',
    title: '3. 끝! 이제 업로드만 기다리면 돼요',
    description: '설정에서 인스타그램 계정을 연동하면 바로 시작할 수 있어요.',
  },
];

interface OnboardingModalProps {
  onClose: () => void;
}

// 신규 사용자 온보딩 (5단계 부가 기능). 대시보드 첫 방문 시 1회 노출.
export default function OnboardingModal({ onClose }: OnboardingModalProps) {
  const [stepIndex, setStepIndex] = useState(0);
  const isLastStep = stepIndex === STEPS.length - 1;
  const step = STEPS[stepIndex];

  const handleNext = () => {
    if (isLastStep) {
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
        <p>{step.description}</p>

        <div className="onboarding-dots">
          {STEPS.map((s, i) => (
            <span key={s.title} className={`onboarding-dot ${i === stepIndex ? 'onboarding-dot-active' : ''}`} />
          ))}
        </div>

        <div className="onboarding-actions">
          {stepIndex > 0 && (
            <button type="button" className="btn-outline" onClick={handlePrev}>
              이전
            </button>
          )}
          <button type="button" className="btn-primary" onClick={handleNext}>
            {isLastStep ? '시작하기' : '다음'}
          </button>
        </div>

        {!isLastStep && (
          <button type="button" className="onboarding-skip" onClick={onClose}>
            건너뛰기
          </button>
        )}
      </div>
    </div>
  );
}
