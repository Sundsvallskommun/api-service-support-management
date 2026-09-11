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
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.StatementJsonParameterRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

@ExtendWith(MockitoExtension.class)
class ArtefactJsonParameterServiceTest {

	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String KEY = "responseForm";

	private static final Function<JsonParameterEntity, StatementJsonParameterEntity> LINK_FACTORY = parameter -> StatementJsonParameterEntity.create().withJsonParameterEntity(parameter);

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

	private static JsonParameter body() {
		return JsonParameter.create().withSchemaId("test-schema-1.0").withValue(JsonNodeFactory.instance.objectNode().put("name", "test"));
	}

	@Test
	void readAllSortsByKey() {

		// Arrange
		final var links = List.of(linkTo(parameter("2", "zeta")), linkTo(parameter("1", "alpha")));

		// Act
		final var result = service.readAll(links);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly("alpha", "zeta");
	}

	/**
	 * A link whose parameter has gone is skipped rather than mapped to a null.
	 */
	@Test
	void readAllSkipsLinksWithoutAParameter() {

		// Act
		final var result = service.readAll(List.of(StatementJsonParameterEntity.create(), linkTo(parameter("1", KEY))));

		// Verify
		assertThat(result).extracting(JsonParameter::getKey).containsExactly(KEY);
	}

	@Test
	void readAllOfNothingIsEmpty() {

		// Act & Verify
		assertThat(service.readAll(null)).isEmpty();
	}

	@Test
	void read() {

		// Act
		final var result = service.read(List.of(linkTo(parameter("1", KEY))), KEY);

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
	}

	/**
	 * A key belonging to another artefact, or to nothing at all, is not this artefact's to read.
	 */
	@Test
	void readingAKeyTheArtefactDoesNotOwnGivesNotFound() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.read(List.of(linkTo(parameter("1", "somethingElse"))), KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(KEY);
	}

	@Test
	void upsertReplacesAKeyTheArtefactAlreadyOwns() {

		// Arrange
		final var existing = parameter("1", KEY);
		final var links = new ArrayList<>(List.of(linkTo(existing)));
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>(List.of(existing)));

		// Act
		final var result = service.upsert(errandEntity, KEY, null, body(), links, LINK_FACTORY, linkRepositoryMock);

		// Verify
		assertThat(result.created()).isFalse();
		assertThat(existing.getValue()).contains("test");
		assertThat(links).as("no second link was written").hasSize(1);
		verify(entityManagerMock).flush();
		verifyNoInteractions(linkRepositoryMock);
	}

	@Test
	void upsertCreatesTheParameterAndItsLinkForANewKey() {

		// Arrange
		final var links = new ArrayList<StatementJsonParameterEntity>();
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>());
		when(linkRepositoryMock.save(any(StatementJsonParameterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		final var result = service.upsert(errandEntity, KEY, null, body(), links, LINK_FACTORY, linkRepositoryMock);

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
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);
		when(linkRepositoryMock.save(any(StatementJsonParameterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		service.upsert(errandEntity, KEY, null, body(), new ArrayList<>(), LINK_FACTORY, linkRepositoryMock);

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
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>(List.of(parameter("9", KEY))));
		final var links = new ArrayList<StatementJsonParameterEntity>();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.upsert(errandEntity, KEY, null, body(), links, LINK_FACTORY, linkRepositoryMock));

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
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>(List.of(existing)));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.upsert(errandEntity, KEY, "\"2\"", body(), links, LINK_FACTORY, linkRepositoryMock));

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
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>(List.of(existing, parameter("3", "errandOwned"))));

		// Act
		service.delete(errandEntity, links, KEY, null);

		// Verify
		assertThat(links).extracting(link -> link.getJsonParameterEntity().getKey()).containsExactly("other");
		assertThat(errandEntity.getJsonParameters()).extracting(JsonParameterEntity::getKey).containsExactly("errandOwned");
		verify(entityManagerMock, times(2)).flush();
	}

	/**
	 * An ETag that has moved on says so rather than removing what somebody else just wrote.
	 */
	@Test
	void deleteWithAStaleIfMatchIsRejected() {

		// Arrange
		final var existing = parameter("1", KEY).withVersion(2L);
		final var links = new ArrayList<>(List.of(linkTo(existing)));
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID).withJsonParameters(new ArrayList<>(List.of(existing)));

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
		final var errandEntity = ErrandEntity.create().withId(ERRAND_ID);

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
		service.delete(ErrandEntity.create().withId(ERRAND_ID), links, KEY, null);

		// Verify
		assertThat(links).isEmpty();
	}
}
