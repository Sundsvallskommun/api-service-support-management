package se.sundsvall.supportmanagement.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.ownedParameterIds;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.withoutArtefactParameters;

@ExtendWith(MockitoExtension.class)
class ArtefactJsonParametersTest {

	private static final String PARAMETER_ID_1 = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String PARAMETER_ID_2 = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String PARAMETER_ID_3 = "8f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String ARTEFACT_KEY = "responseForm";
	private static final String ERRAND_KEY = "formData";
	private static final String SCHEMA_ID = "test-schema-1.0";

	@Mock
	private ErrandEntity errandEntityMock;

	private static DecisionJsonParameterEntity link(final JsonParameterEntity parameter) {
		return DecisionJsonParameterEntity.create().withJsonParameterEntity(parameter);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void ownedParameterIdsOfNoLinks(final List<DecisionJsonParameterEntity> links) {

		// Act & Verify
		assertThat(ownedParameterIds(links)).isEmpty();
	}

	@Test
	void ownedParameterIdsOfSeveralLinks() {

		// Arrange
		final var links = List.of(
			link(JsonParameterEntity.create().withId(PARAMETER_ID_1)),
			link(JsonParameterEntity.create().withId(PARAMETER_ID_2)));

		// Act
		final var result = ownedParameterIds(links);

		// Verify
		assertThat(result).containsExactlyInAnyOrder(PARAMETER_ID_1, PARAMETER_ID_2);
	}

	@Test
	void ownedParameterIdsSkipsLinksWithoutParameter() {

		// Arrange
		final var links = List.of(link(null), link(JsonParameterEntity.create().withId(PARAMETER_ID_1)));

		// Act
		final var result = ownedParameterIds(links);

		// Verify
		assertThat(result).containsExactly(PARAMETER_ID_1);
	}

	@Test
	void removeParametersById() {

		// Arrange
		final var first = JsonParameterEntity.create().withId(PARAMETER_ID_1).withKey("first");
		final var second = JsonParameterEntity.create().withId(PARAMETER_ID_2).withKey("second");
		final var third = JsonParameterEntity.create().withId(PARAMETER_ID_3).withKey("third");
		final var errandEntity = ErrandEntity.create().withJsonParameters(new ArrayList<>(List.of(first, second, third)));

		// Act
		removeParameters(errandEntity, Set.of(PARAMETER_ID_1, PARAMETER_ID_3));

		// Verify
		assertThat(errandEntity.getJsonParameters()).containsExactly(second);
	}

	@Test
	void removeParametersLeavesAnotherWithTheSameKeyAndValue() {

		// Arrange
		final var named = JsonParameterEntity.create().withId(PARAMETER_ID_1).withKey("data").withSchemaId("schema").withValue("{}");
		final var twin = JsonParameterEntity.create().withId(PARAMETER_ID_2).withKey("data").withSchemaId("schema").withValue("{}");
		final var errandEntity = ErrandEntity.create().withJsonParameters(new ArrayList<>(List.of(named, twin)));

		// Act
		removeParameters(errandEntity, Set.of(PARAMETER_ID_1));

		// Verify
		assertThat(errandEntity.getJsonParameters()).singleElement().isSameAs(twin);
	}

	/**
	 * An artefact owning no parameters leaves the collection of the errand alone, which would otherwise be loaded for
	 * nothing.
	 */
	@Test
	void removeParametersWithNothingToRemove() {

		// Act
		removeParameters(errandEntityMock, Set.of());

		// Verify
		verifyNoInteractions(errandEntityMock);
	}

	@Test
	void removeParametersFromErrandWithoutParameters() {

		// Arrange
		final var errandEntity = ErrandEntity.create();

		// Act
		removeParameters(errandEntity, Set.of(PARAMETER_ID_1));

		// Verify
		assertThat(errandEntity.getJsonParameters()).isNull();
	}

	/**
	 * A patch leaving the parameters of the errand alone asks nothing of them, and loads none of their links.
	 */
	@Test
	void withoutArtefactParametersWhenThePatchLeavesThemAlone() {

		// Arrange
		final Function<ErrandField, Predicate<String>> writableKey = _ -> _ -> true;

		// Act
		final var result = withoutArtefactParameters(errandEntityMock, null, writableKey);

		// Verify
		assertThat(result).isSameAs(writableKey);
		verifyNoInteractions(errandEntityMock);
	}

	/**
	 * A patch replacing the parameters of the errand without naming the one a statement owns leaves it where it is - the
	 * key is simply not the patch's to change.
	 */
	@Test
	void anArtefactParameterIsNotWritableThroughTheErrand() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withJsonParameters(new ArrayList<>(List.of(artefactOwned(), errandOwned())));

		// Act
		final var result = withoutArtefactParameters(errandEntity, List.of(patched(ERRAND_KEY, "changed")), _ -> _ -> true);

		// Verify
		assertThat(result.apply(ErrandField.JSON_PARAMETERS))
			.accepts(ERRAND_KEY)
			.rejects(ARTEFACT_KEY, ARTEFACT_KEY.toUpperCase());
		assertThat(result.apply(ErrandField.PARAMETERS)).as("other fields are not the artefacts' business").accepts(ARTEFACT_KEY);
	}

	/**
	 * A caller patching back what they were served carries the parameter as it stands, which changes nothing.
	 */
	@Test
	void carryingAnArtefactParameterAsItStandsIsNotAChange() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withJsonParameters(new ArrayList<>(List.of(artefactOwned(), errandOwned())));

		// Act
		final var result = withoutArtefactParameters(errandEntity, List.of(patched(ARTEFACT_KEY, "pending"), patched(ERRAND_KEY, "pending")), _ -> _ -> true);

		// Verify
		assertThat(result.apply(ErrandField.JSON_PARAMETERS)).rejects(ARTEFACT_KEY);
	}

	/**
	 * Changing the content of an artefact through the errand is refused rather than ignored: the caller is told where it
	 * is written. The key is matched without regard to case, as the database matches it.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		ARTEFACT_KEY, "RESPONSEFORM"
	})
	void changingAnArtefactParameterThroughTheErrandIsAConflict(final String key) {

		// Arrange
		final var errandEntity = ErrandEntity.create().withJsonParameters(new ArrayList<>(List.of(artefactOwned(), errandOwned())));
		final var jsonParameters = List.of(patched(key, "changed"));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> withoutArtefactParameters(errandEntity, jsonParameters, _ -> _ -> true));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(key);
	}

	/**
	 * What the caller may not change stays that way; the artefacts only take keys away.
	 */
	@Test
	void keysTheCallerMayNotChangeStaySo() {

		// Arrange
		final var errandEntity = ErrandEntity.create().withJsonParameters(new ArrayList<>(List.of(errandOwned())));

		// Act
		final var result = withoutArtefactParameters(errandEntity, List.of(), _ -> key -> !ERRAND_KEY.equals(key));

		// Verify
		assertThat(result.apply(ErrandField.JSON_PARAMETERS)).rejects(ERRAND_KEY).accepts("another");
	}

	private static JsonParameterEntity artefactOwned() {
		return JsonParameterEntity.create().withId(PARAMETER_ID_1).withKey(ARTEFACT_KEY).withSchemaId(SCHEMA_ID).withValue("{\"answer\":\"pending\"}")
			.withStatementLinks(List.of(StatementJsonParameterEntity.create()));
	}

	private static JsonParameterEntity errandOwned() {
		return JsonParameterEntity.create().withId(PARAMETER_ID_2).withKey(ERRAND_KEY).withSchemaId(SCHEMA_ID).withValue("{\"answer\":\"pending\"}");
	}

	private static JsonParameter patched(final String key, final String answer) {
		return JsonParameter.create().withKey(key).withSchemaId(SCHEMA_ID).withValue(JsonNodeFactory.instance.objectNode().put("answer", answer));
	}
}
