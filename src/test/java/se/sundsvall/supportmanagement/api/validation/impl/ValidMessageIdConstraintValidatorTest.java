package se.sundsvall.supportmanagement.api.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.IN_REPLY_TO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.MESSAGE_ID;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader.REFERENCES;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;

@ExtendWith(MockitoExtension.class)
class ValidMessageIdConstraintValidatorTest {

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private ConstraintValidatorContext mockContext;

	@InjectMocks
	private ValidMessageIdConstraintValidator validator;

	private static Stream<Arguments> validArgumentsProvider() {
		return Stream.of(
			Arguments.of(Map.of(MESSAGE_ID, List.of("<valid@sundsvall.se>"))),
			Arguments.of(Map.of(IN_REPLY_TO, List.of("<valid@sundsvall.se>"))),
			Arguments.of(Map.of(REFERENCES, List.of("<valid@sundsvall.se>", "<alsoValid@sundsvall.se>"))));
	}

	private static Stream<Arguments> invalidArgumentsProvider() {
		return Stream.of(
			Arguments.of(Map.of(MESSAGE_ID, List.of("missing@brackets"))),
			Arguments.of(Map.of(IN_REPLY_TO, List.of("<missingthe-snabel-A>"))),
			Arguments.of(Map.of(REFERENCES, List.of("inv<@>alid", "invalid"))));
	}

	@ParameterizedTest
	@MethodSource("validArgumentsProvider")
	void validMessageIds(final Map<EmailHeader, List<String>> headers) {
		for (final var header : headers.entrySet()) {
			for (final var value : header.getValue()) {
				assertThat(validator.isValid(value, mockContext)).isTrue();
			}
		}
	}

	@ParameterizedTest
	@MethodSource("invalidArgumentsProvider")
	void invalidMessageIds(final Map<EmailHeader, List<String>> headers) {
		for (final var header : headers.entrySet()) {
			for (final var value : header.getValue()) {
				assertThat(validator.isValid(value, mockContext)).isFalse();
			}
		}
	}

	@Test
	void isValidTest() {
		final String valid = "<Abc@abc.se>";
		final String missingBrackets = "abc@abc.se";
		final String blank = "";

		assertThat(validator.isValid(valid, mockContext)).isTrue();
		assertThat(validator.isValid(null, mockContext)).isFalse();
		assertThat(validator.isValid(missingBrackets, mockContext)).isFalse();
		assertThat(validator.isValid(blank, mockContext)).isFalse();
	}

}
