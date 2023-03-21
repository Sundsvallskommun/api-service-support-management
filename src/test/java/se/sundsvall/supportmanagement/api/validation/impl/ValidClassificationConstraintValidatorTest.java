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
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.MetadataService;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.CATEGORY;

@ExtendWith(MockitoExtension.class)
class ValidClassificationConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private MetadataService metadataServiceMock;

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidClassificationConstraintValidator validator;

	@Test
	void invalidCategory() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);

			assertThat(validator.isValid(Classification.create().withCategory("category-1").withType("type-1"), constraintValidatorContextMock)).isFalse();
			verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate(any());
			verify(constraintViolationBuilderMock).addConstraintViolation();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
		}
	}

	@Test
	void validCategory() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryName = "CATEGORY-1";
		final var typeName = "TYPE-1";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);
			when(metadataServiceMock.findCategories(namespace, municipalityId)).thenReturn(List.of(Category.create().withName(categoryName)));
			when(metadataServiceMock.findTypes(namespace, municipalityId, categoryName)).thenReturn(List.of(Type.create().withName(typeName)));

			assertThat(validator.isValid(Classification.create().withCategory(categoryName).withType(typeName), constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock).findTypes(namespace, municipalityId, categoryName);
		}
	}

	@Test
	void classificationNullValue() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);

			assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
		}
	}

	@Test
	void nullValueInCategoryAndType() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);

			assertThat(validator.isValid(Classification.create(), constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock).findTypes(namespace, municipalityId, null);
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
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);

			assertThat(validator.isValid(Classification.create().withCategory(" ").withType(" "), constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock).findTypes(namespace, municipalityId, " ");
		}
	}
}
