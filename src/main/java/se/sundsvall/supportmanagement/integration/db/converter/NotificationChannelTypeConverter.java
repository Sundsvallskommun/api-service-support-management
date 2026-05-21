package se.sundsvall.supportmanagement.integration.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType;

import static java.util.Optional.ofNullable;

@Converter(autoApply = true)
public class NotificationChannelTypeConverter implements AttributeConverter<NotificationChannelType, String> {

	@Override
	public String convertToDatabaseColumn(NotificationChannelType attribute) {
		return ofNullable(attribute).map(NotificationChannelType::name).orElse(null);
	}

	@Override
	public NotificationChannelType convertToEntityAttribute(String dbData) {
		return ofNullable(dbData).map(NotificationChannelType::valueOf).orElse(null);
	}
}
