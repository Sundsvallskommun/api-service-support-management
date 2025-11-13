package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.validation.ValidClassificationCreate;
import se.sundsvall.supportmanagement.service.MetadataService;

public class ValidClassificationCreateConstraintValidator extends ValidClassificationConstraintValidator implements ConstraintValidator<ValidClassificationCreate, Classification> {
	public ValidClassificationCreateConstraintValidator(MetadataService metadataService) {
		super(metadataService);
	}

	@Override
	public void initialize(ValidClassificationCreate validClassificationCreate) {
		super.setNullableIfActive(validClassificationCreate.nullableIfActive());
	}

	@Override
	public boolean isValid(Classification classification, ConstraintValidatorContext constraintValidatorContext) {
		return super.isValid(classification, constraintValidatorContext);
	}
}
