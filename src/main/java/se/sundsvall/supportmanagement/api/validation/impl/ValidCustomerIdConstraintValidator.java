package se.sundsvall.supportmanagement.api.validation.impl;

import se.sundsvall.dept44.common.validators.annotation.impl.ValidUuidConstraintValidator;
import se.sundsvall.supportmanagement.api.model.errand.Customer;
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.api.validation.ValidCustomerId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.EMPLOYEE;

public class ValidCustomerIdConstraintValidator implements ConstraintValidator<ValidCustomerId, Customer> {
	private static final String CUSTOM_ERROR_UUID_MESSAGE_TEMPLATE = "id must be a valid uuid when customer type is %s";
	private static final String CUSTOM_ERROR_USERID_MESSAGE_TEMPLATE = "id must contain at least one non blank character when customer type is %s";
	private static final ValidUuidConstraintValidator UUID_VALIDATOR = new ValidUuidConstraintValidator();

	@Override
	public boolean isValid(final Customer value, final ConstraintValidatorContext context) {
		if (isNull(value) || isNull(value.getType())) {
			return true;
		}

		final var valid = switch (value.getType()) {
			case ENTERPRISE -> UUID_VALIDATOR.isValid(value.getId());
			case PRIVATE -> UUID_VALIDATOR.isValid(value.getId());
			case EMPLOYEE -> isNotBlank(value.getId()); // No format validation on employee id
		};

		if (!valid) {
			useCustomMessageForValidation(context, value.getType());
		}

		return valid;
	}

	private void useCustomMessageForValidation(ConstraintValidatorContext constraintContext, CustomerType customerType) {
		final var errorTemplate = customerType.equals(EMPLOYEE) ? CUSTOM_ERROR_USERID_MESSAGE_TEMPLATE : CUSTOM_ERROR_UUID_MESSAGE_TEMPLATE;
		constraintContext.disableDefaultConstraintViolation();
		constraintContext.buildConstraintViolationWithTemplate(String.format(errorTemplate, customerType.name())).addConstraintViolation();
	}
}
