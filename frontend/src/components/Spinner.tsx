interface SpinnerProps {
  label?: string;
}

// 로딩 스피너 (docs/03_화면설계서.md 3. 공통 컴포넌트)
export default function Spinner({ label }: SpinnerProps) {
  return (
    <div className="spinner-wrap">
      <div className="spinner" />
      {label && <p>{label}</p>}
    </div>
  );
}
