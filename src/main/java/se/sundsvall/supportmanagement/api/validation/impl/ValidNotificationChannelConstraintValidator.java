package se.sundsvall.supportmanagement.api.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;
import se.sundsvall.dept44.common.validators.annotation.impl.ValidMSISDNConstraintValidator;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannel;
import se.sundsvall.supportmanagement.api.validation.ValidNotificationChannel;

import static se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType.SMS;

/**
 * Checks that the destination of an SMS channel, when given, is a number an sms can be sent to. Leaving it out is
 * valid, since the subscriber's own mobile number is then used. Other channel types are left to their own checks.
 */
public class ValidNotificationChannelConstraintValidator implements ConstraintValidator<ValidNotificationChannel, NotificationChannel> {

	private static final ValidMSISDNConstraintValidator MSISDN_VALIDATOR = new ValidMSISDNConstraintValidator();

	@Override
	public boolean isValid(final NotificationChannel channel, final ConstraintValidatorContext context) {
		return Optional.ofNullable(channel)
			.filter(value -> value.getType() == SMS)
			.map(NotificationChannel::getDestination)
			.map(MSISDN_VALIDATOR::isValid)
			.orElse(true);
	}
}
