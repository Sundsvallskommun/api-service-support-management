package se.sundsvall.supportmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class SearchReindexConfigTest {

	@Test
	void oneThreadWithRoomForOneRebuildToWait() {
		final var executor = (ThreadPoolTaskExecutor) new SearchReindexConfig().searchReindexTaskExecutor();

		assertThat(executor.getCorePoolSize()).isEqualTo(1);
		assertThat(executor.getMaxPoolSize()).isEqualTo(1);
		assertThat(executor.getQueueCapacity()).isEqualTo(1);
		assertThat(executor.getThreadNamePrefix()).isEqualTo("search-reindex-");
	}
}
