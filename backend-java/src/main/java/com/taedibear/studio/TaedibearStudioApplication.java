package com.taedibear.studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: 예약 발행(scheduled.ScheduledPublishJob)이 1분마다 동작하도록 한다.
// (Node 버전의 services/cron.service.js와 동일한 polling 방식)
@SpringBootApplication
@EnableScheduling
public class TaedibearStudioApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaedibearStudioApplication.class, args);
	}
}
