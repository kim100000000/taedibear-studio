import { Link } from 'react-router-dom';

// P-개인정보처리방침 (docs/03_화면설계서.md)
export default function PrivacyPage() {
  return (
    <div className="page-content legal-page">
      <div className="legal-back">
        <Link to="/">← 홈으로</Link>
      </div>
      <h1>개인정보처리방침</h1>
      <p className="legal-date">최종 업데이트: 2026년 7월 2일</p>

      <section>
        <h2>1. 수집하는 개인정보 항목</h2>
        <p>서비스는 다음의 개인정보를 수집합니다.</p>
        <ul>
          <li><strong>필수:</strong> 이름, 이메일 주소, 소셜 로그인 계정 정보(구글/카카오/네이버)</li>
          <li><strong>서비스 이용 시 자동 수집:</strong> 업로드한 이미지, 작성된 캡션, 게시 히스토리, 접속 IP, 브라우저 정보</li>
          <li><strong>결제 시:</strong> 결제 수단 정보(토스페이먼츠를 통해 처리되며 당사는 카드 정보를 직접 보유하지 않습니다)</li>
        </ul>
      </section>

      <section>
        <h2>2. 개인정보 수집 및 이용 목적</h2>
        <ul>
          <li>회원 가입 및 본인 식별</li>
          <li>AI 캡션 생성 및 인스타그램 자동 업로드 서비스 제공</li>
          <li>서비스 이용 내역 관리 및 고객 지원</li>
          <li>유료 서비스 결제 처리</li>
          <li>서비스 개선을 위한 통계 분석</li>
        </ul>
      </section>

      <section>
        <h2>3. 개인정보 보유 및 이용 기간</h2>
        <p>
          이용자가 서비스를 이용하는 동안 개인정보를 보유·이용합니다. 회원 탈퇴 시 지체 없이
          파기하며, 관계 법령에 의해 보존해야 하는 경우 해당 기간 동안 별도 보관합니다.
        </p>
        <ul>
          <li>전자상거래 계약·청약 철회 기록: 5년 (전자상거래법)</li>
          <li>소비자 불만·분쟁 처리 기록: 3년 (전자상거래법)</li>
          <li>로그인 기록: 3개월 (통신비밀보호법)</li>
        </ul>
      </section>

      <section>
        <h2>4. 개인정보 제3자 제공</h2>
        <p>
          서비스는 이용자의 동의 없이 개인정보를 제3자에게 제공하지 않습니다. 단, 다음의 경우는
          예외입니다.
        </p>
        <ul>
          <li>이용자가 사전에 동의한 경우</li>
          <li>법령의 규정에 의하거나 수사 목적으로 기관의 요청이 있는 경우</li>
        </ul>
        <p>
          인스타그램 연동 기능 이용 시, Meta(Instagram) API를 통해 이용자의 계정 정보 및
          게시물 데이터가 Meta에 전달됩니다.
        </p>
      </section>

      <section>
        <h2>5. 개인정보 처리 위탁</h2>
        <p>서비스는 원활한 운영을 위해 다음과 같이 개인정보 처리를 위탁합니다.</p>
        <ul>
          <li><strong>Amazon Web Services(AWS):</strong> 서버 및 이미지 파일 저장</li>
          <li><strong>Google Gemini API:</strong> AI 캡션 생성</li>
          <li><strong>토스페이먼츠:</strong> 결제 처리</li>
        </ul>
      </section>

      <section>
        <h2>6. 이용자의 권리</h2>
        <p>이용자는 언제든지 다음의 권리를 행사할 수 있습니다.</p>
        <ul>
          <li>개인정보 열람, 수정, 삭제 요청</li>
          <li>개인정보 처리 동의 철회(회원 탈퇴)</li>
        </ul>
        <p>요청은 서비스 내 설정 페이지 또는 이메일을 통해 접수할 수 있습니다.</p>
      </section>

      <section>
        <h2>7. 쿠키 사용</h2>
        <p>
          서비스는 인증 토큰 저장을 위해 브라우저 로컬 스토리지를 사용합니다. 브라우저 설정을
          통해 이를 거부할 수 있으나, 서비스 이용이 제한될 수 있습니다.
        </p>
      </section>

      <section>
        <h2>8. 개인정보 보호책임자</h2>
        <p>개인정보 관련 문의는 아래로 연락해주세요.</p>
        <ul>
          <li><strong>이메일:</strong> privacy@taedibear.studio</li>
        </ul>
      </section>

      <div className="legal-back legal-back-bottom">
        <Link to="/">← 홈으로</Link>
      </div>
    </div>
  );
}
