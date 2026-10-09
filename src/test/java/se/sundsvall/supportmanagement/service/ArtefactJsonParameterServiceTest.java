package se.sundsvall.supportmanagement.service;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

@ExtendWith(MockitoExtension.class)
class ArtefactJsonParameterServiceTest {

	private static final String KEY = "responseForm";
	private static final String SCHEMA_ID = "test-schema-1.0";
	private static final String NEW_SCHEMA_ID = "test-schema-2.0";
	private static final String NEW_VALUE = "{\"answer\":\"given\"}";

	private static final Supplier<StatementJsonParameterEntity> NOT_CALLED = () -> {
		throw new AssertionError("a parameter the owner already holds is not created again");
	};

	@Mock
	private EntityManager entityManagerMock;

	@InjectMocks
	private ArtefactJsonParameterService service;

	private static StatementJsonParameterEntity parameter(final String key) {
		return StatementJsonParameterEntity.create()
			.withId("id-" + key)
			.withKey(key)
			.withSchemaId(SCHEMA_ID)
			.withValue("{\"answer\":\"pending\"}")
			.withVersion(2L);
	}

	private static JsonParameter body(final String key) {
		return JsonParameter.create()
			.withKey(key)
			.withSchemaId(NEW_SCHEMA_ID)
			.withValue(JsonNodeFactory.instance.objectNode().put("answer", "given"));
	}

	/**
	 * The parameters are mapped in the order the owner holds them, which is the order of their keys.
	 */
	@Test
	void readAll() {

		// Arrange
		final var parameters = List.of(parameter("alpha"), parameter("zeta"));

		// Act
		final var result = service.readAll(parameters);

		// Verify
		assertThat(result).extracting(JsonParameter::getKey, JsonParameter::getSchemaId, JsonParameter::getVersion)
			.containsExactly(tuple("alpha", SCHEMA_ID, 2L), tuple("zeta", SCHEMA_ID, 2L));
		assertThat(result.getFirst().getValue()).isEqualTo(JsonNodeFactory.instance.objectNode().put("answer", "pending"));
		verifyNoInteractions(entityManagerMock);
	}

	/**
	 * An owner that has never held a parameter has no collection yet.
	 */
	@Test
	void readAllOfNothingIsEmpty() {

		// Act & Verify
		assertThat(service.readAll(null)).isEmpty();
	}

	/**
	 * Keys are compared the way the database compares them, without regard to case.
	 */
	@Test
	void read() {

		// Arrange
		final var parameters = List.of(parameter("other"), parameter(KEY));

		// Act
		final var result = service.read(parameters, "RESPONSEFORM");

		// Verify
		assertThat(result.getKey()).isEqualTo(KEY);
		assertThat(result.getVersion()).isEqualTo(2L);
	}

	@Test
	void readingAKeyTheOwnerDoesNotHoldGivesNotFound() {

		// Arrange
		final var parameters = List.of(parameter("other"));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.read(parameters, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(KEY);
	}

	@Test
	void readingFromAnOwnerWithoutParametersGivesNotFound() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.read(null, KEY));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
	}

	/**
	 * The response is mapped once the flush has written the parameter, so that it carries the version the ETag is to be
	 * taken from.
	 */
	@Test
	void upsertReplacesAKeyTheOwnerHolds() {

		// Arrange
		final var existing = parameter(KEY);
		final var parameters = new ArrayList<>(List.of(existing));
		doAnswer(_ -> {
			existing.setVersion(3L);
			return null;
		}).when(entityManagerMock).flush();

		// Act
		final var result = service.upsert(parameters, NOT_CALLED, "\"2\"", body(KEY));

		// Verify
		assertThat(result.created()).isFalse();
		assertThat(parameters).containsExactly(existing);
		assertThat(existing.getSchemaId()).isEqualTo(NEW_SCHEMA_ID);
		assertThat(existing.getValue()).isEqualTo(NEW_VALUE);
		assertThat(result.jsonParameter().getKey()).isEqualTo(KEY);
		assertThat(result.jsonParameter().getVersion()).as("the version the flush wrote").isEqualTo(3L);
		verify(entityManagerMock).flush();
	}

	/**
	 * A key differing only in case is the one the owner holds, and the parameter keeps the key it was stored under.
	 */
	@Test
	void upsertMatchesTheKeyWithoutRegardToCase() {

		// Arrange
		final var existing = parameter(KEY);
		final var parameters = new ArrayList<>(List.of(existing));

		// Act
		final var result = service.upsert(parameters, NOT_CALLED, null, body("RESPONSEFORM"));

		// Verify
		assertThat(result.created()).isFalse();
		assertThat(parameters).containsExactly(existing);
		assertThat(existing.getKey()).isEqualTo(KEY);
		assertThat(result.jsonParameter().getKey()).isEqualTo(KEY);
		verify(entityManagerMock).flush();
	}

	/**
	 * An ETag that has moved on is refused, and what somebody else just wrote is left as it stands.
	 */
	@Test
	void upsertWithAStaleIfMatchIsRejected() {

		// Arrange
		final var existing = parameter(KEY);
		final var parameters = new ArrayList<>(List.of(existing));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.upsert(parameters, NOT_CALLED, "\"1\"", body(KEY)));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		assertThat(existing.getSchemaId()).isEqualTo(SCHEMA_ID);
		assertThat(existing.getValue()).isEqualTo("{\"answer\":\"pending\"}");
		verifyNoInteractions(entityManagerMock);
	}

	/**
	 * A new parameter is created by the owner's factory, so that it points at the owner, and is added to the owner's
	 * collection, which is what persists it.
	 */
	@Test
	void upsertCreatesAKeyTheOwnerDoesNotHold() {

		// Arrange
		final var owner = StatementEntity.create().withId("statement-id");
		final var other = parameter("other");
		final var parameters = new ArrayList<>(List.of(other));

		// Act
		final var result = service.upsert(parameters, () -> StatementJsonParameterEntity.create().withStatementEntity(owner), null, body(KEY));

		// Verify
		assertThat(result.created()).isTrue();
		assertThat(result.jsonParameter().getKey()).isEqualTo(KEY);
		assertThat(parameters).hasSize(2);
		assertThat(parameters.getLast()).satisfies(created -> {
			assertThat(created.getStatementEntity()).isSameAs(owner);
			assertThat(created.getKey()).isEqualTo(KEY);
			assertThat(created.getSchemaId()).isEqualTo(NEW_SCHEMA_ID);
			assertThat(created.getValue()).isEqualTo(NEW_VALUE);
		});
		assertThat(other.getSchemaId()).as("the other parameter is left as it was").isEqualTo(SCHEMA_ID);
		verify(entityManagerMock).flush();
	}

	/**
	 * A key the owner does not hold is created whatever If-Match the request carries.
	 */
	@Test
	void upsertOfANewKeyDoesNotHoldItToIfMatch() {

		// Arrange
		final var parameters = new ArrayList<StatementJsonParameterEntity>();

		// Act
		final var result = service.upsert(parameters, StatementJsonParameterEntity::create, "\"7\"", body(KEY));

		// Verify
		assertThat(result.created()).isTrue();
		assertThat(parameters).extracting(StatementJsonParameterEntity::getKey).containsExactly(KEY);
		verify(entityManagerMock).flush();
	}

	@Test
	void deleteRemovesTheParameterFromTheOwner() {

		// Arrange
		final var existing = parameter(KEY);
		final var other = parameter("other");
		final var parameters = new ArrayList<>(List.of(existing, other));

		// Act
		service.delete(parameters, "responseform", "\"2\"");

		// Verify
		assertThat(parameters).containsExactly(other);
		verify(entityManagerMock).flush();
	}

	/**
	 * If-Match is opt-in. A request without one is let through.
	 */
	@Test
	void deleteWithoutIfMatch() {

		// Arrange
		final var parameters = new ArrayList<>(List.of(parameter(KEY)));

		// Act
		service.delete(parameters, KEY, null);

		// Verify
		assertThat(parameters).isEmpty();
		verify(entityManagerMock).flush();
	}

	@Test
	void deletingAKeyTheOwnerDoesNotHoldGivesNotFound() {

		// Arrange
		final var parameters = new ArrayList<>(List.of(parameter("other")));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.delete(parameters, KEY, null));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(KEY);
		assertThat(parameters).hasSize(1);
		verifyNoInteractions(entityManagerMock);
	}

	/**
	 * An ETag that has moved on is refused, and what somebody else just wrote is not removed.
	 */
	@Test
	void deleteWithAStaleIfMatchIsRejected() {

		// Arrange
		final var existing = parameter(KEY);
		final var parameters = new ArrayList<>(List.of(existing));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.delete(parameters, KEY, "\"7\""));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(PRECONDITION_FAILED);
		assertThat(parameters).containsExactly(existing);
		verifyNoInteractions(entityManagerMock);
	}
}
