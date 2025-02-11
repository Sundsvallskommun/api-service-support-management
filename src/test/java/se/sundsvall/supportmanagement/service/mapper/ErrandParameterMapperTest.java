package se.sundsvall.supportmanagement.service.mapper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;

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
}
