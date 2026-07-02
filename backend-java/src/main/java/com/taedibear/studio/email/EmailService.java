package com.taedibear.studio.email;

import com.taedibear.studio.domain.Post;
import com.taedibear.studio.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// Phase 2-2: 예약 업로드 최종 실패 시 이메일 알림
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@taedibear.studio}")
    private String fromAddress;

    @Value("${app.mail.admin:}")
    private String adminEmail;

    @Value("${app.client-url:http://localhost:5173}")
    private String clientUrl;

    /**
     * 예약 업로드 최종 실패 알림 이메일 발송.
     * 3회 재시도 후에도 실패했을 때 호출된다.
     * @Async 로 메일 I/O가 스케줄러 스레드를 블로킹하지 않게 한다.
     */
    @Async
    public void sendUploadFailureNotification(Post post, User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[Taedibear Studio] 예약 업로드가 실패했어요");
            helper.setText(buildHtml(post, user), true);

            mailSender.send(message);
            log.info("[Email] 업로드 실패 알림 발송 완료 → {} (postId={})", user.getEmail(), post.getId());
        } catch (MessagingException e) {
            log.error("[Email] 발송 실패 (postId={}, to={}): {}", post.getId(), user.getEmail(), e.getMessage());
        }
    }

    private String buildHtml(Post post, User user) {
        String historyUrl = clientUrl + "/history";
        String thumbnailHtml = (post.getImageUrl() != null && !post.getImageUrl().isBlank())
                ? "<img src=\"" + post.getImageUrl() + "\" alt=\"게시물 이미지\" style=\"max-width:360px;border-radius:12px;display:block;margin:16px auto;\">"
                : "";
        String captionHtml = (post.getCaption() != null && !post.getCaption().isBlank())
                ? "<p style=\"color:#555;font-size:14px;background:#f7f7fb;border-radius:8px;padding:12px 16px;\">"
                  + escapeHtml(post.getCaption()) + "</p>"
                : "";

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="font-family:'Apple SD Gothic Neo',sans-serif;color:#222;max-width:480px;margin:0 auto;padding:24px;">
                  <h2 style="font-size:20px;">😢 예약 업로드가 실패했어요</h2>
                  <p>안녕하세요, <strong>%s</strong>님.<br>
                  예약하셨던 게시물이 3회 시도 후에도 업로드에 실패했어요.</p>
                  %s
                  %s
                  <p style="margin-top:20px;">
                    <a href="%s" style="display:inline-block;background:#6c47ff;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600;">
                      히스토리에서 재시도하기
                    </a>
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                  <p style="font-size:12px;color:#999;">
                    Taedibear Studio · 문의: support@taedibear.studio
                  </p>
                </body>
                </html>
                """.formatted(
                escapeHtml(user.getName()),
                thumbnailHtml,
                captionHtml,
                historyUrl
        );
    }

    /**
     * 관리자용 일일 실패 현황 요약 이메일.
     * ADMIN_EMAIL 미설정 시 발송 스킵.
     */
    @Async
    public void sendAdminDailyReport(int failedCount, int retrySuccessCount, int finalFailedCount) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.info("[Email] ADMIN_EMAIL 미설정 — 관리자 리포트 발송 스킵");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(adminEmail);
            helper.setSubject("[Taedibear Studio] 일일 업로드 실패 현황");
            helper.setText(buildAdminReportHtml(failedCount, retrySuccessCount, finalFailedCount), true);
            mailSender.send(message);
            log.info("[Email] 관리자 리포트 발송 완료 → {}", adminEmail);
        } catch (MessagingException e) {
            log.error("[Email] 관리자 리포트 발송 실패: {}", e.getMessage());
        }
    }

    private String buildAdminReportHtml(int failedCount, int retrySuccessCount, int finalFailedCount) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="font-family:'Apple SD Gothic Neo',sans-serif;color:#222;max-width:480px;margin:0 auto;padding:24px;">
                  <h2 style="font-size:20px;">📊 일일 업로드 실패 현황</h2>
                  <table style="width:100%%;border-collapse:collapse;margin:16px 0;">
                    <tr style="background:#f7f7fb;">
                      <td style="padding:10px 16px;border:1px solid #eee;">실패 발생 건수</td>
                      <td style="padding:10px 16px;border:1px solid #eee;font-weight:700;">%d건</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 16px;border:1px solid #eee;">재시도 성공 건수</td>
                      <td style="padding:10px 16px;border:1px solid #eee;font-weight:700;color:#2e9e44;">%d건</td>
                    </tr>
                    <tr style="background:#fff5f5;">
                      <td style="padding:10px 16px;border:1px solid #eee;">최종 실패 건수</td>
                      <td style="padding:10px 16px;border:1px solid #eee;font-weight:700;color:#d33;">%d건</td>
                    </tr>
                  </table>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                  <p style="font-size:12px;color:#999;">Taedibear Studio 자동 발송</p>
                </body>
                </html>
                """.formatted(failedCount, retrySuccessCount, finalFailedCount);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
