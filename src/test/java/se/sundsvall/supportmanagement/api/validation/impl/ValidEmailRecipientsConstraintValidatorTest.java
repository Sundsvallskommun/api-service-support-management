package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidEmailRecipientsConstraintValidatorTest {

	private static final String MESSAGE_TEMPLATE = "at least one of recipient and recipients must be set";

	@Mock
	private ConstraintValidatorContext constraintValidatorContextMock;

	@Mock
	private ConstraintViolationBuilder constraintViolationBuilderMock;

	@Mock
	private NodeBuilderCustomizableContext nodeBuilderCustomizableContextMock;

	@InjectMocks
	private ValidEmailRecipientsConstraintValidator validator;

	@ParameterizedTest
	@MethodSource("validEmailRecipientsArgumentProvider")
	void validEmailRecipients(final String recipient, final List<String> recipients) {
		final var emailRequest = EmailRequest.create().withRecipient(recipient).withRecipients(recipients);

		assertThat(validator.isValid(emailRequest, constraintValidatorContextMock)).isTrue();

		verifyNoInteractions(constraintValidatorContextMock);
	}

	@ParameterizedTest
	@MethodSource("invalidEmailRecipientsArgumentProvider")
	void invalidEmailRecipients(final String recipient, final List<String> recipients) {
		final var emailRequest = EmailRequest.create().withRecipient(recipient).withRecipients(recipients);
		when(constraintValidatorContextMock.getDefaultConstraintMessageTemplate()).thenReturn(MESSAGE_TEMPLATE);
		when(constraintValidatorContextMock.buildConstraintViolationWithTemplate(MESSAGE_TEMPLATE)).thenReturn(constraintViolationBuilderMock);
		when(constraintViolationBuilderMock.addPropertyNode("recipients")).thenReturn(nodeBuilderCustomizableContextMock);

		assertThat(validator.isValid(emailRequest, constraintValidatorContextMock)).isFalse();

		verify(constraintValidatorContextMock).disableDefaultConstraintViolation();
		verify(nodeBuilderCustomizableContextMock).addConstraintViolation();
	}

	@Test
	void nullEmailRequest() {
		assertThat(validator.isValid(null, constraintValidatorContextMock)).isTrue();
	}

	private static Stream<Arguments> validEmailRecipientsArgumentProvider() {
		return Stream.of(
			Arguments.of("recipient@example.com", null),
			Arguments.of(null, List.of("recipient@example.com")),
			Arguments.of("recipient@example.com", List.of("other@example.com")),
			Arguments.of("", List.of("recipient@example.com")),
			Arguments.of(null, List.of("", "recipient@example.com")));
	}

	private static Stream<Arguments> invalidEmailRecipientsArgumentProvider() {
		return Stream.of(
			Arguments.of(null, null),
			Arguments.of(null, List.of()),
			Arguments.of(" ", null),
			Arguments.of(" ", List.of()),
			Arguments.of(null, List.of("")),
			Arguments.of("", List.of("", " ")));
	}
}
