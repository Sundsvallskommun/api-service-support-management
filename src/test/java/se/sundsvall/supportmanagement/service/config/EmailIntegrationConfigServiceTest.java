package se.sundsvall.supportmanagement.service.config;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.EmailIntegration;
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.service.mapper.EmailIntegrationMapper;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class EmailIntegrationConfigServiceTest {

	@Mock
	private EmailWorkerConfigRepository configRepositoryMock;

	@Mock
	private EmailIntegrationMapper mapperMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Captor
	private ArgumentCaptor<EmailWorkerConfigEntity> entityCaptor;

	@InjectMocks
	private EmailIntegrationConfigService configService;

	@Test
	void create() {
		var request = new EmailIntegration();
		var namespace = "namespace";
		var municipalityId = "municipalityId";
		var entity = new EmailWorkerConfigEntity();

		when(mapperMock.toEntity(any(), any(), any())).thenReturn(entity);
		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(true);

		configService.create(request, namespace, municipalityId);

		verify(mapperMock).toEntity(same(request), eq(namespace), eq(municipalityId));
		verify(namespaceConfigRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(configRepositoryMock).save(same(entity));
	}

	@Test
	void createWithMissingNamespaceConfig() {
		var request = new EmailIntegration();
		var namespace = "namespace";
		var municipalityId = "municipalityId";

		when(namespaceConfigRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(false);
		assertThatThrownBy(() -> configService.create(request, namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Internal Server Error: Namespace config must be created before enabling email integration. Add via /namespaceConfig resource")
			.extracting("status").isEqualTo(INTERNAL_SERVER_ERROR);
	}

	@Test
	void replace() {
		var request = new EmailIntegration();
		var namespace = "namespace";
		var municipalityId = "municipalityId";
		var id = 123L;
		var created = OffsetDateTime.now();
		var entity = new EmailWorkerConfigEntity().withId(id).withCreated(created);
		var replacementEntity = new EmailWorkerConfigEntity();

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));
		when(mapperMock.toEntity(any(), any(), any())).thenReturn(replacementEntity);

		configService.replace(request, namespace, municipalityId);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(mapperMock).toEntity(same(request), eq(namespace), eq(municipalityId));
		verify(configRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue()).isSameAs(replacementEntity);
		assertThat(entityCaptor.getValue().getId()).isEqualTo(id);
		assertThat(entityCaptor.getValue().getCreated()).isEqualTo(created);
	}

	@Test
	void replaceNotFound() {
		var request = new EmailIntegration();
		var namespace = "namespace";
		var municipalityId = "municipalityId";

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.replace(request, namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void get() {
		var response = new EmailIntegration();
		var namespace = "namespace";
		var municipalityId = "municipalityId";
		var entity = new EmailWorkerConfigEntity();

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));
		when(mapperMock.toEmailIntegration(any())).thenReturn(response);

		var result = configService.get(namespace, municipalityId);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(mapperMock).toEmailIntegration(same(entity));
		assertThat(result).isSameAs(response);
	}

	@Test
	void getNotFound() {
		var namespace = "namespace";
		var municipalityId = "municipalityId";

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.get(namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void delete() {
		var namespace = "namespace";
		var municipalityId = "municipalityId";

		when(configRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(true);

		configService.delete(namespace, municipalityId);

		verify(configRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(configRepositoryMock).deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void deleteNotFound() {
		var namespace = "namespace";
		var municipalityId = "municipalityId";

		when(configRepositoryMock.existsByNamespaceAndMunicipalityId(any(), any())).thenReturn(false);

		assertThatThrownBy(() -> configService.delete(namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
	}
}