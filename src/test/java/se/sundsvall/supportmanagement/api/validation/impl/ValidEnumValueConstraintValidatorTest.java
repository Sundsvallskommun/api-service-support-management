package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.validation.ValidEnumValue;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidEnumValueConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext contextMock;

	@Mock
	private ConstraintViolationBuilder violationBuilderMock;

	private static ValidEnumValueConstraintValidator validator(final Class<? extends Enum<?>> type) {
		final var validator = new ValidEnumValueConstraintValidator();
		validator.initialize(new ValidEnumValue() {

			@Override
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return ValidEnumValue.class;
			}

			@Override
			public Class<? extends Enum<?>> value() {
				return type;
			}

			@Override
			public String message() {
				return "not a valid value";
			}

			@Override
			public Class<?>[] groups() {
				return new Class<?>[0];
			}

			@Override
			public Class<? extends jakarta.validation.Payload>[] payload() {
				return null;
			}
		});
		return validator;
	}

	@Test
	void aValueOfTheEnumIsAccepted() {
		assertThat(validator(ProcessStatus.class).isValid("RUNNING", contextMock)).isTrue();

		verifyNoInteractions(contextMock, violationBuilderMock);
	}

	/**
	 * Left to whatever else holds the field, since a field simply left out is not the same as one filled in wrongly - the
	 * severity of an entry is optional, the state of a process is not, and this validator answers the same to both.
	 */
	@Test
	void aMissingValueIsLeftToTheOtherConstraints() {
		assertThat(validator(ProcessStatus.class).isValid(null, contextMock)).isTrue();

		verifyNoInteractions(contextMock, violationBuilderMock);
	}

	@Test
	void aValueOutsideTheEnumIsRefusedAndNamesWhatIsAllowed() {
		when(contextMock.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilderMock);

		assertThat(validator(ProcessStatus.class).isValid("SOMETHING_ELSE", contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		verify(contextMock).buildConstraintViolationWithTemplate("must be one of [RUNNING, WAITING, RETRYING, COMPLETED, FAILED]");
		verify(violationBuilderMock).addConstraintViolation();
	}

	/**
	 * The names are compared as they are written, so a value in the wrong case is refused rather than quietly accepted -
	 * the value is stored as it arrives and would not match the column otherwise.
	 */
	@Test
	void aValueInTheWrongCaseIsRefused() {
		when(contextMock.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilderMock);

		assertThat(validator(ProcessStatus.class).isValid("running", contextMock)).isFalse();
	}
}
