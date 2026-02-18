package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.validation.ValidContactReason;
import se.sundsvall.supportmanagement.service.MetadataService;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.upperCase;

public class ValidContactReasonConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidContactReason, String> {

	private final MetadataService metadataService;
	private boolean nullable;

	public ValidContactReasonConstraintValidator(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Override
	public void initialize(final ValidContactReason constraintAnnotation) {
		this.nullable = constraintAnnotation.nullable();
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (isNull(value) && this.nullable) {
			return true;
		}

		final var namespace = getPathVariable(PATHVARIABLE_NAMESPACE);
		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);

		final var contactReasons = getContactReasons(namespace, municipalityId).stream()
			.map(String::toUpperCase)
			.toList();

		return contactReasons.contains(upperCase(value));
	}

	private List<String> getContactReasons(final String namespace, final String municipalityId) {
		return ofNullable(metadataService.findContactReasons(namespace, municipalityId)).orElse(emptyList()).stream()
			.map(ContactReason::getReason)
			.toList();
	}
}
