package se.sundsvall.supportmanagement.integration.db.converter;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTargetTypeConverterTest {

	private final SubscriptionTargetTypeConverter converter = new SubscriptionTargetTypeConverter();

	@Test
	void convertToDatabaseColumn() {
		assertThat(converter.convertToDatabaseColumn(SubscriptionTargetType.ERRAND)).isEqualTo("ERRAND");
		assertThat(converter.convertToDatabaseColumn(SubscriptionTargetType.NAMESPACE)).isEqualTo("NAMESPACE");
	}

	@Test
	void convertToDatabaseColumnHandlesNull() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
	}

	@Test
	void convertToEntityAttribute() {
		assertThat(converter.convertToEntityAttribute("ERRAND")).isEqualTo(SubscriptionTargetType.ERRAND);
		assertThat(converter.convertToEntityAttribute("NAMESPACE")).isEqualTo(SubscriptionTargetType.NAMESPACE);
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
