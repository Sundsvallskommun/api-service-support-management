package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import se.sundsvall.supportmanagement.api.model.metadata.Status;
import se.sundsvall.supportmanagement.api.validation.ValidStatus;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidStatusConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidStatus, String> {

	private final MetadataService metadataService;

	public ValidStatusConstraintValidator(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return isValid(value, getStatusNames(), context);
	}

	private List<String> getStatusNames() {
		return ofNullable(metadataService.findStatuses(getPathVariable(PATHVARIABLE_NAMESPACE), getPathVariable(PATHVARIABLE_MUNICIPALITY_ID))).orElse(emptyList()).stream()
			.map(Status::getName)
			.toList();
	}
}
