package se.sundsvall.supportmanagement.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.supportmanagement.api.validation.impl.AbstractTagConstraintValidator.PATHVARIABLE_NAMESPACE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.CATEGORY;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EntityType.TYPE;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.metadata.Category;
import se.sundsvall.supportmanagement.api.model.metadata.Type;
import se.sundsvall.supportmanagement.service.MetadataService;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidClassificationConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private MetadataService metadataServiceMock = Mockito.mock();

	@Mock
	private RequestAttributes requestAttributesMock;

	@InjectMocks
	private ValidClassificationCreateConstraintValidator validatorCreate;
	@InjectMocks
	private ValidClassificationUpdateConstraintValidator validatorUpdate;

	public Stream<Arguments> validators() {
		return Stream.of(Arguments.of(validatorCreate), Arguments.of(validatorUpdate));
	}

	@BeforeAll
	void setupMockito() {
		MockitoAnnotations.openMocks(this);
	}

	@BeforeEach
	void resetMocksBetweenIterations() {
		Mockito.reset(metadataServiceMock);
		Mockito.reset(constraintValidatorContextMock, constraintViolationBuilderMock);
	}

	@ParameterizedTest
	@MethodSource("validators")
	void invalidCategory(ValidClassificationConstraintValidator validator) {

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

	@ParameterizedTest
	@MethodSource("validators")
	void validCategory(ValidClassificationConstraintValidator validator) {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryName = "CATEGORY-1";
		final var typeName = "TYPE-1";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);
			when(metadataServiceMock.isValidated(namespace, municipalityId, TYPE)).thenReturn(true);
			when(metadataServiceMock.findCategories(namespace, municipalityId)).thenReturn(List.of(Category.create().withName(categoryName)));
			when(metadataServiceMock.findTypes(namespace, municipalityId, categoryName)).thenReturn(List.of(Type.create().withName(typeName)));

			assertThat(validator.isValid(Classification.create().withCategory(categoryName).withType(typeName), constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock).findTypes(namespace, municipalityId, categoryName);
		}
	}

	@ParameterizedTest
	@MethodSource("validators")
	void validCategoryInvalidType(ValidClassificationConstraintValidator validator) {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var categoryName = "CATEGORY-1";
		final var typeName = "invalid-type";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);
			when(metadataServiceMock.isValidated(namespace, municipalityId, TYPE)).thenReturn(true);
			when(metadataServiceMock.findCategories(namespace, municipalityId)).thenReturn(List.of(Category.create().withName(categoryName)));
			when(metadataServiceMock.findTypes(namespace, municipalityId, categoryName)).thenReturn(List.of(Type.create().withName("TYPE-1")));
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

			assertThat(validator.isValid(Classification.create().withCategory(categoryName).withType(typeName), constraintValidatorContextMock)).isFalse();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock).findTypes(namespace, municipalityId, categoryName);
		}
	}

	@ParameterizedTest
	@MethodSource("validators")
	void nullValueInCategory(ValidClassificationConstraintValidator validator) {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

			assertThat(validator.isValid(Classification.create(), constraintValidatorContextMock)).isFalse();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock, never()).findTypes(any(), any(), any());
		}
	}

	@ParameterizedTest
	@MethodSource("validators")
	void nullableIfActiveTrue(ValidClassificationConstraintValidator validator) {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);
		validator.setNullableIfActive(true);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);

			assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock, never()).findTypes(any(), any(), any());
		}
	}

	@ParameterizedTest
	@MethodSource("validators")
	void nullValueInType(ValidClassificationConstraintValidator validator) {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);
		final var categoryName = "category-1";

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);
			when(metadataServiceMock.isValidated(namespace, municipalityId, TYPE)).thenReturn(true);
			when(metadataServiceMock.findCategories(namespace, municipalityId)).thenReturn(List.of(Category.create().withName(categoryName)));
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

			assertThat(validator.isValid(Classification.create().withCategory(categoryName), constraintValidatorContextMock)).isFalse();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
			verify(metadataServiceMock).findTypes(namespace, municipalityId, categoryName);
		}
	}

	@ParameterizedTest
	@MethodSource("validators")
	void blankString(ValidClassificationConstraintValidator validator) {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var attributes = Map.of(PATHVARIABLE_NAMESPACE, namespace, PATHVARIABLE_MUNICIPALITY_ID, municipalityId);

		try (MockedStatic<RequestContextHolder> requestContextHolderMock = Mockito.mockStatic(RequestContextHolder.class)) {
			requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributesMock);
			when(requestAttributesMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, SCOPE_REQUEST)).thenReturn(attributes);
			when(metadataServiceMock.isValidated(namespace, municipalityId, CATEGORY)).thenReturn(true);
			when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

			assertThat(validator.isValid(Classification.create().withCategory(" ").withType(" "), constraintValidatorContextMock)).isFalse();
			verify(metadataServiceMock).isValidated(namespace, municipalityId, CATEGORY);
			verify(metadataServiceMock).findCategories(namespace, municipalityId);
		}
	}
}
