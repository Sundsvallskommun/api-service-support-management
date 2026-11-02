package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;
import se.sundsvall.supportmanagement.api.validation.ValidEventFilter;

import static java.util.Objects.isNull;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.LABEL;

public class ValidEventFilterConstraintValidator implements ConstraintValidator<ValidEventFilter, EventFilter> {

	@Override
	public boolean isValid(final EventFilter eventFilter, final ConstraintValidatorContext context) {
		if (isNull(eventFilter) || isNull(eventFilter.getLabelId())) {
			return true;
		}
		return LABEL.getValue().equals(eventFilter.getSubtype());
	}
}
