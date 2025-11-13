package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.validation.ValidClassificationUpdate;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidClassificationUpdateConstraintValidator extends ValidClassificationConstraintValidator implements ConstraintValidator<ValidClassificationUpdate, Classification> {
	public ValidClassificationUpdateConstraintValidator(MetadataService metadataService) {
		super(metadataService);
	}

	@Override
	public void initialize(ValidClassificationUpdate validClassificationCreate) {
		super.setNullableIfActive(validClassificationCreate.nullableIfActive());
	}

	@Override
	public boolean isValid(Classification classification, ConstraintValidatorContext constraintValidatorContext) {
		return super.isValid(classification, constraintValidatorContext);
	}
}
