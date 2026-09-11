package se.sundsvall.supportmanagement.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.DecisionJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.ownedParameterIds;
import static se.sundsvall.supportmanagement.service.util.ArtefactJsonParameters.removeParameters;

@ExtendWith(MockitoExtension.class)
class ArtefactJsonParametersTest {

	private static final String PARAMETER_ID_1 = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String PARAMETER_ID_2 = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String PARAMETER_ID_3 = "8f79a808-0ef3-4985-99b9-b12f23e202a7";

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
}
