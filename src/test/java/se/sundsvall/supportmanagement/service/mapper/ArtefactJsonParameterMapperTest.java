package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.MeasureJsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementJsonParameterEntity;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactJsonParameterMapper.toJsonParameter;
import static se.sundsvall.supportmanagement.service.mapper.ArtefactJsonParameterMapper.toJsonParameters;

class ArtefactJsonParameterMapperTest {

	@Test
	void toJsonParameterMapsEveryField() {

		// Arrange
		final var entity = MeasureJsonParameterEntity.create()
			.withId("id")
			.withKey("measureForm")
			.withSchemaId("test-schema-1.0")
			.withValue("{\"answer\":\"pending\"}")
			.withVersion(2L);

		// Act
		final var result = toJsonParameter(entity);

		// Assert
		assertThat(result).isEqualTo(JsonParameter.create()
			.withKey("measureForm")
			.withSchemaId("test-schema-1.0")
			.withValue(JsonNodeFactory.instance.objectNode().put("answer", "pending"))
			.withVersion(2L));
	}

	@Test
	void aParameterWithoutAValueMapsToNoValue() {
		assertThat(toJsonParameter(StatementJsonParameterEntity.create().withKey("responseForm")).getValue()).isNull();
	}

	/**
	 * The owner reads its parameters ordered by key, and that order is what is served.
	 */
	@Test
	void toJsonParametersKeepsTheOrderOfTheOwner() {

		// Arrange
		final var entities = List.of(
			StatementJsonParameterEntity.create().withKey("alpha").withValue("{}"),
			StatementJsonParameterEntity.create().withKey("zeta").withValue("{}"));

		// Act
		final var result = toJsonParameters(entities);

		// Assert
		assertThat(result).extracting(JsonParameter::getKey).containsExactly("alpha", "zeta");
	}

	@Test
	void toJsonParametersOfNothingIsEmpty() {
		assertThat(toJsonParameters(null)).isEmpty();
	}
}
