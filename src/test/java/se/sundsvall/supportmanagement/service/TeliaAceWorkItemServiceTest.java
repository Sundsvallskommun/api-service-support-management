package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.emailreader.Email;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

	@InjectMocks
	private TeliaAceWorkItemService service;

	@Captor
	private ArgumentCaptor<TeliaAceWorkItemEntity> workItemCaptor;

	private static NamespaceConfigEntity enabledConfig() {
		return NamespaceConfigEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withValues(java.util.List.of(
				NamespaceConfigValueEmbeddable.create().withKey("TELIA_ACE_WORK_ITEM_ENABLED").withValue("true").withType(ValueType.BOOLEAN),
				NamespaceConfigValueEmbeddable.create().withKey("TELIA_ACE_WORK_ITEM_BASE_URL").withValue(BASE_URL).withType(ValueType.STRING)));
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

	@Test
	void enqueueForNewErrand_whenEnabled_savesWorkItemWithoutAgent() {
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(enabledConfig()));

		service.enqueueForNewErrand(errand(), email());

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
	void enqueueForUpdatedErrand_whenAssignedAndEnabled_savesWorkItemWithAgent() {
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(enabledConfig()));
		final var assignedErrand = errand().withAssignedUserId(ASSIGNED_USER_ID);

		service.enqueueForUpdatedErrand(assignedErrand, email());

		verify(workItemRepositoryMock).save(workItemCaptor.capture());
		final var workItem = workItemCaptor.getValue();
		assertThat(workItem.getSubject()).isEqualTo("Uppdaterat ärende i Draken");
		assertThat(workItem.getPredefinedAgentName()).isEqualTo(ASSIGNED_USER_ID);
	}

	@Test
	void enqueueForUpdatedErrand_whenNotAssigned_doesNothing() {
		service.enqueueForUpdatedErrand(errand(), email());

		verify(workItemRepositoryMock, never()).save(any());
		verifyNoMoreInteractions(namespaceConfigRepositoryMock);
	}

	@Test
	void enqueueForNewErrand_whenNamespaceConfigMissing_doesNothing() {
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		service.enqueueForNewErrand(errand(), email());

		verify(workItemRepositoryMock, never()).save(any());
	}

	@Test
	void enqueueForNewErrand_whenDisabled_doesNothing() {
		final var disabledConfig = NamespaceConfigEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withValues(java.util.List.of(
				NamespaceConfigValueEmbeddable.create().withKey("TELIA_ACE_WORK_ITEM_ENABLED").withValue("false").withType(ValueType.BOOLEAN)));
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(disabledConfig));

		service.enqueueForNewErrand(errand(), email());

		verify(workItemRepositoryMock, never()).save(any());
	}
}
