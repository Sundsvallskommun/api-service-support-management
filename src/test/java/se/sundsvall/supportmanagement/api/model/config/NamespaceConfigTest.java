package se.sundsvall.supportmanagement.api.model.config;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.subscriber.NotificationChannelType;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField.TITLE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.DECISION;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;

class NamespaceConfigTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(NamespaceConfig.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var displayName = "displayName";
		final var shortCode = "shortCode";
		final var notificationTTLInDays = 40;
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusDays(1);
		final var accessControl = true;
		final var notifyReporter = true;
		final var roleBasedMapping = true;
		final var resourceAccessControl = true;
		final var processConsumer = "pw-alkt";
		final var processTriggers = List.of(ERRAND, DECISION);
		final var smsNotificationsEnabled = true;
		final var roleFieldRestrictions = List.of(RoleFieldRestriction.create().withRole("FIRST_LINE_CASE_OFFICER"));
		final var limitedReadAccess = LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(TITLE)));
		final var reporterAccess = ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(TITLE)));
		final var teliaAceWorkItemEnabled = true;
		final var teliaAceWorkItemBaseUrl = "https://draken.sundsvall.se/kontaktsundsvall";
		final var baseUrl = "https://draken.example.com";
		final var reporterProfileId = "123e4567-e89b-12d3-a456-426614174000";
		final var newSubscriberChannels = List.of(NotificationChannelType.INTERNAL, NotificationChannelType.EMAIL);

		final var bean = NamespaceConfig.create()
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withDisplayName(displayName)
			.withShortCode(shortCode)
			.withNotificationTTLInDays(notificationTTLInDays)
			.withAccessControl(accessControl)
			.withNotifyReporter(notifyReporter)
			.withRoleBasedMapping(roleBasedMapping)
			.withResourceAccessControl(resourceAccessControl)
			.withProcessConsumer(processConsumer)
			.withProcessTriggers(processTriggers)
			.withSmsNotificationsEnabled(smsNotificationsEnabled)
			.withRoleFieldRestrictions(roleFieldRestrictions)
			.withLimitedReadAccess(limitedReadAccess)
			.withReporterAccess(reporterAccess)
			.withBaseUrl(baseUrl)
			.withReporterProfileId(reporterProfileId)
			.withNewSubscriberChannels(newSubscriberChannels)
			.withCreated(created)
			.withModified(modified)
			.withTeliaAceWorkItemEnabled(teliaAceWorkItemEnabled)
			.withTeliaAceWorkItemBaseUrl(teliaAceWorkItemBaseUrl);

		assertThat(bean).hasNoNullFieldsOrProperties();
		assertThat(bean.getNamespace()).isEqualTo(namespace);
		assertThat(bean.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getShortCode()).isEqualTo(shortCode);
		assertThat(bean.getNotificationTTLInDays()).isEqualTo(notificationTTLInDays);
		assertThat(bean.isAccessControl()).isEqualTo(accessControl);
		assertThat(bean.isNotifyReporter()).isEqualTo(notifyReporter);
		assertThat(bean.isRoleBasedMapping()).isEqualTo(roleBasedMapping);
		assertThat(bean.isResourceAccessControl()).isEqualTo(resourceAccessControl);
		assertThat(bean.getProcessConsumer()).isEqualTo(processConsumer);
		assertThat(bean.getProcessTriggers()).isEqualTo(processTriggers);
		assertThat(bean.isSmsNotificationsEnabled()).isEqualTo(smsNotificationsEnabled);
		assertThat(bean.getRoleFieldRestrictions()).isEqualTo(roleFieldRestrictions);
		assertThat(bean.getLimitedReadAccess()).isEqualTo(limitedReadAccess);
		assertThat(bean.getReporterAccess()).isEqualTo(reporterAccess);
		assertThat(bean.getBaseUrl()).isEqualTo(baseUrl);
		assertThat(bean.getReporterProfileId()).isEqualTo(reporterProfileId);
		assertThat(bean.getNewSubscriberChannels()).isEqualTo(newSubscriberChannels);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
		assertThat(bean.isTeliaAceWorkItemEnabled()).isEqualTo(teliaAceWorkItemEnabled);
		assertThat(bean.getTeliaAceWorkItemBaseUrl()).isEqualTo(teliaAceWorkItemBaseUrl);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NamespaceConfig.create()).hasAllNullFieldsOrPropertiesExcept("accessControl", "notifyReporter", "roleBasedMapping", "resourceAccessControl", "singleDecisionPerErrand", "excludeEventDescriptionsInEmail", "teliaAceWorkItemEnabled",
			"smsNotificationsEnabled")
			.satisfies(namespaceConfig -> {
				assertThat(namespaceConfig.isAccessControl()).isFalse();
				assertThat(namespaceConfig.isNotifyReporter()).isFalse();
				assertThat(namespaceConfig.isRoleBasedMapping()).isFalse();
				assertThat(namespaceConfig.isResourceAccessControl()).isFalse();
				assertThat(namespaceConfig.isSmsNotificationsEnabled()).isFalse();
			});
		assertThat(new NamespaceConfig()).hasAllNullFieldsOrPropertiesExcept("accessControl", "notifyReporter", "roleBasedMapping", "resourceAccessControl", "singleDecisionPerErrand", "excludeEventDescriptionsInEmail", "teliaAceWorkItemEnabled",
			"smsNotificationsEnabled")
			.satisfies(namespaceConfig -> {
				assertThat(namespaceConfig.isAccessControl()).isFalse();
				assertThat(namespaceConfig.isNotifyReporter()).isFalse();
				assertThat(namespaceConfig.isRoleBasedMapping()).isFalse();
				assertThat(namespaceConfig.isResourceAccessControl()).isFalse();
				assertThat(namespaceConfig.isSmsNotificationsEnabled()).isFalse();
			});
	}
}
