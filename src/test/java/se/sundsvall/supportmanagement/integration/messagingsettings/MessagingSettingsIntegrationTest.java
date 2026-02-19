package se.sundsvall.supportmanagement.integration.messagingsettings;

import generated.se.sundsvall.messagingsettings.MessagingSettingValue;
import generated.se.sundsvall.messagingsettings.MessagingSettings;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class MessagingSettingsIntegrationTest {
	private static final String FILTER_STRING = "exists(values.key: 'namespace' and values.value: '%s') and exists(values.key: 'department_name' and values.value: '%s')";
	private static final String MUNICIPALITY_ID = "my-municipality";
	private static final String NAMESPACE = "my-namespace";
	private static final String DEPARTMENT_NAME = "my-department";
	private static final String KEY_EMAIL = "contact_information_email";
	private static final String KEY_EMAIL_NAME = "contact_information_email_name";
	private static final String KEY_URL = "contact_information_url";
	private static final String KEY_SMS_SENDER = "sms_sender";
	private static final String KEY_SUPPORT_TEXT = "support_text";
	private static final String KEY_REPORTER_SUPPORT_TEXT = "reporter_support_text";
	private static final String KEY_KATLA_URL = "katla_url";

	@Mock
	private MessagingSettingsClient clientMock;

	@InjectMocks
	private MessagingSettingsIntegration integration;

	@Captor
	private ArgumentCaptor<String> filterCaptor;

	@Test
	void getMessagingsettingsWithOptionalValuesSet() {
		final var emailValue = "email";
		final var emailNameValue = "emailName";
		final var urlValue = "url";
		final var smsSenderValue = "smsSender";
		final var supportTextValue = "supportText";
		final var reporterSupportTextValue = "reporterSupportText";
		final var katlaUrlValue = "katlaUrl";

		when(clientMock.getMessagingsettings(eq(MUNICIPALITY_ID), anyString())).thenReturn(List.of(new MessagingSettings().values(List.of(
			new MessagingSettingValue().key(KEY_EMAIL).value(emailValue),
			new MessagingSettingValue().key(KEY_EMAIL_NAME).value(emailNameValue),
			new MessagingSettingValue().key(KEY_KATLA_URL).value(katlaUrlValue),
			new MessagingSettingValue().key(KEY_REPORTER_SUPPORT_TEXT).value(reporterSupportTextValue),
			new MessagingSettingValue().key(KEY_URL).value(urlValue),
			new MessagingSettingValue().key(KEY_SMS_SENDER).value(smsSenderValue),
			new MessagingSettingValue().key(KEY_SUPPORT_TEXT).value(supportTextValue)))));

		final var result = integration.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME);

		assertThat(result.contactInformationEmail()).isEqualTo(emailValue);
		assertThat(result.contactInformationEmailName()).isEqualTo(emailNameValue);
		assertThat(result.contactInformationUrl()).isEqualTo(urlValue);
		assertThat(result.smsSender()).isEqualTo(smsSenderValue);
		assertThat(result.supportText()).isEqualTo(supportTextValue);
		assertThat(result.katlaUrl()).isEqualTo(katlaUrlValue);
		assertThat(result.reporterSupportText()).isEqualTo(reporterSupportTextValue);
	}

	@Test
	void getMessagingsettingsWithoutOptionalValuesSet() {
		final var emailValue = "email";
		final var urlValue = "url";
		final var smsSenderValue = "smsSender";

		when(clientMock.getMessagingsettings(eq(MUNICIPALITY_ID), anyString())).thenReturn(List.of(new MessagingSettings().values(List.of(
			new MessagingSettingValue().key(KEY_EMAIL).value(emailValue),
			new MessagingSettingValue().key(KEY_URL).value(urlValue),
			new MessagingSettingValue().key(KEY_SMS_SENDER).value(smsSenderValue)))));

		final var result = integration.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME);

		assertThat(result.contactInformationEmail()).isEqualTo(emailValue);
		assertThat(result.contactInformationEmailName()).isEqualTo(emailValue);
		assertThat(result.contactInformationUrl()).isEqualTo(urlValue);
		assertThat(result.smsSender()).isEqualTo(smsSenderValue);
		assertThat(result.supportText()).isBlank();
	}

	@Test
	void getMessagingsettingsWhenAbsent() {
		final var e = assertThrows(ThrowableProblem.class, () -> integration.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME));

		verify(clientMock).getMessagingsettings(eq(MUNICIPALITY_ID), filterCaptor.capture());

		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getDetail()).isEqualTo("No messagingsettings found for namespace '%s' and department with name '%s' within municipality with id '%s'".formatted(NAMESPACE, DEPARTMENT_NAME, MUNICIPALITY_ID));
		assertThat(filterCaptor.getValue()).isEqualTo(FILTER_STRING.formatted(NAMESPACE, DEPARTMENT_NAME));

	}

	@ParameterizedTest
	@MethodSource("manadatoryValueAbsentProvider")
	void getMessagingsettingsWhenMandatoryValueIsAbsent(List<MessagingSettings> clientResponse) {
		when(clientMock.getMessagingsettings(eq(MUNICIPALITY_ID), anyString())).thenReturn(clientResponse);

		final var e = assertThrows(ThrowableProblem.class, () -> integration.getMessagingsettings(MUNICIPALITY_ID, NAMESPACE, DEPARTMENT_NAME));

		verify(clientMock).getMessagingsettings(eq(MUNICIPALITY_ID), filterCaptor.capture());

		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getDetail()).isEqualTo("One or more mandatory settings [%s, %s, %s] are absent for namespace 'my-namespace' and department with name 'my-department' within municipality with id 'my-municipality'"
			.formatted(KEY_EMAIL, KEY_URL, KEY_SMS_SENDER));
		assertThat(filterCaptor.getValue()).isEqualTo(FILTER_STRING.formatted(NAMESPACE, DEPARTMENT_NAME));
	}

	private static Stream<Arguments> manadatoryValueAbsentProvider() {
		return Stream.of(
			Arguments.of(List.of(new MessagingSettings().values(emptyList()))),
			Arguments.of(List.of(new MessagingSettings().values(List.of(
				new MessagingSettingValue().key(KEY_EMAIL).value("value"))))),
			Arguments.of(List.of(new MessagingSettings().values(List.of(
				new MessagingSettingValue().key(KEY_EMAIL).value("value"),
				new MessagingSettingValue().key(KEY_URL).value("value"))))));

	}
}
