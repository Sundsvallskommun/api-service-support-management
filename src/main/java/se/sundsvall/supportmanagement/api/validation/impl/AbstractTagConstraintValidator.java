package se.sundsvall.supportmanagement.api.validation.impl;

import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import javax.validation.ConstraintValidatorContext;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

abstract class AbstractTagConstraintValidator {
	private static final String CUSTOM_ERROR_MESSAGE_TEMPLATE = "value '%s' doesn't match any of %s";

	boolean isValid(String value, List<String> validTags, ConstraintValidatorContext context) {
		var valid = isBlank(value) || ofNullable(validTags).orElse(emptyList()).stream().anyMatch(value::equalsIgnoreCase);

		if (!valid) {
			useCustomMessageForValidation(value, validTags, context);
		}

		return valid;
	}

	private void useCustomMessageForValidation(String value, List<String> validTags, ConstraintValidatorContext constraintContext) {
		constraintContext.disableDefaultConstraintViolation();
		constraintContext.buildConstraintViolationWithTemplate(InterpolationHelper.escapeMessageParameter(String.format(CUSTOM_ERROR_MESSAGE_TEMPLATE, value, validTags))).addConstraintViolation();
	}
}
