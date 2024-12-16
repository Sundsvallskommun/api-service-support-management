package se.sundsvall.supportmanagement.config;

import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SemaphoreConfig {

	@Value("${semaphore.maxMemoryUsage}")
	private int maxMemoryUsage;

	@Bean
	Semaphore semaphore() {
		return new Semaphore(maxMemoryUsage);
	}
}
