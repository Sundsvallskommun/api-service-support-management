package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import se.sundsvall.supportmanagement.api.model.metadata.ContactReason;
import se.sundsvall.supportmanagement.api.validation.ValidContactReason;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidContactReasonConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidContactReason, String> {

	private final MetadataService metadataService;
	private boolean nullable;

	public ValidContactReasonConstraintValidator(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	public void initialize(final ValidContactReason constraintAnnotation) {
		this.nullable = constraintAnnotation.nullable();
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (Objects.isNull(value) && this.nullable) {
			return true;
		}

		final var namespace = getPathVariable(PATHVARIABLE_NAMESPACE);
		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);

		final var contactReasons = getContactReasons(namespace, municipalityId).stream()
			.map(String::toUpperCase)
			.toList();

		return contactReasons.contains(value.toUpperCase());
	}

	private List<String> getContactReasons(final String namespace, final String municipalityId) {
		return ofNullable(metadataService.findContactReasons(namespace, municipalityId)).orElse(emptyList()).stream()
			.map(ContactReason::getReason)
			.toList();
	}
}
