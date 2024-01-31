package se.sundsvall.supportmanagement;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import se.sundsvall.dept44.ServiceApplication;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@ServiceApplication
@EnableCaching
@EnableFeignClients
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class Application {
	public static void main(String... args) {
		run(Application.class, args);
	}
}
