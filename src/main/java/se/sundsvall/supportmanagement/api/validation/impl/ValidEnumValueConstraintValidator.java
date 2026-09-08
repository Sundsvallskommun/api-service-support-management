package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import se.sundsvall.supportmanagement.api.validation.ValidEnumValue;

import static java.util.Objects.isNull;

public class ValidEnumValueConstraintValidator implements ConstraintValidator<ValidEnumValue, String> {

	private List<String> allowed;

	@Override
	public void initialize(final ValidEnumValue annotation) {
		this.allowed = Arrays.stream(annotation.value().getEnumConstants())
			.map(Enum::name)
			.toList();
	}

	/**
	 * A null value is left to {@code @NotBlank} or {@code @NotNull} to refuse, since an optional field that was simply
	 * left out is not an invalid one.
	 */
	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (isNull(value) || allowed.contains(value)) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("must be one of %s".formatted(allowed)).addConstraintViolation();
		return false;
	}
}
