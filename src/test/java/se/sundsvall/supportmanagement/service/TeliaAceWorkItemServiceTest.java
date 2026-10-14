package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.emailreader.Email;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeliaAceWorkItemServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "KONTAKTSUNDSVALL";
	private static final String ERRAND_ID = "errand-id";
	private static final String ERRAND_NUMBER = "KS-123456";
	private static final String BASE_URL = "https://draken.sundsvall.se/kontaktsundsvall";
	private static final String SENDER = "anna.andersson@example.com";
	private static final String ASSIGNED_USER_ID = "jep11jep";

	@Mock
	private TeliaAceWorkItemRepository workItemRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private NamespaceConfigMapper namespaceConfigMapperMock;

	@Mock
	private Consumer<String> setUnHealthyConsumerMock;

	@Mock
	private PlatformTransactionManager transactionManagerMock;

	@InjectMocks
	private TeliaAceWorkItemService service;

	@Captor
	private ArgumentCaptor<TeliaAceWorkItemEntity> workItemCaptor;

	private static NamespaceConfigEntity configEntity() {
		return NamespaceConfigEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);
	}

	private static ErrandEntity errand() {
		return ErrandEntity.create()
			.withId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER)
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID);
	}

	private static Email email() {
		return new Email().sender(SENDER);
	}

	// The global toggle defaults to disabled, but every test here except the one for that default exercises
	// behaviour behind it, so it is switched on here rather than repeated in each test.
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "enabled", true);
	}

	private void mockNamespaceConfig(final NamespaceConfig config) {
		final var entity = configEntity();
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));
		when(namespaceConfigMapperMock.toNamespaceConfig(entity)).thenReturn(config);
	}

	@Test
	void notify_whenEnabledAndUnassigned_savesWorkItemWithoutAgent() {
		mockNamespaceConfig(NamespaceConfig.create().withTeliaAceWorkItemEnabled(true).withTeliaAceWorkItemBaseUrl(BASE_URL));

		service.notify(errand(), email());

		verify(workItemRepositoryMock).save(workItemCaptor.capture());
		final var workItem = workItemCaptor.getValue();
		assertThat(workItem.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(workItem.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(workItem.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(workItem.getFromAddress()).isEqualTo(SENDER);
		assertThat(workItem.getSubject()).isEqualTo("Nytt ärende i Draken");
		assertThat(workItem.getContentUrl()).isEqualTo(BASE_URL + "/arende/" + ERRAND_NUMBER);
		assertThat(workItem.getPredefinedAgentName()).isNull();
	}

	@Test
	void notify_whenBaseUrlHasTrailingSlash_stripsIt() {
		mockNamespaceConfig(NamespaceConfig.create().withTeliaAceWorkItemEnabled(true).withTeliaAceWorkItemBaseUrl(BASE_URL + "/"));

		service.notify(errand(), email());

		verify(workItemRepositoryMock).save(workItemCaptor.capture());
		assertThat(workItemCaptor.getValue().getContentUrl()).isEqualTo(BASE_URL + "/arende/" + ERRAND_NUMBER);
	}

	@Test
	void notify_whenEnabledAndAssigned_savesWorkItemWithAgent() {
		mockNamespaceConfig(NamespaceConfig.create().withTeliaAceWorkItemEnabled(true).withTeliaAceWorkItemBaseUrl(BASE_URL));
		final var assignedErrand = errand().withAssignedUserId(ASSIGNED_USER_ID);

		service.notify(assignedErrand, email());

		verify(workItemRepositoryMock).save(workItemCaptor.capture());
		final var workItem = workItemCaptor.getValue();
		assertThat(workItem.getPredefinedAgentName()).isEqualTo(ASSIGNED_USER_ID);
	}

	@Test
	void notify_whenNamespaceConfigMissing_doesNothing() {
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		service.notify(errand(), email());

		verify(workItemRepositoryMock, never()).save(any());
	}

	@Test
	void notify_whenDisabled_doesNothing() {
		mockNamespaceConfig(NamespaceConfig.create().withTeliaAceWorkItemEnabled(false));

		service.notify(errand(), email());

		verify(workItemRepositoryMock, never()).save(any());
	}

	@Test
	void notify_whenGloballyDisabledByDefault_doesNothingRegardlessOfNamespaceConfig() {
		ReflectionTestUtils.setField(service, "enabled", false);

		service.notify(errand(), email());

		verifyNoInteractions(namespaceConfigRepositoryMock, workItemRepositoryMock);
	}

	@Test
	void onTeliaAceNotificationRequested_delegatesToNotify() {
		when(transactionManagerMock.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
		mockNamespaceConfig(NamespaceConfig.create().withTeliaAceWorkItemEnabled(true).withTeliaAceWorkItemBaseUrl(BASE_URL));

		service.onTeliaAceNotificationRequested(new TeliaAceNotificationRequestedEvent(errand(), email(), setUnHealthyConsumerMock));

		verify(workItemRepositoryMock).save(workItemCaptor.capture());
		assertThat(workItemCaptor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		verifyNoInteractions(setUnHealthyConsumerMock);
	}

	/**
	 * This is the scenario the listener exists for: by running after the publishing transaction has committed, a
	 * failure here can only ever be this method's own, never the errand processing that triggered it - so it is caught
	 * and signalled here, not left to surface somewhere that can no longer do anything about the errand.
	 */
	@Test
	void onTeliaAceNotificationRequested_whenNotifyFails_signalsUnhealthyWithoutThrowing() {
		when(transactionManagerMock.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenThrow(new RuntimeException("Database down"));

		assertThatNoException().isThrownBy(() -> service.onTeliaAceNotificationRequested(new TeliaAceNotificationRequestedEvent(errand(), email(), setUnHealthyConsumerMock)));

		verify(setUnHealthyConsumerMock).accept("Failed to queue Telia ACE work item for errand " + ERRAND_ID);
	}

	/**
	 * Covers a failure that only surfaces when the transaction commits - the {@code TeliaAceWorkItemEntity} id is
	 * server-generated, so the insert runs at commit, after {@link TeliaAceWorkItemService#notify} has already
	 * returned. Routing the transaction through an explicit {@code TransactionTemplate} inside the try, rather than
	 * a method-level {@code @Transactional} on the listener, is what lets this catch block see that failure at all.
	 */
	@Test
	void onTeliaAceNotificationRequested_whenCommitFails_signalsUnhealthyWithoutThrowing() {
		final var status = new SimpleTransactionStatus();
		when(transactionManagerMock.getTransaction(any())).thenReturn(status);
		doThrow(new RuntimeException("Commit failed")).when(transactionManagerMock).commit(status);
		mockNamespaceConfig(NamespaceConfig.create().withTeliaAceWorkItemEnabled(true).withTeliaAceWorkItemBaseUrl(BASE_URL));

		assertThatNoException().isThrownBy(() -> service.onTeliaAceNotificationRequested(new TeliaAceNotificationRequestedEvent(errand(), email(), setUnHealthyConsumerMock)));

		verify(setUnHealthyConsumerMock).accept("Failed to queue Telia ACE work item for errand " + ERRAND_ID);
	}
}
