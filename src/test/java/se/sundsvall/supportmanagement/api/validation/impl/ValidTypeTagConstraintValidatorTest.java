package se.sundsvall.supportmanagement.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;

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

import se.sundsvall.supportmanagement.service.TagService;

@ExtendWith(MockitoExtension.class)
class ValidTypeTagConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private TagService tagServiceMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidTypeTagConstraintValidator validator;

	@Test
	void invalidType() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

			assertThat(validator.isValid("type-1", constraintValidatorContextMock)).isFalse();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate(any());
			verify(constraintViolationBuilderMock).addConstraintViolation();
			verify(tagServiceMock).findAllTypeTags(namespace, municipalityId, "CATEGORY-1"); // TODO: Refactor when API is changed in UF-4537
		}
	}

	@Test
	void validType() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(tagServiceMock.findAllTypeTags(namespace, municipalityId, "CATEGORY-1")).thenReturn(List.of("TYPE-1")); // TODO: Refactor when API is changed in UF-4537

			assertThat(validator.isValid("type-1", constraintValidatorContextMock)).isTrue();
			verify(tagServiceMock).findAllTypeTags(namespace, municipalityId, "CATEGORY-1"); // TODO: Refactor when API is changed in UF-4537
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
			verify(tagServiceMock).findAllTypeTags(namespace, municipalityId, "CATEGORY-1"); // TODO: Refactor when API is changed in UF-4537
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
			verify(tagServiceMock).findAllTypeTags(namespace, municipalityId, "CATEGORY-1"); // TODO: Refactor when API is changed in UF-4537
		}
	}
}
