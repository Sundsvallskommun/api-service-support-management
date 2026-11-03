package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ValidEmailRecipientsConstraintValidatorTest {

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@InjectMocks
	private ValidEmailRecipientsConstraintValidator validator;

	@ParameterizedTest
	@MethodSource("validEmailRecipientsArgumentProvider")
	void validEmailRecipients(final String recipient, final List<String> recipients, final boolean valid) {
		final var emailRequest = EmailRequest.create().withRecipient(recipient).withRecipients(recipients);

		assertThat(validator.isValid(emailRequest, constraintValidatorContextMock)).isEqualTo(valid);
	}

	@Test
	void nullEmailRequest() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
	}

	private static Stream<Arguments> validEmailRecipientsArgumentProvider() {
		return Stream.of(
			Arguments.of("recipient@example.com", null, true),
			Arguments.of(null, List.of("recipient@example.com"), true),
			Arguments.of("recipient@example.com", List.of("other@example.com"), true),
			Arguments.of(null, null, false),
			Arguments.of(null, List.of(), false),
			Arguments.of(" ", null, false),
			Arguments.of(" ", List.of(), false));
	}
}
