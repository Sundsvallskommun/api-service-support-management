package se.sundsvall.supportmanagement.api.validation.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

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
	
	@InjectMocks
	private ValidFileSizeConstraintValidator validator;

	@Test
	void validFileSize() {
		ReflectionTestUtils.setField(validator, "maximumByteSize", 10);

		final var base64Data = "dGVzdHN0cmluZw=="; //Translates to 'Teststring'
		
		assertThat(validator.isValid(base64Data, constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}

	@Test
	void fileSizeToLarge() {
		ReflectionTestUtils.setField(validator, "maximumByteSize", 9);

		final var base64Data = "dGVzdHN0cmluZw=="; //Translates to 'Teststring'
		
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid(base64Data, constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("attachment exceeds the maximum allowed size of 9 bytes");
		verify(constraintViolationBuilderMock).addConstraintViolation();
}
	
	@Test
	void nullData() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}

	@Test
	void notBase64Data() {
		final var base64Data = "åäö";
		
		assertThat(validator.isValid(base64Data, constraintValidatorContextMock)).isTrue();
		
		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}
}
