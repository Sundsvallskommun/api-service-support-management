package se.sundsvall.supportmanagement.api.validation.impl;

import static org.apache.commons.lang3.ObjectUtils.anyNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import se.sundsvall.supportmanagement.api.model.errand.Suspend;
import se.sundsvall.supportmanagement.api.validation.ValidSuspend;

public class ValidSuspendConstraintValidator implements ConstraintValidator<ValidSuspend, Suspend> {

	@Override
	public boolean isValid(final Suspend suspend, final ConstraintValidatorContext context) {
		if (suspend == null) {
			return true;
		}

		if (anyNull(suspend.getSuspendedFrom(), suspend.getSuspendedTo())) {
			return false;
		}

		return suspend.getSuspendedFrom().isBefore(suspend.getSuspendedTo());
	}
}
