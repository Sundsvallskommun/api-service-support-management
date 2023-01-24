package se.sundsvall.supportmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

import se.sundsvall.dept44.ServiceApplication;

@ServiceApplication
@EnableCaching
@EnableFeignClients
public class Application {
	public static void main(final String... args) {
		SpringApplication.run(Application.class, args);
	}
}
