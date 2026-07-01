package com.taedibear.studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: 예약 발행(scheduled.ScheduledPublishJob)이 1분마다 동작하도록 한다.
// @EnableAsync: EmailService.sendUploadFailureNotification 을 별도 스레드로 실행 (Phase 2-2)
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TaedibearStudioApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaedibearStudioApplication.class, args);
	}
}
