package se.sundsvall.supportmanagement.integration.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.sundsvall.supportmanagement.integration.db.model.subscriber.DbSubscriptionTargetType;

import static java.util.Optional.ofNullable;

@Converter(autoApply = true)
public class DbSubscriptionTargetTypeConverter implements AttributeConverter<DbSubscriptionTargetType, String> {

	@Override
	public String convertToDatabaseColumn(DbSubscriptionTargetType attribute) {
		return ofNullable(attribute).map(DbSubscriptionTargetType::name).orElse(null);
	}

	@Override
	public DbSubscriptionTargetType convertToEntityAttribute(String dbData) {
		return ofNullable(dbData).map(DbSubscriptionTargetType::valueOf).orElse(null);
	}
}
