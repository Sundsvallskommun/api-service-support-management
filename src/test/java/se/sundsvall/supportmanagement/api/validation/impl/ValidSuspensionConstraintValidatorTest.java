package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.errand.Suspension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ValidSuspensionConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@InjectMocks
	private ValidSuspensionConstraintValidator validator;

	@ParameterizedTest
	@MethodSource("validSuspensionArgumentProvider")
	void validSuspension(final OffsetDateTime suspendFrom, final OffsetDateTime suspendTo, final boolean valid) {
		final var suspension = new Suspension().withSuspendedFrom(suspendFrom).withSuspendedTo(suspendTo);

		assertThat(validator.isValid(suspension, constraintValidatorContextMock)).isEqualTo(valid);
	}

	@Test
	void nullSuspension() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
	}

	private static Stream<Arguments> validSuspensionArgumentProvider() {
		return Stream.of(
			Arguments.of(
				OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1)),
				OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.ofHours(1)),
				true),
			Arguments.of(
				OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.ofHours(1)),
				OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1)),
				false),
			Arguments.of(
				OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(1)),
				null,
				false),
			Arguments.of(
				null,
				null,
				true));
	}
}
