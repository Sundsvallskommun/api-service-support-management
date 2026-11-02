package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map.Entry;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.validation.ValidJsonParameters;
import se.sundsvall.supportmanagement.service.JsonSchemaValidationService;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper.escapeMessageParameter;
import static org.springframework.util.CollectionUtils.isEmpty;

public class ValidJsonParametersConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidJsonParameters, List<JsonParameter>> {

	private final JsonSchemaValidationService jsonSchemaValidationService;

	public ValidJsonParametersConstraintValidator(final JsonSchemaValidationService jsonSchemaValidationService) {
		this.jsonSchemaValidationService = jsonSchemaValidationService;
	}

	@Override
	public boolean isValid(final List<JsonParameter> value, final ConstraintValidatorContext context) {
		if (isEmpty(value)) {
			return true;
		}

		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);
		var hasErrors = false;

		final var duplicateKeys = value.stream()
			.collect(toMap(JsonParameter::getKey, param -> 1, Integer::sum))
			.entrySet().stream()
			.filter(keyCount -> keyCount.getValue() > 1)
			.map(Entry::getKey)
			.collect(toSet());

		for (int i = 0; i < value.size(); i++) {
			final var param = value.get(i);

			if (duplicateKeys.contains(param.getKey())) {
				useCustomMessageForValidation("duplicate key '%s'".formatted(param.getKey()), i, context);
				hasErrors = true;
				continue;
			}

			final var violation = jsonSchemaValidationService.findViolation(municipalityId, param.getSchemaId(), param.getValue());
			if (violation.isPresent()) {
				useCustomMessageForValidation(extractErrorMessage(violation.get(), param), i, context);
				hasErrors = true;
			}
		}

		return !hasErrors;
	}

	private String extractErrorMessage(final ThrowableProblem e, final JsonParameter param) {
		return ofNullable(e.getMessage())
			.filter(detail -> !detail.isBlank())
			.orElse("validation failed for schema '%s'".formatted(param.getSchemaId()));
	}

	private void useCustomMessageForValidation(final String message, final int index, final ConstraintValidatorContext constraintContext) {
		constraintContext.disableDefaultConstraintViolation();
		constraintContext.buildConstraintViolationWithTemplate(escapeMessageParameter(message))
			.addBeanNode()
			.inIterable()
			.atIndex(index)
			.addConstraintViolation();
	}
}
