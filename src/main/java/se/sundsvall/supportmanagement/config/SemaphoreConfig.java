package se.sundsvall.supportmanagement.config;

import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SemaphoreConfig {

	@Bean
	Semaphore semaphore(@Value("${semaphore.maxMemoryUsage}") final int maxMemoryUsage) {
		return new Semaphore(maxMemoryUsage);
	}
}
