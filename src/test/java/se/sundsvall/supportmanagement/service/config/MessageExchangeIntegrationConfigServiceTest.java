package se.sundsvall.supportmanagement.service.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeIntegration;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeIntegrationConfigRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeIntegrationConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class MessageExchangeIntegrationConfigServiceTest {

	@Mock
	private MessageExchangeIntegrationConfigRepository configRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Captor
	private ArgumentCaptor<MessageExchangeIntegrationConfigEntity> entityCaptor;

	@InjectMocks
	private MessageExchangeIntegrationConfigService configService;

	@Test
	void create() {
		final var request = MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("OPEN");
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(true);

		configService.create(request, namespace, municipalityId);

		verify(namespaceConfigRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(configRepositoryMock).save(entityCaptor.capture());

		final var saved = entityCaptor.getValue();
		assertThat(saved.getNamespace()).isEqualTo(namespace);
		assertThat(saved.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(saved.getTriggerStatusChangeOn()).isEqualTo("SOLVED");
		assertThat(saved.getStatusChangeTo()).isEqualTo("OPEN");
	}

	@Test
	void createWithMissingNamespaceConfig() {
		final var request = new MessageExchangeIntegration();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(false);

		assertThatThrownBy(() -> configService.create(request, namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Namespace config must be created before enabling message exchange integration config. Add via /namespaceConfig resource")
			.extracting("status").isEqualTo(INTERNAL_SERVER_ERROR);
	}

	@Test
	void replace() {
		final var request = MessageExchangeIntegration.create()
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("OPEN");
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var id = 123L;
		final var created = OffsetDateTime.now();
		final var entity = new MessageExchangeIntegrationConfigEntity().withId(id).withCreated(created);

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));

		configService.replace(request, namespace, municipalityId);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(configRepositoryMock).save(entityCaptor.capture());

		final var saved = entityCaptor.getValue();
		assertThat(saved.getId()).isEqualTo(id);
		assertThat(saved.getCreated()).isEqualTo(created);
		assertThat(saved.getNamespace()).isEqualTo(namespace);
		assertThat(saved.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(saved.getTriggerStatusChangeOn()).isEqualTo("SOLVED");
		assertThat(saved.getStatusChangeTo()).isEqualTo("OPEN");
	}

	@Test
	void replaceNotFound() {
		final var request = new MessageExchangeIntegration();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.replace(request, namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void get() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var entity = MessageExchangeIntegrationConfigEntity.create()
			.withTriggerStatusChangeOn("SOLVED")
			.withStatusChangeTo("OPEN")
			.withCreated(created)
			.withModified(modified);

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));

		final var result = configService.get(namespace, municipalityId);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
		assertThat(result.getTriggerStatusChangeOn()).isEqualTo("SOLVED");
		assertThat(result.getStatusChangeTo()).isEqualTo("OPEN");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void getNotFound() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.get(namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void delete() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(true);

		configService.delete(namespace, municipalityId);

		verify(configRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(configRepositoryMock).deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void deleteNotFound() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(false);

		assertThatThrownBy(() -> configService.delete(namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
	}
}
