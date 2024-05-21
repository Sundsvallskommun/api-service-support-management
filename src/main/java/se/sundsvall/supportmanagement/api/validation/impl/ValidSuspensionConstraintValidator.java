package se.sundsvall.supportmanagement.api.validation.impl;

import static org.apache.commons.lang3.ObjectUtils.anyNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import se.sundsvall.supportmanagement.api.model.errand.Suspension;
import se.sundsvall.supportmanagement.api.validation.ValidSuspension;

public class ValidSuspensionConstraintValidator implements ConstraintValidator<ValidSuspension, Suspension> {

	@Override
	public boolean isValid(final Suspension suspension, final ConstraintValidatorContext context) {
		if (suspension == null) {
			return true;
		}

		if (anyNull(suspension.getSuspendedFrom(), suspension.getSuspendedTo())) {
			return false;
		}

		return suspension.getSuspendedFrom().isBefore(suspension.getSuspendedTo());
	}
}
