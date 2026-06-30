import type { ToastData } from '../types';

interface ToastProps {
  toast: ToastData | null;
  onClose: () => void;
}

// 토스트 메시지 (docs/03_화면설계서.md 3. 공통 컴포넌트)
// 사용법: const [toast, setToast] = useState<ToastData | null>(null); 후 <Toast toast={toast} onClose={() => setToast(null)} />
export default function Toast({ toast, onClose }: ToastProps) {
  if (!toast) return null;

  return (
    <div className={`toast toast-${toast.type || 'success'}`} onClick={onClose}>
      {toast.message}
    </div>
  );
}
