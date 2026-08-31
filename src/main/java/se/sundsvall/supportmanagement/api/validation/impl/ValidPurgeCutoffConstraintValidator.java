package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;
import java.time.Period;
import org.springframework.beans.factory.annotation.Value;
import se.sundsvall.supportmanagement.api.validation.ValidPurgeCutoff;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Objects.isNull;

public class ValidPurgeCutoffConstraintValidator implements ConstraintValidator<ValidPurgeCutoff, OffsetDateTime> {

	private static final String CUSTOM_ERROR_MESSAGE_TEMPLATE = "must be at least %s before the current time";

	// The period is subtracted as a calendar amount rather than a fixed number of days, so that the floor lands on the
	// same date of the year regardless of leap years.
	@Value("${errand.purge.minimum-age:P2Y}")
	private Period minimumAge;

	@Override
	public boolean isValid(final OffsetDateTime cutoff, final ConstraintValidatorContext context) {
		if (isNull(cutoff)) {
			return true; // A missing cutoff is what @NotNull reports, and reporting it twice helps nobody
		}

		if (cutoff.isAfter(now(systemDefault()).minus(minimumAge))) {
			useCustomMessageForValidation(context, CUSTOM_ERROR_MESSAGE_TEMPLATE.formatted(minimumAge));
			return false;
		}

		return true;
	}

	private void useCustomMessageForValidation(final ConstraintValidatorContext constraintContext, final String message) {
		constraintContext.disableDefaultConstraintViolation();
		constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}
}
