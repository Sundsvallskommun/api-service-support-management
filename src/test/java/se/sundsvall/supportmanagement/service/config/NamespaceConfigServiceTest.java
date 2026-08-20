package se.sundsvall.supportmanagement.service.config;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.config.ResourceAccess;
import se.sundsvall.supportmanagement.api.model.config.RoleFieldRestriction;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.PARAMETERS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.TITLE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.ERRAND;

@ExtendWith(MockitoExtension.class)
class NamespaceConfigServiceTest {

	@Mock
	private NamespaceConfigRepository configRepositoryMock;

	@Mock
	private NamespaceConfigMapper mapperMock;

	@Captor
	private ArgumentCaptor<NamespaceConfigEntity> entityCaptor;

	@InjectMocks
	private NamespaceConfigService configService;

	@Test
	void create() {
		final var request = NamespaceConfig.create();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var entity = NamespaceConfigEntity.create();

		when(mapperMock.toEntity(any(), any(), any())).thenReturn(entity);

		configService.create(request, namespace, municipalityId);

		verify(mapperMock).toEntity(same(request), eq(namespace), eq(municipalityId));
		verify(configRepositoryMock).save(same(entity));
	}

	@Test
	void createWhenNamespaceExists() {
		final var request = NamespaceConfig.create();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.existsByNamespaceAndMunicipalityId(namespace, municipalityId)).thenReturn(true);

		final var e = assertThrows(ThrowableProblem.class, () -> configService.create(request, namespace, municipalityId));

		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Namespace 'namespace' already exists in municipality 'municipalityId'");
		verify(configRepositoryMock).existsByNamespaceAndMunicipalityId(namespace, municipalityId);
		verifyNoMoreInteractions(configRepositoryMock, mapperMock);
	}

	@Test
	void replace() {
		final var request = NamespaceConfig.create();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var id = 123L;
		final var created = OffsetDateTime.now();
		final var entity = NamespaceConfigEntity.create().withId(id).withCreated(created);
		final var replacementEntity = NamespaceConfigEntity.create();

		when(configRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));
		when(mapperMock.toEntity(any(), any(), any())).thenReturn(replacementEntity);

		configService.replace(request, namespace, municipalityId);

		verify(configRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(mapperMock).toEntity(same(request), eq(namespace), eq(municipalityId));
		verify(configRepositoryMock).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue()).isSameAs(replacementEntity);
		assertThat(entityCaptor.getValue().getId()).isEqualTo(id);
		assertThat(entityCaptor.getValue().getCreated()).isEqualTo(created);
	}

	@Test
	void replaceNotFound() {
		final var request = NamespaceConfig.create();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.replace(request, namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void get() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var entity = NamespaceConfigEntity.create();
		final var response = NamespaceConfig.create();

		when(configRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(entity));
		when(mapperMock.toNamespaceConfig(any())).thenReturn(response);

		final var result = configService.get(namespace, municipalityId);

		verify(configRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(mapperMock).toNamespaceConfig(same(entity));
		assertThat(result).isSameAs(response);
	}

	@Test
	void getNotFound() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.get(namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void findAllWhenMuncipalityNull() {
		final var response = List.of(NamespaceConfig.create());
		final var entities = List.of(NamespaceConfigEntity.create());

		when(configRepositoryMock.findAll()).thenReturn(entities);
		when(mapperMock.toNamespaceConfigs(any())).thenReturn(response);

		final var result = configService.findAll(null);

		verify(configRepositoryMock).findAll();
		verify(mapperMock).toNamespaceConfigs(same(entities));
		verifyNoMoreInteractions(configRepositoryMock, mapperMock);
		assertThat(result).isSameAs(response);
	}

	@Test
	void findAllWhenMuncipalityPresent() {
		final var municipalityId = "municipalityId";
		final var response = List.of(NamespaceConfig.create());
		final var entities = List.of(NamespaceConfigEntity.create());

		when(configRepositoryMock.findAllByMunicipalityId(municipalityId)).thenReturn(entities);
		when(mapperMock.toNamespaceConfigs(any())).thenReturn(response);

		final var result = configService.findAll(municipalityId);

		verify(configRepositoryMock).findAllByMunicipalityId(municipalityId);
		verify(mapperMock).toNamespaceConfigs(same(entities));
		verifyNoMoreInteractions(configRepositoryMock, mapperMock);
		assertThat(result).isSameAs(response);
	}

	@Test
	void delete() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.of(NamespaceConfigEntity.create()));

		configService.delete(namespace, municipalityId);

		verify(configRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
		verify(configRepositoryMock).deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void deleteNotFound() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		when(configRepositoryMock.findByNamespaceAndMunicipalityId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> configService.delete(namespace, municipalityId))
			.isInstanceOf(Problem.class)
			.hasMessage("Not Found: No config found in namespace 'namespace' for municipality 'municipalityId'")
			.extracting("status").isEqualTo(NOT_FOUND);

		verify(configRepositoryMock).findByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	@Test
	void createWithDuplicatedRoleAccess() {
		final var request = NamespaceConfig.create().withRoleFieldRestrictions(List.of(
			RoleFieldRestriction.create().withRole("REPORTER"),
			RoleFieldRestriction.create().withRole("reporter")));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: Role 'REPORTER' occurs more than once in role access");
		verify(configRepositoryMock, never()).save(any());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"LIMITED", "limited", "Reporter", "REPORTER"
	})
	void createWithRoleNamedAfterAReservedScope(final String role) {
		// Stored verbatim, such a role would be read back as the limited read or reporter configuration of the namespace.
		final var request = NamespaceConfig.create().withRoleFieldRestrictions(List.of(
			RoleFieldRestriction.create().withRole(role).withFields(List.of(FieldAccess.create().withField(TITLE)))));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: Role '%s' is reserved and may not be used in role access".formatted(role));
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void replaceWithRoleNamedAfterAReservedScope() {
		final var request = NamespaceConfig.create().withRoleFieldRestrictions(List.of(
			RoleFieldRestriction.create().withRole("LIMITED").withFields(List.of(FieldAccess.create().withField(TITLE)))));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.replace(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithKeysOnNonKeyedField() {
		final var request = NamespaceConfig.create().withRoleFieldRestrictions(List.of(
			RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(TITLE).withKeys(List.of("key-1"))))));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: Keys may not be set for field 'TITLE' of 'CASE_OFFICER' as the field holds no keyed collection");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithValidRoleAccess() {
		final var request = NamespaceConfig.create()
			.withReporterAccess(ReporterAccess.create()
				.withResources(List.of(ResourceAccess.create().withResource(ERRAND).withLevel(AccessLevel.R)))
				.withFields(List.of(FieldAccess.create().withField(PARAMETERS).withKeys(List.of("key-1")))))
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(TITLE))));

		configService.create(request, "namespace", "municipalityId");

		verify(configRepositoryMock).save(any());
	}
}
