interface EmptyStateProps {
  message: string;
  actionLabel?: string;
  onAction?: () => void;
}

// 빈 상태 화면 (docs/03_화면설계서.md 3. 공통 컴포넌트)
export default function EmptyState({ message, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <p>{message}</p>
      {actionLabel && (
        <button type="button" className="btn-primary" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
