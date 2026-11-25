package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.metadata.ResolutionCode;
import se.sundsvall.supportmanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;
import static se.sundsvall.supportmanagement.model.enums.EntityType.RESOLUTION_CODE;

@ExtendWith(MockitoExtension.class)
class ValidResolutionConstraintValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final Map<String, String> ATTRIBUTES = Map.of(PATHVARIABLE_NAMESPACE, NAMESPACE, PATHVARIABLE_MUNICIPALITY_ID, MUNICIPALITY_ID);
	private static final List<ResolutionCode> CODES = List.of(
		ResolutionCode.create().withName("SOLVED"),
		ResolutionCode.create().withName("LEGACY").withDeprecated(true));

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private MetadataService metadataServiceMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidResolutionConstraintValidator validator;

	@Test
	void unregisteredResolution() {
		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(ATTRIBUTES);
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);
			when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);
			when(metadataServiceMock.findResolutionCodes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(Sort.class))).thenReturn(CODES);

			assertThat(validator.isValid("UNKNOWN", constraintValidatorContextMock)).isFalse();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("value 'UNKNOWN' doesn't match any of [SOLVED, LEGACY]");
			verify(constraintViolationBuilderMock).addConstraintViolation();
			verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
			verify(metadataServiceMock).findResolutionCodes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(Sort.class));
		}
	}

	/**
	 * Case is ignored, and a deprecated code is still registered and so still accepted.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"SOLVED", "solved", "LEGACY", "Legacy"
	})
	void registeredResolution(final String resolution) {
		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(ATTRIBUTES);
			when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);
			when(metadataServiceMock.findResolutionCodes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(Sort.class))).thenReturn(CODES);

			assertThat(validator.isValid(resolution, constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
			verify(metadataServiceMock).findResolutionCodes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(Sort.class));
		}
	}

	/**
	 * An absent or empty resolution is let through, since an empty value clears the resolution of the errand.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"", " "
	})
	void emptyResolution(final String resolution) {
		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(ATTRIBUTES);
			when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);

			assertThat(validator.isValid(resolution, constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
			verify(metadataServiceMock).findResolutionCodes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(Sort.class));
		}
	}

	@Test
	void nullResolution() {
		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(ATTRIBUTES);
			when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(true);

			assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
			verify(metadataServiceMock).findResolutionCodes(eq(NAMESPACE), eq(MUNICIPALITY_ID), any(Sort.class));
		}
	}

	@Test
	void notValidatedResolution() {
		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(ATTRIBUTES);
			when(metadataServiceMock.isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE)).thenReturn(false);

			assertThat(validator.isValid("UNKNOWN", constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(NAMESPACE, MUNICIPALITY_ID, RESOLUTION_CODE);
			verifyNoMoreInteractions(metadataServiceMock);
		}
	}

	@Test
	void noRequestPresent() {
		final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("SOLVED", constraintValidatorContextMock));
		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
	}
}
