package se.sundsvall.supportmanagement.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.api.model.errand.Suspension;

@ExtendWith(MockitoExtension.class)
class ValidSuspensionConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@InjectMocks
	private ValidSuspensionConstraintValidator validator;

	private static Stream<Arguments> argumentProvider() {
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
				false)
		);
	}

	@ParameterizedTest
	@MethodSource("argumentProvider")
	void validSuspension(final OffsetDateTime suspendFrom, final OffsetDateTime suspendTo, final boolean valid) {
		var suspension = new Suspension().withSuspendedFrom(suspendFrom).withSuspendedTo(suspendTo);

		assertThat(validator.isValid(suspension, constraintValidatorContextMock)).isEqualTo(valid);
	}

	@Test
	void nullSuspension() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
	}


}
