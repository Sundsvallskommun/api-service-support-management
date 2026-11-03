package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.validation.ValidEmailRecipients;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

public class ValidEmailRecipientsConstraintValidator implements ConstraintValidator<ValidEmailRecipients, EmailRequest> {

	@Override
	public boolean isValid(final EmailRequest emailRequest, final ConstraintValidatorContext context) {
		if (isNull(emailRequest)) {
			return true;
		}

		return isNotBlank(emailRequest.getRecipient()) || !isEmpty(emailRequest.getRecipients());
	}
}
