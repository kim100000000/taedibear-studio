interface ConfirmModalProps {
  open: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}

// 삭제 확인 / 연동 해제 확인용 모달 (docs/03_화면설계서.md 3. 공통 컴포넌트)
export default function ConfirmModal({ open, title, message, onConfirm, onCancel }: ConfirmModalProps) {
  if (!open) return null;

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        <p>{message}</p>
        <div className="modal-actions">
          <button type="button" className="btn-outline" onClick={onCancel}>
            취소
          </button>
          <button type="button" className="btn-danger" onClick={onConfirm}>
            확인
          </button>
        </div>
      </div>
    </div>
  );
}
