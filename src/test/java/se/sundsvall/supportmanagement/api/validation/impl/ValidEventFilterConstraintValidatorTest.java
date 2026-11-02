package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.subscriber.EventFilter;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ValidEventFilterConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@InjectMocks
	private ValidEventFilterConstraintValidator validator;

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
		// Without a labelId any subtype goes
		"ERRAND, null, true",
		"null, null, true",
		// A labelId only goes with LABEL
		"LABEL, f2b7c5d1-7e3a-4b8e-9f0a-1c2d3e4f5a6b, true",
		"ERRAND, f2b7c5d1-7e3a-4b8e-9f0a-1c2d3e4f5a6b, false",
		"null, f2b7c5d1-7e3a-4b8e-9f0a-1c2d3e4f5a6b, false"
	})
	void validEventFilter(final String subtype, final String labelId, final boolean valid) {
		final var eventFilter = EventFilter.create().withType("UPDATE").withSubtype(subtype).withLabelId(labelId);

		assertThat(validator.isValid(eventFilter, constraintValidatorContextMock)).isEqualTo(valid);
	}

	@Test
	void nullEventFilter() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
	}
}
