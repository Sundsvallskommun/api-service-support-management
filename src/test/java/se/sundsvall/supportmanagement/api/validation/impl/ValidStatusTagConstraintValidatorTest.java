package se.sundsvall.supportmanagement.api.validation.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.service.TagService;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidStatusTagConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private TagService tagServiceMock;

	@InjectMocks
	private ValidStatusTagConstraintValidator validator;

	@Test
	void invalidStatus() {
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid("status-1", constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate(any());
		verify(constraintViolationBuilderMock).addConstraintViolation();
	}

	@Test
	void validStatus() {
		when(tagServiceMock.findAllStatusTags()).thenReturn(List.of("STATUS-1"));
		assertThat(validator.isValid("status-1", constraintValidatorContextMock)).isTrue();
	}

	@Test
	void nullValue() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
	}

	@Test
	void blankString() {
		assertThat(validator.isValid(" ", constraintValidatorContextMock)).isTrue();
	}
}
