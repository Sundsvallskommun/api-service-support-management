package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import generated.se.sundsvall.emailreader.Email;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isAutoReply;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isDeliveryStatusReport;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isInvalidEmailAddress;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isNoReplyAddress;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.isSystemMessage;
import static se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderUtilities.shouldSuppressConfirmation;

class EmailReaderUtilitiesTest {

	@Test
	void isAutoReplyWithAutoSubmittedHeader() {
		final var email = new Email();
		email.setHeaders(Map.of("AUTO_SUBMITTED", List.of("auto-replied")));

		assertThat(isAutoReply(email)).isTrue();
	}

	@Test
	void isAutoReplyWithNoHeader() {
		final var email = new Email();
		email.setHeaders(Map.of("AUTO_SUBMITTED", List.of("No")));

		assertThat(isAutoReply(email)).isFalse();
	}

	@Test
	void isAutoReplyWithNullHeaders() {
		final var email = new Email();

		assertThat(isAutoReply(email)).isFalse();
	}

	@Test
	void isDeliveryStatusReportWithMatchingContentType() {
		final var email = new Email();
		email.setHeaders(Map.of("CONTENT_TYPE", List.of("multipart/report; report-type=delivery-status")));

		assertThat(isDeliveryStatusReport(email)).isTrue();
	}

	@Test
	void isDeliveryStatusReportWithNonMatchingContentType() {
		final var email = new Email();
		email.setHeaders(Map.of("CONTENT_TYPE", List.of("text/plain")));

		assertThat(isDeliveryStatusReport(email)).isFalse();
	}

	@Test
	void isSystemMessageWithEmptyReturnPath() {
		final var email = new Email();
		email.setHeaders(Map.of("RETURN_PATH", List.of("<>")));

		assertThat(isSystemMessage(email)).isTrue();
	}

	@Test
	void isSystemMessageWithNonEmptyReturnPath() {
		final var email = new Email();
		email.setHeaders(Map.of("RETURN_PATH", List.of("<user@domain.com>")));

		assertThat(isSystemMessage(email)).isFalse();
	}

	@Test
	void isSystemMessageWithNullHeaders() {
		final var email = new Email();

		assertThat(isSystemMessage(email)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"no-reply@domain.com", "noreply@domain.com", "NO-reply@otherdomain.com", "noREPLY@OTHERdomain.com"
	})
	void isNoReplyAddressWithNoReplyAddress(final String sender) {
		final var email = new Email();
		email.setSender(sender);

		assertThat(isNoReplyAddress(email)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"user@domain.com", "reply@domain.com", "support@noreply.com"
	})
	void isNoReplyAddressWithRegularAddress(final String sender) {
		final var email = new Email();
		email.setSender(sender);

		assertThat(isNoReplyAddress(email)).isFalse();
	}

	@Test
	void isNoReplyAddressWithNullSender() {
		final var email = new Email();

		assertThat(isNoReplyAddress(email)).isFalse();
	}

	@Test
	void isInvalidEmailAddressWithInvalidAddress() {
		final var email = new Email();
		email.setSender("invalid");

		assertThat(isInvalidEmailAddress(email)).isTrue();
	}

	@Test
	void isInvalidEmailAddressWithValidAddress() {
		final var email = new Email();
		email.setSender("user@domain.com");

		assertThat(isInvalidEmailAddress(email)).isFalse();
	}

	@Test
	void shouldSuppressConfirmationWithSystemMessage() {
		final var email = new Email();
		email.setSender("postmaster@domain.com");
		email.setHeaders(Map.of("RETURN_PATH", List.of("<>")));

		assertThat(shouldSuppressConfirmation(email)).isTrue();
	}

	@Test
	void shouldSuppressConfirmationWithInvalidEmail() {
		final var email = new Email();
		email.setSender("invalid");

		assertThat(shouldSuppressConfirmation(email)).isTrue();
	}

	@Test
	void shouldSuppressConfirmationWithAutoReply() {
		final var email = new Email();
		email.setSender("user@domain.com");
		email.setHeaders(Map.of("AUTO_SUBMITTED", List.of("auto-replied")));

		assertThat(shouldSuppressConfirmation(email)).isTrue();
	}

	@Test
	void shouldSuppressConfirmationNotForDeliveryStatusReport() {
		final var email = new Email();
		email.setSender("user@domain.com");
		email.setHeaders(Map.of(
			"AUTO_SUBMITTED", List.of("auto-replied"),
			"CONTENT_TYPE", List.of("multipart/report; report-type=delivery-status")));

		assertThat(shouldSuppressConfirmation(email)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"no-reply@domain.com", "noreply@domain.com", "NO-reply@otherdomain.com", "noREPLY@OTHERdomain.com"
	})
	void shouldSuppressConfirmationWithNoReplyAddress(final String sender) {
		final var email = new Email();
		email.setSender(sender);

		assertThat(shouldSuppressConfirmation(email)).isTrue();
	}

	@Test
	void shouldSuppressConfirmationWithValidRegularEmail() {
		final var email = new Email();
		email.setSender("user@domain.com");

		assertThat(shouldSuppressConfirmation(email)).isFalse();
	}
}
