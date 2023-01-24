package se.sundsvall.supportmanagement.api.validation.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.api.model.errand.Customer.create;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.EMPLOYEE;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.ENTERPRISE;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.PRIVATE;

@ExtendWith(MockitoExtension.class)
class ValidCustomerIdConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	private ValidCustomerIdConstraintValidator validator = new ValidCustomerIdConstraintValidator();

	@Test
	void withNull() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
	}

	@Test
	void withEmptyCustomer() {
		assertThat(validator.isValid(create(), constraintValidatorContextMock)).isTrue();
	}

	@Test
	void withValidEmployee() {
		assertThat(validator.isValid(create().withId("any-string").withType(EMPLOYEE), constraintValidatorContextMock)).isTrue();
	}

	@Test
	void withValidEnterprise() {
		assertThat(validator.isValid(create().withId(randomUUID().toString()).withType(ENTERPRISE), constraintValidatorContextMock)).isTrue();
	}

	@Test
	void withValidPrivate() {
		assertThat(validator.isValid(create().withId(randomUUID().toString()).withType(PRIVATE), constraintValidatorContextMock)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " " })
	@NullSource
	void withInvalidEmployee(String id) {
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid(create().withId(id).withType(EMPLOYEE), constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("id must contain at least one non blank character when customer type is EMPLOYEE");
		verify(constraintViolationBuilderMock).addConstraintViolation();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "non-valid-uuid" })
	@NullSource
	void withInvalidEnterprise(String id) {
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid(create().withId(id).withType(ENTERPRISE), constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("id must be a valid uuid when customer type is ENTERPRISE");
		verify(constraintViolationBuilderMock).addConstraintViolation();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "non-valid_uuid" })
	@NullSource
	void withInvalidPrivate(String id) {
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid(create().withId(id).withType(PRIVATE), constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("id must be a valid uuid when customer type is PRIVATE");
		verify(constraintViolationBuilderMock).addConstraintViolation();
	}
}
