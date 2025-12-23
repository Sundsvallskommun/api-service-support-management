package se.sundsvall.supportmanagement.integration.db.converter;

import static java.util.Optional.ofNullable;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;

@Converter(autoApply = true)
public class ValueTypeConverter implements AttributeConverter<ValueType, String> {

	@Override
	public String convertToDatabaseColumn(ValueType attribute) {
		return ofNullable(attribute).map(ValueType::name).orElse(null);
	}

	@Override
	public ValueType convertToEntityAttribute(String dbData) {
		return ValueType.valueOf(dbData);
	}
}
