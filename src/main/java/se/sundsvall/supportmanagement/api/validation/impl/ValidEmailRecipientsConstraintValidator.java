package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.validation.ValidEmailRecipients;

import static java.util.Objects.isNull;

public class ValidEmailRecipientsConstraintValidator implements ConstraintValidator<ValidEmailRecipients, EmailRequest> {

	@Override
	public boolean isValid(final EmailRequest emailRequest, final ConstraintValidatorContext context) {
		if (isNull(emailRequest) || !emailRequest.allRecipients().isEmpty()) {
			return true;
		}

		// Reported on recipients rather than on the request itself, so that a client can tie it to a field
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
			.addPropertyNode("recipients")
			.addConstraintViolation();
		return false;
	}
}
