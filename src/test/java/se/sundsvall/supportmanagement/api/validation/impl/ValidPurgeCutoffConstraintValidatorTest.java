package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.time.Duration;
import java.time.Period;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.config.ErrandPurgeProperties;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidPurgeCutoffConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	private final ValidPurgeCutoffConstraintValidator validator = validator("P2Y");

	private static ValidPurgeCutoffConstraintValidator validator(final String minimumAge) {
		return new ValidPurgeCutoffConstraintValidator(new ErrandPurgeProperties(Period.parse(minimumAge), 250, Duration.ofHours(24), 2));
	}

	@Test
	void cutoffOlderThanMinimumAge() {
		assertThat(validator.isValid(now(systemDefault()).minusYears(3), constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}

	@Test
	void cutoffExactlyAtMinimumAge() {
		// A cutoff a moment beyond the floor passes, which is what keeps the boundary itself usable
		assertThat(validator.isValid(now(systemDefault()).minusYears(2).minusSeconds(1), constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}

	@Test
	void cutoffCloserThanMinimumAge() {
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid(now(systemDefault()).minusYears(1), constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("must be at least P2Y before the current time");
		verify(constraintViolationBuilderMock).addConstraintViolation();
	}

	@Test
	void cutoffInTheFuture() {
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(validator.isValid(now(systemDefault()).plusDays(1), constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("must be at least P2Y before the current time");
		verify(constraintViolationBuilderMock).addConstraintViolation();
	}

	@Test
	void configuredPeriodIsReflectedInTheMessage() {
		final var sixMonths = validator("P6M");
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilderMock);

		assertThat(sixMonths.isValid(now(systemDefault()).minusMonths(1), constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).buildConstraintViolationWithTemplate("must be at least P6M before the current time");
	}

	@Test
	void nullCutoff() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock, constraintViolationBuilderMock);
	}
}
