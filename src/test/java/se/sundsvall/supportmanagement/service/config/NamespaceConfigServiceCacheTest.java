package se.sundsvall.supportmanagement.service.config;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class NamespaceConfigServiceCacheTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";

	private NamespaceConfigService mock;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	@Autowired
	private CacheManager cacheManager;

	// Provides a mock implementation for the service and a cache manager
	@EnableCaching
	@Configuration
	public static class CachingTestConfig {

		@Bean
		public NamespaceConfigService namespaceConfigServiceMockImplementation() {
			return mock(NamespaceConfigService.class);
		}

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("namespaceConfigCache");
		}
	}

	@BeforeEach
	void setUp() {
		// NamespaceConfigService is a proxy around our mock, so the mock itself is retrieved to verify against
		mock = AopTestUtils.getTargetObject(namespaceConfigService);

		// The context, and with it the cache, is loaded once for the whole class
		reset(mock);
		cacheManager.getCacheNames().forEach(name -> requireNonNull(cacheManager.getCache(name)).clear());
	}

	@Test
	void isAccessControlActiveIsAnsweredFromCache() {
		// Asked on every request reaching a namespace scoped resource, so it may not hit the database each time.
		when(mock.isAccessControlActive(any(), any()))
			.thenReturn(true)
			.thenThrow(new RuntimeException("Result should be cached!"));

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		verify(mock).isAccessControlActive(NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		verifyNoMoreInteractions(mock);
	}

	@Test
	void createEvictsTheAccessControlAnswer() {
		when(mock.isAccessControlActive(any(), any())).thenReturn(false, true);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isFalse();
		namespaceConfigService.create(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
	}

	@Test
	void replaceEvictsTheAccessControlAnswer() {
		when(mock.isAccessControlActive(any(), any())).thenReturn(true, false);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		namespaceConfigService.replace(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isFalse();
	}

	@Test
	void deleteEvictsTheAccessControlAnswer() {
		when(mock.isAccessControlActive(any(), any())).thenReturn(true, false);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isTrue();
		namespaceConfigService.delete(NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).isFalse();
	}

	@Test
	void createEvictsTheConfiguration() {
		final var before = NamespaceConfig.create().withDisplayName("before");
		final var after = NamespaceConfig.create().withDisplayName("after");
		when(mock.get(any(), any())).thenReturn(before, after);

		assertThat(namespaceConfigService.get(NAMESPACE, MUNICIPALITY_ID)).isEqualTo(before);
		namespaceConfigService.create(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.get(NAMESPACE, MUNICIPALITY_ID)).isEqualTo(after);
	}

	@Test
	void replaceEvictsTheConfiguration() {
		final var before = NamespaceConfig.create().withDisplayName("before");
		final var after = NamespaceConfig.create().withDisplayName("after");
		when(mock.get(any(), any())).thenReturn(before, after);

		assertThat(namespaceConfigService.get(NAMESPACE, MUNICIPALITY_ID)).isEqualTo(before);
		namespaceConfigService.replace(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.get(NAMESPACE, MUNICIPALITY_ID)).isEqualTo(after);
	}

	@Test
	void deleteEvictsTheConfiguration() {
		final var before = NamespaceConfig.create().withDisplayName("before");
		final var after = NamespaceConfig.create().withDisplayName("after");
		when(mock.get(any(), any())).thenReturn(before, after);

		assertThat(namespaceConfigService.get(NAMESPACE, MUNICIPALITY_ID)).isEqualTo(before);
		namespaceConfigService.delete(NAMESPACE, MUNICIPALITY_ID);

		assertThat(namespaceConfigService.get(NAMESPACE, MUNICIPALITY_ID)).isEqualTo(after);
	}

	@Test
	void theProcessConfigurationIsAnsweredFromCache() {
		when(mock.getProcessConsumer(any(), any())).thenReturn(Optional.of("pw-alkt")).thenThrow(new RuntimeException("Result should be cached!"));
		when(mock.getProcessTriggers(any(), any())).thenReturn(Set.of(ERRAND)).thenThrow(new RuntimeException("Result should be cached!"));

		assertThat(namespaceConfigService.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).hasValue("pw-alkt");
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).containsExactly(ERRAND);

		assertThat(namespaceConfigService.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).hasValue("pw-alkt");
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).containsExactly(ERRAND);
	}

	@Test
	void createEvictsTheProcessConfiguration() {
		givenProcessConfigurationChanges();

		askForTheProcessConfiguration();
		namespaceConfigService.create(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertTheChangedProcessConfigurationIsRead();
	}

	@Test
	void replaceEvictsTheProcessConfiguration() {
		givenProcessConfigurationChanges();

		askForTheProcessConfiguration();
		namespaceConfigService.replace(NamespaceConfig.create(), NAMESPACE, MUNICIPALITY_ID);

		assertTheChangedProcessConfigurationIsRead();
	}

	@Test
	void deleteEvictsTheProcessConfiguration() {
		givenProcessConfigurationChanges();

		askForTheProcessConfiguration();
		namespaceConfigService.delete(NAMESPACE, MUNICIPALITY_ID);

		assertTheChangedProcessConfigurationIsRead();
	}

	private void givenProcessConfigurationChanges() {
		when(mock.getProcessConsumer(any(), any())).thenReturn(Optional.empty(), Optional.of("pw-alkt"));
		when(mock.getProcessTriggers(any(), any())).thenReturn(Set.of(), Set.of(ERRAND));
	}

	private void askForTheProcessConfiguration() {
		assertThat(namespaceConfigService.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).isEmpty();
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).isEmpty();
	}

	private void assertTheChangedProcessConfigurationIsRead() {
		assertThat(namespaceConfigService.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).hasValue("pw-alkt");
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).containsExactly(ERRAND);
	}
}
