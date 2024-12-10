package se.sundsvall.supportmanagement.integration.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;
import se.sundsvall.supportmanagement.integration.db.model.enums.CommunicationType;

@Converter(autoApply = true)
public class CommunicationTypeConverter implements AttributeConverter<CommunicationType, String> {

	@Override
	public String convertToDatabaseColumn(CommunicationType attribute) {
		return Optional.ofNullable(attribute).map(CommunicationType::toString).orElse(null);
	}

	@Override
	public CommunicationType convertToEntityAttribute(String dbData) {
		return CommunicationType.valueOf(dbData);
	}
}
