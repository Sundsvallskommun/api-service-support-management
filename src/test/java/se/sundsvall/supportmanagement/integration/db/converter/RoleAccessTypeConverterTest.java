package se.sundsvall.supportmanagement.integration.db.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.sundsvall.supportmanagement.integration.db.model.enums.RoleAccessType;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAccessTypeConverterTest {

	private final RoleAccessTypeConverter converter = new RoleAccessTypeConverter();

	@ParameterizedTest
	@EnumSource(RoleAccessType.class)
	void convertRoundTrip(final RoleAccessType type) {
		assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(type))).isEqualTo(type);
	}

	@Test
	void convertNullToDatabaseColumn() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
	}
}
