package se.sundsvall.supportmanagement.api.validation.impl;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannel;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType.EMAIL;
import static se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType.INTERNAL;
import static se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType.SMS;

class ValidNotificationChannelConstraintValidatorTest {

	private final ValidNotificationChannelConstraintValidator validator = new ValidNotificationChannelConstraintValidator();

	private static Stream<Arguments> channelArgumentProvider() {
		return Stream.of(
			Arguments.of(SMS, "+46701234567", true),
			Arguments.of(SMS, null, true),
			Arguments.of(SMS, "070-1234567", false),
			Arguments.of(SMS, "0701234567", false),
			Arguments.of(SMS, "not-a-number", false),
			Arguments.of(EMAIL, "user@example.com", true),
			Arguments.of(INTERNAL, null, true),
			Arguments.of(null, "not-a-number", true));
	}

	@ParameterizedTest
	@MethodSource("channelArgumentProvider")
	void isValid(final NotificationChannelType type, final String destination, final boolean expected) {
		final var channel = NotificationChannel.create().withType(type).withDestination(destination);

		assertThat(validator.isValid(channel, null)).isEqualTo(expected);
	}

	@Test
	void nullIsValid() {
		assertThat(validator.isValid(null, null)).isTrue();
	}
}
