import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from './ThemeToggle';

// P-04~P-10 공통 상단 네비게이션 (docs/03_화면설계서.md 3. 공통 컴포넌트)
export default function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <Link to="/dashboard" className="navbar-logo">
        🐻 Taedibear Studio
      </Link>
      <div className="navbar-menu">
        <Link to="/upload">업로드</Link>
        <Link to="/history">히스토리</Link>
        <Link to="/settings">설정</Link>
      </div>
      <div className="navbar-profile">
        <ThemeToggle />
        <span>{user?.name}님</span>
        <button type="button" onClick={handleLogout}>
          로그아웃
        </button>
      </div>
    </nav>
  );
}
