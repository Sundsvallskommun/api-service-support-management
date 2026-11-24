package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportmanagement.api.validation.ValidResolution;
import se.sundsvall.supportmanagement.service.ResolutionValidator;

public class ValidResolutionConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidResolution, String> {

	static final String PATHVARIABLE_ERRAND_ID = "errandId";

	private final ResolutionValidator resolutionValidator;

	public ValidResolutionConstraintValidator(final ResolutionValidator resolutionValidator) {
		this.resolutionValidator = resolutionValidator;
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return resolutionValidator.isValid(value,
			getPathVariable(PATHVARIABLE_NAMESPACE),
			getPathVariable(PATHVARIABLE_MUNICIPALITY_ID),
			findPathVariable(PATHVARIABLE_ERRAND_ID).orElse(null));
	}
}
