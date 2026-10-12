package se.sundsvall.supportmanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.DecisionParameterEntity;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ErrandParameterMapperTest {

	@Test
	void toUniqueKeyList() {

		// Arrange
		final var parameterList = List.of(
			Parameter.create()
				.withDisplayName("displayNameA")
				.withGroup("groupA")
				.withKey("keyA")
				.withValues(List.of("value1", "value2", "value3")),
			Parameter.create()
				.withDisplayName("displayNameD")
				.withKey("keyC")
				.withValues(List.of("value4", "value5", "value6")),
			Parameter.create()
				.withDisplayName("displayNameB")
				.withKey("keyB")
				.withValues(List.of("value4", "value5", "value6")),
			Parameter.create()
				.withDisplayName("displayNameC")
				.withKey("keyA")
				.withValues(List.of("value7", "value8", "value9")));

		final var result = ErrandParameterMapper.toUniqueKeyList(parameterList);

		// Assert
		assertThat(result)
			.hasSize(3)
			.isEqualTo(List.of(
				Parameter.create()
					.withDisplayName("displayNameA")
					.withGroup("groupA")
					.withKey("keyA")
					.withValues(List.of("value1", "value2", "value3", "value7", "value8", "value9")),
				Parameter.create()
					.withDisplayName("displayNameD")
					.withKey("keyC")
					.withValues(List.of("value4", "value5", "value6")),
				Parameter.create()
					.withDisplayName("displayNameB")
					.withKey("keyB")
					.withValues(List.of("value4", "value5", "value6"))));
	}

	@Test
	void toUniqueKeyListSingleValuedParametersWithTheSameKey() {

		// Arrange
		final var parameterList = List.of(
			Parameter.create()
				.withDisplayName("displayNameA")
				.withGroup("groupA")
				.withKey("keyA")
				.withValues(List.of("value1")),
			Parameter.create()
				.withDisplayName("displayNameB")
				.withKey("keyA")
				.withValues(List.of("value2")),
			Parameter.create()
				.withDisplayName("displayNameC")
				.withKey("keyA")
				.withValues(List.of("value3")));

		final var result = ErrandParameterMapper.toUniqueKeyList(parameterList);

		// Assert
		assertThat(result)
			.hasSize(1)
			.isEqualTo(List.of(
				Parameter.create()
					.withDisplayName("displayNameA")
					.withGroup("groupA")
					.withKey("keyA")
					.withValues(List.of("value1", "value2", "value3"))));
	}

	@Test
	void toUniqueKeyListWhenNullValuesInOneElement() {

		// Arrange
		final var parameterList = List.of(
			Parameter.create()
				.withDisplayName("displayNameA")
				.withGroup("groupA")
				.withKey("keyA")
				.withValues(null), // List with null value in one parameter object.
			Parameter.create()
				.withDisplayName("displayNameB")
				.withKey("keyB")
				.withValues(List.of("value4", "value5", "value6")),
			Parameter.create()
				.withDisplayName("displayNameC")
				.withKey("keyA")
				.withValues(List.of("value7", "value8", "value9")));

		final var result = ErrandParameterMapper.toUniqueKeyList(parameterList);

		// Assert
		assertThat(result)
			.hasSize(2)
			.isEqualTo(List.of(
				Parameter.create()
					.withDisplayName("displayNameA")
					.withGroup("groupA")
					.withKey("keyA")
					.withValues(List.of("value7", "value8", "value9")),
				Parameter.create()
					.withDisplayName("displayNameB")
					.withKey("keyB")
					.withValues(List.of("value4", "value5", "value6"))));
	}

	@Test
	void toUniqueKeyListWhenNullValueInSingleElement() {

		// Arrange
		final var parameterList = List.of(
			Parameter.create()
				.withDisplayName("displayNameA")
				.withKey("keyA")
				.withValues(null));

		final var result = ErrandParameterMapper.toUniqueKeyList(parameterList);

		// Assert
		assertThat(result)
			.hasSize(1)
			.isEqualTo(List.of(
				Parameter.create()
					.withDisplayName("displayNameA")
					.withKey("keyA")
					.withValues(emptyList())));
	}

	@Test
	void toUniqueKeyListWhenInputIsEmpty() {

		// Act
		final var result = ErrandParameterMapper.toUniqueKeyList(emptyList());

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void toTrimmedUniqueKeyList() {

		// Arrange
		final var parameters = List.of(
			Parameter.create().withKey(" zone ").withDisplayName("Zon").withValues(List.of("A")),
			Parameter.create().withKey("ärende").withValues(List.of("b")),
			Parameter.create().withKey("Area").withGroup("place"),
			Parameter.create().withKey("zone").withDisplayName("ignored").withGroup("ignored").withValues(List.of("B")),
			Parameter.create().withKey("Zone").withValues(List.of("C")));

		// Act
		final var result = ErrandParameterMapper.toTrimmedUniqueKeyList(parameters);

		// Assert
		assertThat(result).containsExactly(
			Parameter.create().withKey("Area").withGroup("place").withValues(emptyList()),
			Parameter.create().withKey("Zone").withValues(List.of("C")),
			Parameter.create().withKey("zone").withDisplayName("Zon").withValues(List.of("A", "B")),
			Parameter.create().withKey("ärende").withValues(List.of("b")));
		assertThat(parameters.getFirst().getKey()).as("the sent parameters are left as they were").isEqualTo(" zone ");
	}

	@Test
	void toTrimmedUniqueKeyListWhenInputIsNull() {
		assertThat(ErrandParameterMapper.toTrimmedUniqueKeyList(null)).isEmpty();
	}

	@Test
	void toArtefactParameterEntities() {

		// Arrange
		final var decision = DecisionEntity.create().withId("decision-id");
		final var parameters = List.of(
			Parameter.create().withKey(" zone ").withDisplayName("Zon").withGroup("place").withValues(List.of("A")),
			Parameter.create().withKey("zone").withDisplayName("ignored").withValues(List.of("B")));

		// Act
		final var result = ErrandParameterMapper.toArtefactParameterEntities(parameters, () -> DecisionParameterEntity.create().withDecisionEntity(decision));

		// Assert
		assertThat(result).singleElement().satisfies(entity -> {
			assertThat(entity.getDecisionEntity()).isSameAs(decision);
			assertThat(entity.getKey()).isEqualTo("zone");
			assertThat(entity.getDisplayName()).isEqualTo("Zon");
			assertThat(entity.getParameterGroup()).isEqualTo("place");
			assertThat(entity.getValues()).containsExactly("A", "B");
		});
	}

	@Test
	void replaceArtefactParametersLeavesParametersComingOutTheSameUntouched() {

		// Arrange
		final var stored = new ArrayList<>(List.of(artefactParameter("b", "2"), artefactParameter("a", "1")));
		final var replacements = List.of(artefactParameter("a", "1"), artefactParameter("b", "2"));

		// Act
		final var replaced = ErrandParameterMapper.replaceArtefactParameters(stored, _ -> fail("the artefact already has a list"), replacements);

		// Assert
		assertThat(replaced).isFalse();
		assertThat(stored).extracting(DecisionParameterEntity::getKey).containsExactly("b", "a");
	}

	@Test
	void replaceArtefactParametersReplacesTheStoredOnesInPlace() {

		// Arrange
		final var stored = new ArrayList<>(List.of(artefactParameter("a", "1")));
		final var replacements = List.of(artefactParameter("a", "2"));

		// Act
		final var replaced = ErrandParameterMapper.replaceArtefactParameters(stored, _ -> fail("the artefact already has a list"), replacements);

		// Assert
		assertThat(replaced).isTrue();
		assertThat(stored).containsExactlyElementsOf(replacements);
	}

	@Test
	void replaceArtefactParametersGivesAnArtefactWithoutParametersAListOfItsOwn() {

		// Arrange
		final var given = new ArrayList<List<DecisionParameterEntity>>();
		final var replacements = List.of(artefactParameter("a", "1"));

		// Act
		final var replaced = ErrandParameterMapper.replaceArtefactParameters(null, given::add, replacements);

		// Assert
		assertThat(replaced).isTrue();
		assertThat(given).singleElement().satisfies(list -> assertThat(list).containsExactlyElementsOf(replacements));
	}

	@Test
	void toArtefactParametersWhenInputIsNull() {
		assertThat(ErrandParameterMapper.toArtefactParameters(null)).isEmpty();
		assertThat(ErrandParameterMapper.toArtefactParameter(null)).isNull();
	}

	private static DecisionParameterEntity artefactParameter(final String key, final String value) {
		return DecisionParameterEntity.create().withKey(key).withValues(List.of(value));
	}
}
