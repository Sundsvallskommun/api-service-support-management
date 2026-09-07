package se.sundsvall.supportmanagement.service.mapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.api.model.config.Validation;
import se.sundsvall.supportmanagement.integration.db.model.ValidationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@Component
public class ValidationMapper {

	/**
	 * Maps the stored validations to a complete list, holding one entry per existing {@link EntityType}. Types without a
	 * stored entity are returned as not validated, which is how they are treated when errands are validated.
	 */
	public List<Validation> toValidations(final List<ValidationEntity> entities) {
		final var entityByType = ofNullable(entities).orElse(emptyList()).stream()
			.collect(toMap(ValidationEntity::getType, Function.identity(), (first, _) -> first));

		return stream(EntityType.values())
			.map(type -> toValidation(type, entityByType))
			.toList();
	}

	public Validation toValidation(final ValidationEntity entity) {
		return Validation.create()
			.withType(entity.getType())
			.withValidated(entity.isValidated())
			.withCreated(entity.getCreated())
			.withModified(entity.getModified());
	}

	public ValidationEntity toEntity(final String namespace, final String municipalityId, final EntityType type, final boolean validated) {
		return ValidationEntity.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withType(type)
			.withValidated(validated);
	}

	private Validation toValidation(final EntityType type, final Map<EntityType, ValidationEntity> entityByType) {
		return ofNullable(entityByType.get(type))
			.map(this::toValidation)
			.orElseGet(() -> Validation.create()
				.withType(type)
				.withValidated(false));
	}
}
