package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Objects;
import se.sundsvall.supportmanagement.api.model.metadata.Label;
import se.sundsvall.supportmanagement.api.validation.ValidLabelSiblings;

public class ValidLabelSiblingsConstraintValidator implements ConstraintValidator<ValidLabelSiblings, Collection<Label>> {

	@Override
	public boolean isValid(final Collection<Label> value, final ConstraintValidatorContext context) {
		return hasUniqueSiblingNames(value) && hasSameClassification(value);
	}

	/**
	 * Recursive method to validate that each level of labels contains no entries with same name
	 * 
	 * @param  value collection of labels to verify
	 * @return       true if collection (and its sub collections) only contains entries with unique names,
	 *               false otherwise
	 */
	private boolean hasUniqueSiblingNames(final Collection<Label> value) {
		final var childrenAreValid = ofNullable(value).orElse(emptyList())
			.stream()
			.map(Label::getLabels)
			.allMatch(this::hasUniqueSiblingNames);

		if (childrenAreValid) {
			Collection<String> names = ofNullable(value).orElse(emptyList())
				.stream()
				.filter(Objects::nonNull)
				.map(Label::getName)
				.filter(Objects::nonNull)
				.toList();

			return names.stream()
				.distinct()
				.count() == names.size();
		}

		return false;
	}

	/**
	 * Recursive method to validate that each level of labels contains entries with same classification
	 * 
	 * @param  value collection of labels to verify
	 * @return       true if collection (and its sub collections) only contains entries with the same classification,
	 *               false otherwise
	 */
	private boolean hasSameClassification(final Collection<Label> value) {
		final var childrenAreValid = ofNullable(value).orElse(emptyList())
			.stream()
			.map(Label::getLabels)
			.allMatch(this::hasSameClassification);

		if (childrenAreValid) {
			return ofNullable(value).orElse(emptyList())
				.stream()
				.filter(Objects::nonNull)
				.map(Label::getClassification)
				.distinct()
				.count() <= 1; // When list is empty (resulting in count 0) or having all entries with
								 // same classification (resulting in count 1) is considered to be valid
		}
		return false;
	}
}
