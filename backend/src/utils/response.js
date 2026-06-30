// docs/05_API명세서.md 공통 응답 구조
// 성공: { success: true, data: {...} }
// 실패: { success: false, error: "메시지" }

function success(res, data, status = 200) {
  return res.status(status).json({ success: true, data });
}

function fail(res, status, message) {
  return res.status(status).json({ success: false, error: message });
}

module.exports = { success, fail };
