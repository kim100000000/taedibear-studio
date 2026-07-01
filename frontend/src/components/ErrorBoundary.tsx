import { Component, type ErrorInfo, type ReactNode } from 'react';
import i18n from '../i18n';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

// 화면 렌더링 중 예기치 못한 에러가 나도 흰 화면 대신 안내 화면을 보여준다.
// (docs/03_화면설계서.md 3. 공통 컴포넌트 — 에러 핸들링)
export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // eslint-disable-next-line no-console
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  handleReload = () => {
    this.setState({ hasError: false });
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="empty-state">
          <p>{i18n.t('error.boundary.message')}</p>
          <button type="button" className="btn-primary" onClick={this.handleReload}>
            {i18n.t('error.boundary.button')}
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
