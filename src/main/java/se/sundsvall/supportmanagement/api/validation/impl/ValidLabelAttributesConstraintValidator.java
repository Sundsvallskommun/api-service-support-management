package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.model.metadata.LabelAttribute;
import se.sundsvall.supportmanagement.api.validation.ValidLabelAttributes;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class ValidLabelAttributesConstraintValidator implements ConstraintValidator<ValidLabelAttributes, Collection<Label>> {

	@Override
	public boolean isValid(final Collection<Label> value, final ConstraintValidatorContext context) {
		return hasUniqueAttributeKeysRecursively(value);
	}

	/**
	 * Recursive method to validate that every label in the tree has unique attribute keys among its own attributes.
	 *
	 * @param  value collection of labels to verify
	 * @return       true if every label (and its descendants) carries unique attribute keys, false otherwise
	 */
	private boolean hasUniqueAttributeKeysRecursively(final Collection<Label> value) {
		return ofNullable(value).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.allMatch(label -> hasUniqueAttributeKeys(label.getAttributes())
				&& hasUniqueAttributeKeysRecursively(label.getLabels()));
	}

	private boolean hasUniqueAttributeKeys(final Collection<LabelAttribute> attributes) {
		final var keys = ofNullable(attributes).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(LabelAttribute::getKey)
			.filter(Objects::nonNull)
			.toList();

		return keys.stream().distinct().count() == keys.size();
	}
}
