package se.sundsvall.supportmanagement.integration.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.SubscriptionTargetType;

import static java.util.Optional.ofNullable;

@Converter(autoApply = true)
public class SubscriptionTargetTypeConverter implements AttributeConverter<SubscriptionTargetType, String> {

	@Override
	public String convertToDatabaseColumn(SubscriptionTargetType attribute) {
		return ofNullable(attribute).map(SubscriptionTargetType::name).orElse(null);
	}

	@Override
	public SubscriptionTargetType convertToEntityAttribute(String dbData) {
		return ofNullable(dbData).map(SubscriptionTargetType::valueOf).orElse(null);
	}
}
