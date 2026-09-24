package se.sundsvall.supportmanagement.service.config;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.PARAMETERS;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.TITLE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.COMMUNICATION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource.ERRAND;

@ExtendWith(MockitoExtension.class)
class NamespaceConfigServiceTest {

	@Mock
	private NamespaceConfigRepository configRepositoryMock;

	@Mock
	private NamespaceConfigMapper mapperMock;

	@Captor
	private ArgumentCaptor<NamespaceConfigEntity> entityCaptor;

	private NamespaceConfigService configService;

	@BeforeEach
	void setUp() {
		configService = new NamespaceConfigService(configRepositoryMock, mapperMock);
	}

	@Test
	void isSingleDecisionPerErrandReadsTheSettingOfTheNamespace() {
		final var entity = NamespaceConfigEntity.create();
		when(configRepositoryMock.findByNamespaceAndMunicipalityId("namespace", "2281")).thenReturn(Optional.of(entity));
		when(mapperMock.toNamespaceConfig(entity)).thenReturn(NamespaceConfig.create().withSingleDecisionPerErrand(true));

		assertThat(configService.isSingleDecisionPerErrand("namespace", "2281")).isTrue();
	}

	@Test
	void isSingleDecisionPerErrandIsFalseForANamespaceWithoutConfiguration() {
		when(configRepositoryMock.findByNamespaceAndMunicipalityId("namespace", "2281")).thenReturn(Optional.empty());

		assertThat(configService.isSingleDecisionPerErrand("namespace", "2281")).isFalse();
		verifyNoInteractions(mapperMock);
	}

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
	void createWithLevelOnNonKeyedField() {
		final var request = NamespaceConfig.create().withRoleFieldRestrictions(List.of(
			RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(TITLE).withLevel(AccessLevel.R)))));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: Level may not be set for field 'TITLE' of 'CASE_OFFICER' as the field holds no keyed collection");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithLimitedReadAsFieldLevel() {
		final var request = NamespaceConfig.create().withRoleFieldRestrictions(List.of(
			RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(PARAMETERS).withLevel(AccessLevel.LR)))));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: Level 'LR' may not be set for field 'PARAMETERS' of 'CASE_OFFICER' as a field is held at read or read/write");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithLevelOnKeyedField() {
		final var request = NamespaceConfig.create().withRoleFieldRestrictions(List.of(
			RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(PARAMETERS).withKeys(List.of("key-1")).withLevel(AccessLevel.R)))));

		when(mapperMock.toEntity(any(), any(), any())).thenReturn(NamespaceConfigEntity.create());

		// A level is what a keyed field is for, so it passes where the two cases above do not.
		assertThatNoException().isThrownBy(() -> configService.create(request, "namespace", "municipalityId"));

		verify(configRepositoryMock).save(any());
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

		when(mapperMock.toEntity(request, "namespace", "municipalityId")).thenReturn(NamespaceConfigEntity.create());

		configService.create(request, "namespace", "municipalityId");

		verify(configRepositoryMock).save(any());
	}

	/**
	 * A service with the real mapper, for the duplicate checks, which run against the mapped rows.
	 */
	private NamespaceConfigService serviceWithRealMapper() {
		return new NamespaceConfigService(configRepositoryMock, new NamespaceConfigMapper());
	}

	@Test
	void createWithSameResourceGrantedAtTwoLevels() {
		final var request = NamespaceConfig.create()
			.withReporterAccess(ReporterAccess.create().withResources(List.of(
				ResourceAccess.create().withResource(COMMUNICATION).withLevel(AccessLevel.R),
				ResourceAccess.create().withResource(COMMUNICATION).withLevel(AccessLevel.RW))));

		final var service = serviceWithRealMapper();
		final var exception = assertThrows(ThrowableProblem.class, () -> service.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: 'COMMUNICATION' occurs more than once as RESOURCE in 'REPORTER'");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithSameFieldGrantedTwice() {
		final var request = NamespaceConfig.create()
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(
				FieldAccess.create().withField(TITLE),
				FieldAccess.create().withField(TITLE))));

		final var service = serviceWithRealMapper();
		final var exception = assertThrows(ThrowableProblem.class, () -> service.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: 'TITLE' occurs more than once as FIELD in 'LIMITED'");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithRepeatedKeyInsideOneField() {
		final var request = NamespaceConfig.create()
			.withReporterAccess(ReporterAccess.create().withFields(List.of(
				FieldAccess.create().withField(PARAMETERS).withKeys(List.of("key-1", "key-1")))));

		final var service = serviceWithRealMapper();
		final var exception = assertThrows(ThrowableProblem.class, () -> service.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: 'PARAMETERS:key-1' occurs more than once as FIELD in 'REPORTER'");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void replaceWithDuplicateGrantIsRejected() {
		final var request = NamespaceConfig.create()
			.withReporterAccess(ReporterAccess.create().withResources(List.of(
				ResourceAccess.create().withResource(COMMUNICATION).withLevel(AccessLevel.R),
				ResourceAccess.create().withResource(COMMUNICATION).withLevel(AccessLevel.R))));

		when(configRepositoryMock.findByNamespaceAndMunicipalityId("namespace", "municipalityId"))
			.thenReturn(Optional.of(NamespaceConfigEntity.create().withId(1L)));

		final var service = serviceWithRealMapper();
		final var exception = assertThrows(ThrowableProblem.class, () -> service.replace(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithUnknownProcessConsumer() {
		final var request = NamespaceConfig.create().withProcessConsumer("pw-alk");

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo(
			"Bad Request: 'pw-alk' is not a known process consumer. The process consumer of a namespace is the address events are delivered to, and must be 'pw-alkt'");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void replaceWithUnknownProcessConsumer() {
		final var request = NamespaceConfig.create().withProcessConsumer("PW-ALKT");

		// The name is the delivery address, so it has to match exactly rather than case insensitively
		final var exception = assertThrows(ThrowableProblem.class, () -> configService.replace(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithProcessConsumerOnAnAccessControlledNamespace() {
		final var request = NamespaceConfig.create()
			.withAccessControl(true)
			.withProcessConsumer("pw-alkt");

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo(
			"Bad Request: Access control may not be active for a namespace with the process consumer 'pw-alkt'. A process consumer is not an AD account, and the access mapper grants access to nothing else, so every read and write the process makes for the namespace would be denied");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void replaceTurningOnAccessControlForANamespaceWithAProcessConsumer() {
		final var request = NamespaceConfig.create()
			.withProcessConsumer("pw-alkt")
			.withAccessControl(true);

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.replace(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		verify(configRepositoryMock, never()).findByNamespaceAndMunicipalityId(any(), any());
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithDuplicatedProcessTrigger() {
		final var request = NamespaceConfig.create()
			.withProcessConsumer("pw-alkt")
			.withProcessTriggers(List.of(EventSubType.ERRAND, EventSubType.MESSAGE, EventSubType.ERRAND));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo("Bad Request: 'ERRAND' occurs more than once among the process triggers");
		verify(configRepositoryMock, never()).save(any());
	}

	private static Stream<Arguments> incompleteProcessTriggers() {
		return Stream.of(
			arguments(List.of(), "ERRAND and DECISION"),
			arguments(List.of(EventSubType.MESSAGE), "ERRAND and DECISION"),
			arguments(List.of(EventSubType.ERRAND, EventSubType.ATTACHMENT), "DECISION"),
			arguments(List.of(EventSubType.DECISION), "ERRAND"));
	}

	/**
	 * A namespace with a process consumer is refused unless its process triggers include both ERRAND and DECISION.
	 */
	@ParameterizedTest
	@MethodSource("incompleteProcessTriggers")
	void createWithProcessConsumerMissingARequiredTrigger(final List<EventSubType> triggers, final String missing) {
		final var request = NamespaceConfig.create()
			.withProcessConsumer("pw-alkt")
			.withProcessTriggers(triggers);

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo(("Bad Request: A namespace with the process consumer 'pw-alkt' must list %s among its process triggers. Without ERRAND an errand given its "
			+ "process label after it was created never starts its process, and without DECISION a process waiting for a decision is never told that it has been made").formatted(missing));
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void createWithProcessConsumerWithoutAnyTriggers() {
		final var request = NamespaceConfig.create().withProcessConsumer("pw-alkt");

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).contains("ERRAND and DECISION");
		verify(configRepositoryMock, never()).save(any());
	}

	@Test
	void replaceWithProcessConsumerMissingDecision() {
		final var request = NamespaceConfig.create()
			.withProcessConsumer("pw-alkt")
			.withProcessTriggers(List.of(EventSubType.ERRAND, EventSubType.MESSAGE));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.replace(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).contains("must list DECISION");
		verify(configRepositoryMock, never()).findByNamespaceAndMunicipalityId(any(), any());
		verify(configRepositoryMock, never()).save(any());
	}

	/**
	 * A command among the process triggers is refused, whether the namespace runs a process or not.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"PROCESS", "SIGNAL"
	})
	void createWithACommandAmongTheProcessTriggers(final EventSubType command) {
		final var request = NamespaceConfig.create()
			.withProcessTriggers(List.of(EventSubType.ERRAND, command));

		final var exception = assertThrows(ThrowableProblem.class, () -> configService.create(request, "namespace", "municipalityId"));

		assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(exception.getMessage()).isEqualTo(("Bad Request: '%s' is a command to the process rather than a change to the errand. Commands always reach the process and are never "
			+ "filtered by the process triggers, so it may not be listed among them").formatted(command));
		verify(configRepositoryMock, never()).save(any());
	}

	/**
	 * Triggers without a consumer are accepted and kept.
	 */
	@Test
	void createWithProcessTriggersButNoProcessConsumer() {
		final var request = NamespaceConfig.create()
			.withProcessTriggers(List.of(EventSubType.MESSAGE));
		final var entity = NamespaceConfigEntity.create();

		when(mapperMock.toEntity(any(), any(), any())).thenReturn(entity);

		configService.create(request, "namespace", "municipalityId");

		verify(configRepositoryMock).save(same(entity));
	}

	@Test
	void createWithProcessConfiguration() {
		final var request = NamespaceConfig.create()
			.withProcessConsumer("pw-alkt")
			.withProcessTriggers(List.of(EventSubType.ERRAND, EventSubType.DECISION));
		final var entity = NamespaceConfigEntity.create();

		when(mapperMock.toEntity(any(), any(), any())).thenReturn(entity);

		configService.create(request, "namespace", "municipalityId");

		verify(configRepositoryMock).save(same(entity));
	}

	@Test
	void getProcessTriggersReadsTheConfiguredOnes() {
		final var entity = NamespaceConfigEntity.create();

		when(configRepositoryMock.findByNamespaceAndMunicipalityId("namespace", "municipalityId")).thenReturn(Optional.of(entity));
		when(mapperMock.toProcessTriggers(same(entity))).thenReturn(List.of(EventSubType.ERRAND, EventSubType.MESSAGE));

		assertThat(configService.getProcessTriggers("namespace", "municipalityId")).containsExactlyInAnyOrder(EventSubType.ERRAND, EventSubType.MESSAGE);
	}

	@Test
	void getProcessTriggersOfANamespaceThatNamesNoneIsEmpty() {
		final var entity = NamespaceConfigEntity.create();

		when(configRepositoryMock.findByNamespaceAndMunicipalityId("namespace", "municipalityId")).thenReturn(Optional.of(entity));
		when(mapperMock.toProcessTriggers(same(entity))).thenReturn(null);

		assertThat(configService.getProcessTriggers("namespace", "municipalityId")).isEmpty();
	}

	@Test
	void getProcessTriggersOfANamespaceWithoutConfigurationIsEmpty() {
		when(configRepositoryMock.findByNamespaceAndMunicipalityId("namespace", "municipalityId")).thenReturn(Optional.empty());

		assertThat(configService.getProcessTriggers("namespace", "municipalityId")).isEmpty();
	}
}
