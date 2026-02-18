package se.sundsvall.supportmanagement;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import se.sundsvall.dept44.ServiceApplication;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication
@EnableCaching
@EnableFeignClients
@EnableScheduling
public class Application {
	public static void main(String... args) {
		run(Application.class, args);
	}
}
