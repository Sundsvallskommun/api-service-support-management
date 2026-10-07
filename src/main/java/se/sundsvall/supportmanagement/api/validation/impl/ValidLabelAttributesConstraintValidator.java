package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;
import se.sundsvall.supportmanagement.api.validation.ValidLabelAttributes;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class ValidLabelAttributesConstraintValidator implements ConstraintValidator<ValidLabelAttributes, Collection<LabelAttribute>> {

	@Override
	public boolean isValid(final Collection<LabelAttribute> value, final ConstraintValidatorContext context) {
		final var keys = ofNullable(value).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(LabelAttribute::getKey)
			.filter(Objects::nonNull)
			.toList();

		return keys.stream().distinct().count() == keys.size();
	}
}
