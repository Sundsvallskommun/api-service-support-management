package se.sundsvall.supportmanagement.integration.messagingsettings;

import generated.se.sundsvall.messagingsettings.MessagingSettingValue;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.service.model.MessagingSettings;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.service.mapper.MessagingSettingsMapper.toFilterString;

@Component
public class MessagingSettingsIntegration {
	private static final String KEY_CONTACT_INFORMATION_EMAIL = "contact_information_email";
	private static final String KEY_CONTACT_INFORMATION_EMAIL_NAME = "contact_information_email_name";
	private static final String KEY_CONTACT_INFORMATION_URL = "contact_information_url";
	private static final String KEY_SMS_SENDER = "sms_sender";
	private static final String KEY_SUPPORT_TEXT = "support_text";
	private static final String KEY_KATLA_URL = "katla_url";
	private static final String KEY_REPORTER_SUPPORT_TEXT = "reporter_support_text";
	private static final Logger LOG = LoggerFactory.getLogger(MessagingSettingsIntegration.class);

	private final MessagingSettingsClient messagingSettingsClient;

	MessagingSettingsIntegration(final MessagingSettingsClient messagingSettingsClient) {
		this.messagingSettingsClient = messagingSettingsClient;
	}

	public MessagingSettings getMessagingsettings(final String municipalityId, final String namespace, final String departmentName) {
		final var messagingSettings = messagingSettingsClient.getMessagingsettings(municipalityId, toFilterString(namespace, departmentName));

		if (messagingSettings.isEmpty()) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "No messagingsettings found for namespace '%s' and department with name '%s' within municipality with id '%s'".formatted(namespace, departmentName, municipalityId));
		}

		// Map received values to internal settings model class, throw exception if mandatory value is not found in response
		try {
			return new MessagingSettings(
				retrieveOptionalValue(KEY_SUPPORT_TEXT, messagingSettings).orElse(null), // This is not a mandatory setting
				retrieveOptionalValue(KEY_REPORTER_SUPPORT_TEXT, messagingSettings).orElse(null), // This is not a mandatory setting
				retrieveValue(KEY_CONTACT_INFORMATION_URL, messagingSettings),
				retrieveOptionalValue(KEY_KATLA_URL, messagingSettings).orElse("<Add katla_url configuration to namespace in messaging settings>"),
				retrieveValue(KEY_SMS_SENDER, messagingSettings),
				retrieveValue(KEY_CONTACT_INFORMATION_EMAIL, messagingSettings),
				retrieveOptionalValue(KEY_CONTACT_INFORMATION_EMAIL_NAME, messagingSettings).orElse(retrieveValue(KEY_CONTACT_INFORMATION_EMAIL, messagingSettings)));
		} catch (final ThrowableProblem e) {
			LOG.error("{} for namespace '{}' and department with name '{}' within municipality '{}'", e.getDetail(), sanitizeForLogging(namespace), sanitizeForLogging(departmentName), sanitizeForLogging(municipalityId));
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "One or more mandatory settings %s are absent for namespace '%s' and department with name '%s' within municipality with id '%s'"
				.formatted(List.of(KEY_CONTACT_INFORMATION_EMAIL, KEY_CONTACT_INFORMATION_URL, KEY_SMS_SENDER), namespace, departmentName, municipalityId));
		}
	}

	private String retrieveValue(String key, List<generated.se.sundsvall.messagingsettings.MessagingSettings> messagingSettings) {
		return retrieveOptionalValue(key, messagingSettings)
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "No setting matching key '%s' found".formatted(key)));
	}

	private Optional<String> retrieveOptionalValue(String key, List<generated.se.sundsvall.messagingsettings.MessagingSettings> messagingSettings) {
		return ofNullable(messagingSettings).orElse(emptyList()).stream()
			.map(generated.se.sundsvall.messagingsettings.MessagingSettings::getValues)
			.flatMap(List::stream)
			.filter(value -> Strings.CI.equals(key, value.getKey()))
			.map(MessagingSettingValue::getValue)
			.findFirst();
	}
}
