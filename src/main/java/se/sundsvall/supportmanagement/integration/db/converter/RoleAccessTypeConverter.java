package se.sundsvall.supportmanagement.integration.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import se.sundsvall.supportmanagement.integration.db.model.enums.RoleAccessType;

import static java.util.Optional.ofNullable;

@Converter(autoApply = true)
public class RoleAccessTypeConverter implements AttributeConverter<RoleAccessType, String> {

	@Override
	public String convertToDatabaseColumn(RoleAccessType attribute) {
		return ofNullable(attribute).map(RoleAccessType::name).orElse(null);
	}

	@Override
	public RoleAccessType convertToEntityAttribute(String dbData) {
		return RoleAccessType.valueOf(dbData);
	}
}
