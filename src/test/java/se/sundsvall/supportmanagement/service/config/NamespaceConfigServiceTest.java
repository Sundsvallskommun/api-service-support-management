package se.sundsvall.supportmanagement.service.config;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class NamespaceConfigServiceTest {

	@Mock
	private NamespaceConfigRepository configRepositoryMock;

	@Mock
	private NamespaceConfigMapper mapperMock;

	@Captor
	ArgumentCaptor<NamespaceConfigEntity> entityCaptor;

	@InjectMocks
	private NamespaceConfigService configService;

	@Test
	void create() {
		var request = new NamespaceConfig();
		var namespace = "namespace";
		var municipalityId = "municipalityId";
		var entity = new NamespaceConfigEntity();

		when(mapperMock.toEntity(any(), any(), any())).thenReturn(entity);

		configService.create(request, namespace, municipalityId);

		verify(mapperMock).toEntity(same(request), eq(namespace), eq(municipalityId));
		verify(configRepositoryMock).save(same(entity));
	}

	@Test
	void replace() {
		var request = new NamespaceConfig();
		var namespace = "namespace";
		var municipalityId = "municipalityId";
		var id = 123L;
		var created = OffsetDateTime.now();
		var entity = new NamespaceConfigEntity().withId(id).withCreated(created);
		var replacementEntity = new NamespaceConfigEntity();

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
		var request = new NamespaceConfig();
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
		var response = new NamespaceConfig();
		var namespace = "namespace";
		var municipalityId = "municipalityId";
		var entity = new NamespaceConfigEntity();

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));
		when(mapperMock.toNamespaceConfig(any())).thenReturn(response);

		var result = configService.get(namespace, municipalityId);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(mapperMock).toNamespaceConfig(same(entity));
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

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(NamespaceConfigEntity.create()));

		configService.delete(namespace, municipalityId);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(configRepositoryMock).deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void deleteNotFound() {
		var namespace = "namespace";
		var municipalityId = "municipalityId";

		when(configRepositoryMock.getByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.delete(namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).getByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

}