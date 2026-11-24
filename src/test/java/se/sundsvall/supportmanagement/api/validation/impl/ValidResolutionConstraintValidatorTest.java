package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.service.ResolutionValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;
import static se.sundsvall.supportmanagement.api.validation.impl.ValidResolutionConstraintValidator.PATHVARIABLE_ERRAND_ID;

@ExtendWith(MockitoExtension.class)
class ValidResolutionConstraintValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ResolutionValidator resolutionValidatorMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidResolutionConstraintValidator validator;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(resolutionValidatorMock);
		verifyNoInteractions(constraintValidatorContextMock);
	}

	@ParameterizedTest
	@ValueSource(booleans = {
		true, false
	})
	void passesTheErrandOfThePath(final boolean valid) {
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, NAMESPACE, PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID, PATHVARIABLE_ERRAND_ID, ERRAND_ID);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(resolutionValidatorMock.isValid("SOLVED", NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(valid);

			assertThat(validator.isValid("SOLVED", constraintValidatorContextMock)).isEqualTo(valid);

			verify(resolutionValidatorMock).isValid("SOLVED", NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
		}
	}

	@Test
	void passesNoErrandWhenThePathHasNone() {
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, NAMESPACE, PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(resolutionValidatorMock.isValid("SOLVED", NAMESPACE, MUNICIPALITY_ID, null)).thenReturn(true);

			assertThat(validator.isValid("SOLVED", constraintValidatorContextMock)).isTrue();

			verify(resolutionValidatorMock).isValid("SOLVED", NAMESPACE, MUNICIPALITY_ID, null);
		}
	}

	@Test
	void noRequestPresent() {
		assertThatThrownBy(() -> validator.isValid("SOLVED", constraintValidatorContextMock))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessage("Internal Server Error: Path variable 'namespace' is not readable from request")
			.extracting(e -> ((ThrowableProblem) e).getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
	}
}
