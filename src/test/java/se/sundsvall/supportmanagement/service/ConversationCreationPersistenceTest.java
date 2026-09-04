package se.sundsvall.supportmanagement.service;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationRequest;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.ConversationRepository;
import se.sundsvall.supportmanagement.integration.messageexchange.MessageExchangeClient;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.scheduler.messageexchange.MessageExchangeScheduler;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.supportmanagement.api.model.communication.conversation.ConversationType.INTERNAL;

/** Exercises the service proxy with separate transactions against MariaDB. */
@DataJpaTest(properties = "integration.messageexchange.namespace=draken")
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Import({
	ConversationService.class, AccessControlService.class
})
@MockitoBean(types = {
	MessageExchangeScheduler.class, CommunicationService.class, RelationClient.class, ErrandAttachmentService.class, AccessMapperService.class
})
@Sql({
	"/db/scripts/truncate.sql", "/db/scripts/testdata-junit.sql"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConversationCreationPersistenceTest {

	@Autowired
	private ConversationService service;
	@Autowired
	private ConversationRepository conversations;
	@MockitoBean
	private MessageExchangeClient exchange;
	@MockitoBean
	private NamespaceConfigService config;

	private void prepare() {
		// The namespace permits access; use the real access service, including its errand row lock.
		when(config.get("NAMESPACE.1", "2281")).thenReturn(NamespaceConfig.create());
		conversations.deleteAll(conversations.findByMunicipalityIdAndNamespaceAndErrandId("2281", "NAMESPACE.1", "ERRAND_ID-1"));
		when(exchange.getConversationById(anyString(), anyString(), anyString())).thenAnswer(invocation -> ResponseEntity.ok(new generated.se.sundsvall.messageexchange.Conversation().id(invocation.getArgument(2)).topic("Report")));
	}

	private ConversationRequest request() {
		return ConversationRequest.create().withTopic("Report").withType(INTERNAL);
	}

	@Test
	void simultaneousRequestsReturnOnePersistedConversation() throws Exception {
		prepare();
		final var firstCreating = new CountDownLatch(1);
		final var releaseCreation = new CountDownLatch(1);
		final var secondStarted = new CountDownLatch(1);
		when(exchange.createConversation(anyString(), anyString(), any())).thenAnswer(_ -> {
			firstCreating.countDown();
			if (!releaseCreation.await(5, TimeUnit.SECONDS))
				throw new IllegalStateException("Creation was not released");
			return ResponseEntity.created(URI.create("/conversations/" + randomUUID())).build();
		});
		try (final var executor = Executors.newFixedThreadPool(2)) {
			final var first = executor.submit(() -> service.createConversation("2281", "NAMESPACE.1", "ERRAND_ID-1", request()));
			try {
				assertThat(firstCreating.await(5, TimeUnit.SECONDS)).isTrue();
				final var second = executor.submit(() -> {
					secondStarted.countDown();
					return service.createConversation("2281", "NAMESPACE.1", "ERRAND_ID-1", request());
				});
				assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
				assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
				releaseCreation.countDown();
				assertThat(second.get(5, TimeUnit.SECONDS).getId()).isEqualTo(first.get(5, TimeUnit.SECONDS).getId());
			} finally {
				releaseCreation.countDown();
			}
		}
		assertThat(conversations.findByMunicipalityIdAndNamespaceAndErrandId("2281", "NAMESPACE.1", "ERRAND_ID-1")).hasSize(1);
		verify(exchange, times(1)).createConversation(anyString(), anyString(), any());
	}

	@Test
	void failedCreationRollsBackAndReleasesTheErrandForRetry() {
		prepare();
		when(exchange.createConversation(anyString(), anyString(), any()))
			.thenThrow(new IllegalStateException("MessageExchange unavailable"))
			.thenReturn(ResponseEntity.created(URI.create("/conversations/" + randomUUID())).build());
		assertThatThrownBy(() -> service.createConversation("2281", "NAMESPACE.1", "ERRAND_ID-1", request())).isInstanceOf(IllegalStateException.class);
		assertThat(conversations.findByMunicipalityIdAndNamespaceAndErrandId("2281", "NAMESPACE.1", "ERRAND_ID-1")).isEmpty();
		assertThat(service.createConversation("2281", "NAMESPACE.1", "ERRAND_ID-1", request()).getId()).isNotBlank();
		assertThat(conversations.findByMunicipalityIdAndNamespaceAndErrandId("2281", "NAMESPACE.1", "ERRAND_ID-1")).hasSize(1);
	}
}
