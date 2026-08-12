package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.validation.ValidJsonParameter;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper.escapeMessageParameter;

public class ValidJsonParameterConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidJsonParameter, JsonParameter> {

	private final JsonSchemaClient jsonSchemaClient;

	public ValidJsonParameterConstraintValidator(final JsonSchemaClient jsonSchemaClient) {
		this.jsonSchemaClient = jsonSchemaClient;
	}

	@Override
	public boolean isValid(final JsonParameter value, final ConstraintValidatorContext context) {
		if (value == null || isBlank(value.getSchemaId()) || value.getValue() == null) {
			return true;
		}

		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);

		try {
			jsonSchemaClient.validateJson(municipalityId, value.getSchemaId(), value.getValue());
			return true;
		} catch (final ServerProblem e) {
			throw e;
		} catch (final ThrowableProblem e) {
			useCustomMessageForValidation(extractErrorMessage(e, value), context);
			return false;
		}
	}

	private String extractErrorMessage(final ThrowableProblem e, final JsonParameter param) {
		final var fallback = "validation failed for schema '%s'".formatted(param.getSchemaId());
		final var detail = e.getDetail();
		return (detail != null && detail.isBlank()) ? fallback : ofNullable(e.getMessage()).orElse(fallback);
	}

	private void useCustomMessageForValidation(final String message, final ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(escapeMessageParameter(message))
			.addConstraintViolation();
	}
}
