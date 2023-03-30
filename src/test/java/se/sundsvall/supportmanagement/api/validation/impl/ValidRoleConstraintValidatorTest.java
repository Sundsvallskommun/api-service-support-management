package se.sundsvall.supportmanagement.api.validation.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.metadata.Role;
import se.sundsvall.supportmanagement.service.MetadataService;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.ROLE;

@ExtendWith(MockitoExtension.class)
class ValidRoleConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private MetadataService metadataServiceMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidRoleConstraintValidator validator;

	@Test
	void invalidRole() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);
			when(metadataServiceMock.isValidated(namespace, municipalityId, ROLE)).thenReturn(true);

			assertThat(validator.isValid("invalid-role", constraintValidatorContextMock)).isFalse();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate(any());
			verify(constraintViolationBuilderMock).addConstraintViolation();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, ROLE);
			verify(metadataServiceMock).findRoles(namespace, municipalityId);
		}
	}

	@Test
	void validRole() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.findRoles(namespace, municipalityId)).thenReturn(List.of(Role.create().withName("ROLE-1")));
			when(metadataServiceMock.isValidated(namespace, municipalityId, ROLE)).thenReturn(true);

			assertThat(validator.isValid("role-1", constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, ROLE);
			verify(metadataServiceMock).findRoles(namespace, municipalityId);
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
			when(metadataServiceMock.isValidated(namespace, municipalityId, ROLE)).thenReturn(true);

			assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, ROLE);
			verify(metadataServiceMock).findRoles(namespace, municipalityId);
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
			when(metadataServiceMock.isValidated(namespace, municipalityId, ROLE)).thenReturn(true);

			assertThat(validator.isValid(" ", constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, ROLE);
			verify(metadataServiceMock).findRoles(namespace, municipalityId);
		}
	}

	@Test
	void notValidatedRole() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, ROLE)).thenReturn(false);


			assertThat(validator.isValid("role-1", constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, ROLE);
			verifyNoMoreInteractions(metadataServiceMock);
		}
	}
	@Test
	void noRequestPresent() {
		final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("role-1", constraintValidatorContextMock));
		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
	}

	@Test
	void noRequestAttributesMapPresent() {
		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);

			final var e = assertThrows(ThrowableProblem.class, () -> validator.isValid("role-1", constraintValidatorContextMock));
			assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
			assertThat(e.getMessage()).isEqualTo("Internal Server Error: Path variable 'namespace' is not readable from request");
		}
	}
}
