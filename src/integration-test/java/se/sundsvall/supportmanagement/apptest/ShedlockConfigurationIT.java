package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.supportmanagement.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("it")
class ShedlockConfigurationIT { // Needs to be run in IT-phase as we need flyway to be enabled to verify shedlock mechanism

	@Autowired
	private LockProvider lockProvider;

	@Test
	void testAutowiring() {
		assertThat(lockProvider).isNotNull();
	}

	@Test
	void testLockMechanism() {
		final var lockConfiguration = new LockConfiguration(Instant.now(), "lockName", Duration.ofSeconds(30), Duration.ofSeconds(0));
		final var lock = lockProvider.lock(lockConfiguration);

		assertThat(lock).isPresent();
		assertThat(lockProvider.lock(lockConfiguration)).isNotPresent();

		lock.get().unlock();
	}
}
