package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.springframework.data.domain.Sort;
import se.sundsvall.supportmanagement.api.model.metadata.ResolutionCode;
import se.sundsvall.supportmanagement.api.validation.ValidResolution;
import se.sundsvall.supportmanagement.model.enums.EntityType;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class ValidResolutionConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidResolution, String> {

	private final MetadataService metadataService;

	public ValidResolutionConstraintValidator(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		final var namespace = getPathVariable(PATHVARIABLE_NAMESPACE);
		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);
		return !metadataService.isValidated(namespace, municipalityId, EntityType.RESOLUTION_CODE) ||
			isValid(value, getResolutionCodes(namespace, municipalityId), context);
	}

	private List<String> getResolutionCodes(final String namespace, final String municipalityId) {
		return ofNullable(metadataService.findResolutionCodes(namespace, municipalityId, Sort.unsorted())).orElse(emptyList()).stream()
			.map(ResolutionCode::getName)
			.toList();
	}
}
