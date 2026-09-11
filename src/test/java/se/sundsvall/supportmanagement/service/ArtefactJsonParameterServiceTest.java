package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.StatementJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import se.sundsvall.supportmanagement.service.AccessControlService.KeyAccess;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.JSON_PARAMETERS;

@ExtendWith(MockitoExtension.class)
class ArtefactJsonParameterServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String KEY = "responseForm";

	private static final Function<JsonParameterEntity, StatementJsonParameterEntity> LINK_FACTORY = parameter -> StatementJsonParameterEntity.create().withJsonParameterEntity(parameter);
	private static final KeyAccess MAY_CHANGE = new KeyAccess(_ -> true, _ -> true);

	@Mock
	private AccessControlService accessControlServiceMock;

	@Mock
	private EntityManager entityManagerMock;

	@Mock
	private StatementJsonParameterRepository linkRepositoryMock;

	@InjectMocks
	private ArtefactJsonParameterService service;

	private static JsonParameterEntity parameter(final String id, final String key) {
		return JsonParameterEntity.create().withId(id).withKey(key).withSchemaId("test-schema-1.0").withValue("{}");
	}

	private static StatementJsonParameterEntity linkTo(final JsonParameterEntity parameter) {
		return StatementJsonParameterEntity.create().withId("link-" + parameter.getId()).withJsonParameterEntity(parameter);
	}

	private static ErrandEntity errand(final JsonParameterEntity... parameters) {
		return ErrandEntity.create().withId(ERRAND_ID).withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withJsonParameters(new ArrayList<>(List.of(parameters)));
	}

	private static JsonParameter body() {
		return JsonParameter.create().withKey(KEY).withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode().put("name", "test"));
	}

	private void mockReadableKeys(final ErrandEntity errandEntity, final String hiddenKey) {
		when(accessControlServiceMock.readableKeyPredicate(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(), same(errandEntity), eq(JSON_PARAMETERS)))
			.thenReturn(key -> !key.equals(hiddenKey));
	}

	private void mockKeyAccess(final ErrandEntity errandEntity, final JsonParameter body, final KeyAccess keyAccess) {
		when(accessControlServiceMock.verifyJsonParameterAccess(NAMESPACE, MUNICIPALITY_ID, errandEntity, KEY, body)).thenReturn(keyAccess);
	}

	@Test
	void readAllSortsByKey() {

		// Arrange
		final var errandEntity = errand();
		mockReadableKeys(errandEntity, null);
		final var links = List.of(linkTo(parameter("2", "zeta")), linkTo(parameter("1", "alpha")));

		// Act
		final var result = service.readAll(errandEntity, links);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly("alpha", "zeta");
	}

	/**
	 * The parameter is one of the errand's, so a key the namespace keeps from the caller on the errand is kept from them
	 * here as well - otherwise the artefact would be a way around it.
	 */
	@Test
	void readAllLeavesOutKeysTheCallerMayNotSee() {

		// Arrange
		final var errandEntity = errand();
		mockReadableKeys(errandEntity, "secret");
		final var links = List.of(linkTo(parameter("1", "secret")), linkTo(parameter("2", KEY)));

		// Act
		final var result = service.readAll(errandEntity, links);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly(KEY);
	}

	/**
	 * A link whose parameter has gone is skipped rather than mapped to a null.
	 */
	@Test
	void readAllSkipsLinksWithoutAParameter() {

		// Arrange
		final var errandEntity = errand();
		mockReadableKeys(errandEntity, null);

		// Act
		final var result = service.readAll(errandEntity, List.of(StatementJsonParameterEntity.create(), linkTo(parameter("1", KEY))));

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly(KEY);
	}

	@Test
	void readAllOfNothingIsEmpty() {

		// Arrange
		final var errandEntity = errand();
		mockReadableKeys(errandEntity, null);

		// Act & Verify
		assertThat(service.readAll(errandEntity, null)).isEmpty();
	}

	@Test
	void read() {

		// Arrange
		final var errandEntity = errand();

		// Act
		final var result = service.read(errandEntity, List.of(linkTo(parameter("1", KEY))), KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
		verify(accessControlServiceMock).verifyAccessibleKey(NAMESPACE, MUNICIPALITY_ID, errandEntity, JSON_PARAMETERS, KEY);
	}

	@Test
	void readingAKeyTheCallerMayNotSeeIsRefused() {

		// Arrange
		final var errandEntity = errand();
		doThrow(Problem.valueOf(UNAUTHORIZED, "not accessible")).when(accessControlServiceMock).verifyAccessibleKey(NAMESPACE, MUNICIPALITY_ID, errandEntity, JSON_PARAMETERS, KEY);
		final var links = List.of(linkTo(parameter("1", KEY)));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.read(errandEntity, links, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(UNAUTHORIZED);
	}

	/**
	 * A key belonging to another artefact, or to nothing at all, is not this artefact's to read.
	 */
	@Test
	void readingAKeyTheArtefactDoesNotOwnGivesNotFound() {

		// Arrange
		final var errandEntity = errand();
		final var links = List.of(linkTo(parameter("1", "somethingElse")));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.read(errandEntity, links, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(KEY);
	}

	@Test
	void upsertReplacesAKeyTheArtefactAlreadyOwns() {

		// Arrange
		final var existing = parameter("1", KEY);
		final var links = new ArrayList<>(List.of(linkTo(existing)));
		final var errandEntity = errand(existing);
		final var body = body();
		mockKeyAccess(errandEntity, body, MAY_CHANGE);

		// Act
		final var result = service.upsert(errandEntity, null, body, links, LINK_FACTORY, linkRepositoryMock);

		// Verify
		assertThat(result.created()).isFalse();
		assertThat(existing.getValue()).contains("test");
		assertThat(links).as("no second link was written").hasSize(1);
		verify(entityManagerMock).flush();
		verifyNoInteractions(linkRepositoryMock);
	}

	/**
	 * A caller who may see a key but not change it only gets past the access check by sending what is stored. That is
	 * answered as it stands, without writing it again and bumping a version they may not move.
	 */
	@Test
	void upsertOfAKeyTheCallerMayNotChangeLeavesItAsItStands() {

		// Arrange
		final var existing = parameter("1", KEY).withVersion(4L);
		final var links = new ArrayList<>(List.of(linkTo(existing)));
		final var errandEntity = errand(existing);
		final var body = body();
		mockKeyAccess(errandEntity, body, new KeyAccess(_ -> true, _ -> false));

		// Act
		final var result = service.upsert(errandEntity, null, body, links, LINK_FACTORY, linkRepositoryMock);

		// Verify
		assertThat(result.created()).isFalse();
		assertThat(result.jsonParameter().getVersion()).isEqualTo(4L);
		assertThat(existing.getValue()).isEqualTo("{}");
		verifyNoInteractions(entityManagerMock, linkRepositoryMock);
	}

	@Test
	void upsertRefusedByTheKeyAccessWritesNothing() {

		// Arrange
		final var errandEntity = errand();
		final var links = new ArrayList<StatementJsonParameterEntity>();
		final var body = body();
		when(accessControlServiceMock.verifyJsonParameterAccess(NAMESPACE, MUNICIPALITY_ID, errandEntity, KEY, body)).thenThrow(Problem.valueOf(UNAUTHORIZED, "not writable"));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.upsert(errandEntity, null, body, links, LINK_FACTORY, linkRepositoryMock));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(errandEntity.getJsonParameters()).isEmpty();
		assertThat(links).isEmpty();
		verifyNoInteractions(entityManagerMock, linkRepositoryMock);
	}

	@Test
	void upsertCreatesTheParameterAndItsLinkForANewKey() {

		// Arrange
		final var links = new ArrayList<StatementJsonParameterEntity>();
		final var errandEntity = errand();
		final var body = body();
		mockKeyAccess(errandEntity, body, MAY_CHANGE);
		when(linkRepositoryMock.save(any(StatementJsonParameterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		final var result = service.upsert(errandEntity, null, body, links, LINK_FACTORY, linkRepositoryMock);

		// Verify
		assertThat(result.created()).isTrue();
		assertThat(result.jsonParameter().getKey()).isEqualTo(KEY);
		assertThat(errandEntity.getJsonParameters()).as("the parameter is the errand's too").hasSize(1);
		assertThat(links).hasSize(1);
	}

	/**
	 * An errand that has never held a parameter has no collection to add to yet.
	 */
	@Test
	void upsertFillsAnErrandWithoutAParameterCollection() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);
		final var body = body();
		mockKeyAccess(errandEntity, body, MAY_CHANGE);
		when(linkRepositoryMock.save(any(StatementJsonParameterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		service.upsert(errandEntity, null, body, new ArrayList<>(), LINK_FACTORY, linkRepositoryMock);

		// Verify
		assertThat(errandEntity.getJsonParameters()).hasSize(1);
	}

	/**
	 * Keys are unique per errand. An artefact asking for one something else on the errand already holds is told, rather
	 * than quietly taking it over.
	 */
	@Test
	void claimingAKeyTheErrandAlreadyHoldsIsAConflict() {

		// Arrange
		final var errandEntity = errand(parameter("9", KEY));
		final var links = new ArrayList<StatementJsonParameterEntity>();
		final var body = body();
		mockKeyAccess(errandEntity, body, MAY_CHANGE);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.upsert(errandEntity, null, body, links, LINK_FACTORY, linkRepositoryMock));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(KEY, ERRAND_ID);
		assertThat(links).isEmpty();
		verifyNoInteractions(linkRepositoryMock);
	}

	@Test
	void upsertHonoursIfMatch() {

		// Arrange
		final var existing = parameter("1", KEY).withVersion(3L);
		final var links = new ArrayList<>(List.of(linkTo(existing)));
		final var errandEntity = errand(existing);
		final var body = body();
		mockKeyAccess(errandEntity, body, MAY_CHANGE);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.upsert(errandEntity, "\"2\"", body, links, LINK_FACTORY, linkRepositoryMock));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
	}

	/**
	 * The link goes first and the parameter second. Removing the parameter first would leave orphan removal deleting a row
	 * that is no longer there, which the caller would see as a 412 it can do nothing about.
	 */
	@Test
	void deleteRemovesTheLinkAndThenTheParameter() {

		// Arrange
		final var existing = parameter("1", KEY);
		final var links = new ArrayList<>(List.of(linkTo(existing), linkTo(parameter("2", "other"))));
		final var errandEntity = errand(existing, parameter("3", "errandOwned"));

		// Act
		service.delete(errandEntity, links, KEY, null);

		// Verify
		assertThat(links).extracting(link -> link.getJsonParameterEntity().getKey()).containsExactly("other");
		assertThat(errandEntity.getJsonParameters()).extracting(JsonParameterEntity::getKey).containsExactly("errandOwned");
		verify(accessControlServiceMock).verifyWritableKey(NAMESPACE, MUNICIPALITY_ID, errandEntity, JSON_PARAMETERS, KEY);
		verify(entityManagerMock, times(2)).flush();
	}

	@Test
	void deletingAKeyTheCallerMayNotChangeIsRefused() {

		// Arrange
		final var existing = parameter("1", KEY);
		final var links = new ArrayList<>(List.of(linkTo(existing)));
		final var errandEntity = errand(existing);
		doThrow(Problem.valueOf(UNAUTHORIZED, "not writable")).when(accessControlServiceMock).verifyWritableKey(NAMESPACE, MUNICIPALITY_ID, errandEntity, JSON_PARAMETERS, KEY);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.delete(errandEntity, links, KEY, null));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(links).hasSize(1);
		assertThat(errandEntity.getJsonParameters()).hasSize(1);
		verifyNoInteractions(entityManagerMock);
	}

	/**
	 * An ETag that has moved on says so rather than removing what somebody else just wrote.
	 */
	@Test
	void deleteWithAStaleIfMatchIsRejected() {

		// Arrange
		final var existing = parameter("1", KEY).withVersion(2L);
		final var links = new ArrayList<>(List.of(linkTo(existing)));
		final var errandEntity = errand(existing);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.delete(errandEntity, links, KEY, "\"7\""));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		assertThat(links).hasSize(1);
		assertThat(errandEntity.getJsonParameters()).hasSize(1);
		verifyNoInteractions(entityManagerMock);
	}

	@Test
	void deletingAKeyTheArtefactDoesNotOwnGivesNotFound() {

		// Arrange
		final var errandEntity = errand();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.delete(errandEntity, new ArrayList<>(), KEY, null));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		verifyNoInteractions(entityManagerMock);
	}

	/**
	 * An errand whose parameters were never loaded is left alone rather than failing on a null collection.
	 */
	@Test
	void deleteToleratesAnErrandWithoutAParameterCollection() {

		// Arrange
		final var existing = parameter("1", KEY);
		final var links = new ArrayList<>(List.of(linkTo(existing)));

		// Act
		service.delete(ErrandEntity.create().withId(ERRAND_ID).withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID), links, KEY, null);

		// Verify
		assertThat(links).isEmpty();
	}
}
