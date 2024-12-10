package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportmanagement.api.validation.ValidMessageId;

public class ValidMessageIdConstraintValidator implements ConstraintValidator<ValidMessageId, String> {

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (value == null) {
			return false;
		}
		return value.startsWith("<") && value.endsWith(">") && value.contains("@");
	}

}
