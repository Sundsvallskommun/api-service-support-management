package se.sundsvall.supportmanagement.api.validation.impl;

import org.apache.commons.lang3.StringUtils;

import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.validation.UniqueExternalTagKeys;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class UniqueExternalTagKeysConstraintValidator implements ConstraintValidator<UniqueExternalTagKeys, Collection<ExternalTag>> {

	@Override
	public boolean isValid(final Collection<ExternalTag> value, final ConstraintValidatorContext context) {
		Collection<String> nonNullKeys = ofNullable(value).orElse(emptyList())
			.stream()
			.filter(Objects::nonNull)
			.map(ExternalTag::getKey)
			.filter(StringUtils::isNotBlank)
			.map(String::trim)
			.map(String::toLowerCase)
			.toList();

		return nonNullKeys.stream()
			.distinct()
			.count() == nonNullKeys.size();
	}
}
