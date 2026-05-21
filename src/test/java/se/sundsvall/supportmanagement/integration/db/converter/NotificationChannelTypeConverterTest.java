package se.sundsvall.supportmanagement.integration.db.converter;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationChannelTypeConverterTest {

	private final NotificationChannelTypeConverter converter = new NotificationChannelTypeConverter();

	@Test
	void convertToDatabaseColumn() {
		assertThat(converter.convertToDatabaseColumn(NotificationChannelType.EMAIL)).isEqualTo("EMAIL");
		assertThat(converter.convertToDatabaseColumn(NotificationChannelType.INTERNAL)).isEqualTo("INTERNAL");
		assertThat(converter.convertToDatabaseColumn(NotificationChannelType.SMS)).isEqualTo("SMS");
	}

	@Test
	void convertToDatabaseColumnHandlesNull() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
	}

	@Test
	void convertToEntityAttribute() {
		assertThat(converter.convertToEntityAttribute("EMAIL")).isEqualTo(NotificationChannelType.EMAIL);
		assertThat(converter.convertToEntityAttribute("INTERNAL")).isEqualTo(NotificationChannelType.INTERNAL);
		assertThat(converter.convertToEntityAttribute("SMS")).isEqualTo(NotificationChannelType.SMS);
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
