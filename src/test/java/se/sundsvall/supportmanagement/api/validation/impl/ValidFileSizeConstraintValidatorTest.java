package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidFileSizeConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Test
	void validFileSize() {
		final var validator = new ValidFileSizeConstraintValidator(10);

		final var base64Data = "dGVzdHN0cmluZw=="; // Translates to 'Teststring'

		assertThat(validator.isValid(base64Data, constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}

	@Test
	void fileSizeToLarge() {
		final var validator = new ValidFileSizeConstraintValidator(9);

		final var base64Data = "dGVzdHN0cmluZw=="; // Translates to 'Teststring'

		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid(base64Data, constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("attachment exceeds the maximum allowed size of 9 bytes");
		verify(constraintViolationBuilderMock).addConstraintViolation();
	}

	@Test
	void nullData() {
		final var validator = new ValidFileSizeConstraintValidator(10);

		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}

	@Test
	void notBase64Data() {
		final var validator = new ValidFileSizeConstraintValidator(10);
		final var base64Data = "åäö";

		assertThat(validator.isValid(base64Data, constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}
}
