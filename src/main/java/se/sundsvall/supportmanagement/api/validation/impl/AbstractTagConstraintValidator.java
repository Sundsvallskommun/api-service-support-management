package se.sundsvall.supportmanagement.api.validation.impl;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;
import org.springframework.web.context.request.RequestContextHolder;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

abstract class AbstractTagConstraintValidator {
	private static final String CUSTOM_ERROR_MESSAGE_TEMPLATE = "value '%s' doesn't match any of %s";

	static final String PATHVARIABLE_NAMESPACE = "namespace";
	static final String PATHVARIABLE_MUNICIPALITY_ID = "municipalityId";

	/**
	 * Getting value for path variable name from current request
	 * 
	 * @param variableName path parameter to receive value for
	 * @return value of path parameter that matches sent in variable name
	 */
	String getPathVariable(String variableName) {
		return Stream.ofNullable(RequestContextHolder.getRequestAttributes())
			.map(req -> req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST))
			.filter(Objects::nonNull)
			.filter(Map.class::isInstance)
			.map(Map.class::cast)
			.map(map -> map.get(variableName))
			.filter(Objects::nonNull)
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.findAny()
			.orElseThrow(() -> Problem.valueOf(Status.INTERNAL_SERVER_ERROR, String.format("Path variable '%s' is not readable from request", variableName)));
	}

	boolean isValid(String value, List<String> validTags, ConstraintValidatorContext context) {
		var valid = isBlank(value) || ofNullable(validTags).orElse(emptyList()).stream().anyMatch(value::equalsIgnoreCase);

		if (!valid) {
			useCustomMessageForValidation(value, validTags, context);
		}

		return valid;
	}

	private void useCustomMessageForValidation(String value, List<String> validTags, ConstraintValidatorContext constraintContext) {
		constraintContext.disableDefaultConstraintViolation();
		constraintContext.buildConstraintViolationWithTemplate(InterpolationHelper.escapeMessageParameter(String.format(CUSTOM_ERROR_MESSAGE_TEMPLATE, value, validTags))).addConstraintViolation();
	}
}
