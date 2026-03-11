package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import generated.se.sundsvall.emailreader.Email;
import java.util.Optional;
import org.apache.commons.lang3.Strings;
import org.apache.commons.validator.routines.EmailValidator;

import static java.util.Collections.emptyList;

public final class EmailReaderUtilities {

	private EmailReaderUtilities() {
		// Utility class
	}

	static boolean isAutoReply(final Email email) {
		return Optional.ofNullable(email.getHeaders())
			.map(headers -> headers.getOrDefault("AUTO_SUBMITTED", emptyList()))
			.orElse(emptyList())
			.stream()
			.anyMatch(value -> !"No".equalsIgnoreCase(value));
	}

	static boolean isDeliveryStatusReport(final Email email) {
		return Optional.ofNullable(email.getHeaders())
			.map(headers -> headers.getOrDefault("CONTENT_TYPE", emptyList()))
			.orElse(emptyList())
			.stream()
			.anyMatch(value -> value.toLowerCase().contains("report-type=delivery-status"));
	}

	static boolean isSystemMessage(final Email email) {
		final var returnPathValues = Optional.ofNullable(email.getHeaders())
			.map(headers -> headers.getOrDefault("RETURN_PATH", emptyList()))
			.orElse(emptyList());

		return returnPathValues.stream()
			.anyMatch(value -> "<>".equals(value.strip()) || value.isBlank());
	}

	static boolean isNoReplyAddress(final Email email) {
		final var sender = email.getSender();
		return sender != null && Strings.CI.startsWithAny(sender, "no-reply", "noreply");
	}

	static boolean isInvalidEmailAddress(final Email email) {
		return !EmailValidator.getInstance().isValid(email.getSender());
	}

	static boolean shouldSuppressConfirmation(final Email email) {
		// Always suppress for system messages
		if (isSystemMessage(email)) {
			return true;
		}
		// Always suppress for invalid email addresses
		if (isInvalidEmailAddress(email)) {
			return true;
		}
		// Suppress for auto-replies (except delivery-status reports)
		if (isAutoReply(email) && !isDeliveryStatusReport(email)) {
			return true;
		}
		// Suppress for no-reply addresses
		return isNoReplyAddress(email);
	}
}
