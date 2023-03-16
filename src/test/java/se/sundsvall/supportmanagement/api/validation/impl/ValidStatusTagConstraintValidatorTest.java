package se.sundsvall.supportmanagement.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.supportmanagement.service.TagService;

@ExtendWith(MockitoExtension.class)
class ValidStatusTagConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private TagService tagServiceMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidStatusTagConstraintValidator validator;

	@Test
	void invalidStatus() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

			assertThat(validator.isValid("status-1", constraintValidatorContextMock)).isFalse();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate(any());
			verify(constraintViolationBuilderMock).addConstraintViolation();
			verify(tagServiceMock).findAllStatusTags(namespace, municipalityId);
		}
	}

	@Test
	void validStatus() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(tagServiceMock.findAllStatusTags(namespace, municipalityId)).thenReturn(List.of("STATUS-1"));

			assertThat(validator.isValid("status-1", constraintValidatorContextMock)).isTrue();
			verify(tagServiceMock).findAllStatusTags(namespace, municipalityId);
		}
	}

	@Test
	void nullValue() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
			verify(tagServiceMock).findAllStatusTags(namespace, municipalityId);
		}
	}

	@Test
	void blankString() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			assertThat(validator.isValid(" ", constraintValidatorContextMock)).isTrue();
			verify(tagServiceMock).findAllStatusTags(namespace, municipalityId);
		}
	}

	@Test
	void noRequestPresent() {
		final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("status-1", constraintValidatorContextMock));
		assertThat(e.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
		assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
	}

	@Test
	void noRequestAttributesMapPresent() {
		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);

			final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("status-1", constraintValidatorContextMock));
			assertThat(e.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
			assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
		}
	}

	@Test
	void attributePresentAsNonMap() {

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(Long.MAX_VALUE);

			final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("status-1", constraintValidatorContextMock));
			assertThat(e.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
			assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
		}
	}

	@Test
	void municipalityIdAttributePresentAsNonString() {
		final var namespace = "namespace";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, Long.MAX_VALUE);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("status-1", constraintValidatorContextMock));
			assertThat(e.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
			assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'municipalityId' is not readable from request");
		}
	}

	@Test
	void namespaceAttributePresentAsNonString() {
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, Long.MAX_VALUE, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("status-1", constraintValidatorContextMock));
			assertThat(e.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
			assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
		}
	}

	@Test
	void attributePresentAsNull() {
		final var attributes = new HashMap<>();
		attributes.put(PATHVARIABLE_NAMESPACE, null);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);

			final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("status-1", constraintValidatorContextMock));
			assertThat(e.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
			assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
		}
	}
}
