package se.sundsvall.supportmanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "semaphore.maxPermits=4")
class SemaphoreConfigTest {

	@Autowired
	private Semaphore semaphore;

	@Test
	void testSemaphoreConfig() {
		assertThat(semaphore).isNotNull();
		assertThat(semaphore.availablePermits()).isEqualTo(4);
	}

}
