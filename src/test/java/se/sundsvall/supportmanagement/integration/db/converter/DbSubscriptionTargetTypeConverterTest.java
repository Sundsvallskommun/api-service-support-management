package se.sundsvall.supportmanagement.integration.db.converter;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DbSubscriptionTargetTypeConverterTest {

	private final DbSubscriptionTargetTypeConverter converter = new DbSubscriptionTargetTypeConverter();

	@Test
	void convertToDatabaseColumn() {
		assertThat(converter.convertToDatabaseColumn(DbSubscriptionTargetType.ERRAND)).isEqualTo("ERRAND");
		assertThat(converter.convertToDatabaseColumn(DbSubscriptionTargetType.NAMESPACE)).isEqualTo("NAMESPACE");
	}

	@Test
	void convertToDatabaseColumnHandlesNull() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
	}

	@Test
	void convertToEntityAttribute() {
		assertThat(converter.convertToEntityAttribute("ERRAND")).isEqualTo(DbSubscriptionTargetType.ERRAND);
		assertThat(converter.convertToEntityAttribute("NAMESPACE")).isEqualTo(DbSubscriptionTargetType.NAMESPACE);
	}

	@Test
	void convertToEntityAttributeHandlesNull() {
		assertThat(converter.convertToEntityAttribute(null)).isNull();
	}

	@Test
	void convertToEntityAttributeRejectsUnknownValue() {
		assertThatThrownBy(() -> converter.convertToEntityAttribute("BOGUS"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
