package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.dept44.exception.ServerProblem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.validation.ValidJsonParameters;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;

import static java.util.Optional.ofNullable;
import static org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper.escapeMessageParameter;
import static org.springframework.util.CollectionUtils.isEmpty;

public class ValidJsonParametersConstraintValidator extends AbstractTagConstraintValidator implements ConstraintValidator<ValidJsonParameters, List<JsonParameter>> {

	private final JsonSchemaClient jsonSchemaClient;

	public ValidJsonParametersConstraintValidator(final JsonSchemaClient jsonSchemaClient) {
		this.jsonSchemaClient = jsonSchemaClient;
	}

	@Override
	public boolean isValid(final List<JsonParameter> value, final ConstraintValidatorContext context) {
		if (isEmpty(value)) {
			return true;
		}

		final var municipalityId = getPathVariable(PATHVARIABLE_MUNICIPALITY_ID);
		var hasErrors = false;

		final var seenKeys = new HashSet<>();
		final var duplicateKeys = new HashSet<>();

		// First pass: identify duplicate keys
		for (final var param : value) {
			if (!seenKeys.add(param.getKey())) {
				duplicateKeys.add(param.getKey());
			}
		}

		// Second pass: validate each parameter
		for (int i = 0; i < value.size(); i++) {
			final var param = value.get(i);

			if (duplicateKeys.contains(param.getKey())) {
				useCustomMessageForValidation("duplicate key '%s'".formatted(param.getKey()), i, context);
				hasErrors = true;
				continue;
			}

			try {
				jsonSchemaClient.validateJson(municipalityId, param.getSchemaId(), param.getValue());
			} catch (final ServerProblem e) {
				throw e;
			} catch (final ThrowableProblem e) {
				useCustomMessageForValidation(extractErrorMessage(e, param), i, context);
				hasErrors = true;
			}
		}

		return !hasErrors;
	}

	private String extractErrorMessage(final ThrowableProblem e, final JsonParameter param) {
		return ofNullable(e.getDetail())
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
