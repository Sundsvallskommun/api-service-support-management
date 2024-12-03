package se.sundsvall.supportmanagement.config;

import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SemaphoreConfig {

	@Value("${semaphore.maxPermits}")
	private int maxPermits;

	@Bean
	public Semaphore semaphore() {
		return new Semaphore(maxPermits);
	}
}
