package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;

class ErrandEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var now = OffsetDateTime.now();

		final var assignedUserId = "assignedUserId";
		final var assignedGroupId = "assignedGroupId";
		final var attachmentId = UUID.randomUUID().toString();
		final var attachments = List.of(AttachmentEntity.create().withId(attachmentId).withFileName("fileName").withAttachmentData(AttachmentDataEntity.create().withFile(new MariaDbBlob("file".getBytes()))).withMimeType("mimeType"));
		final var category = "category";
		final var namespace = "namespace";
		final var stakeholder = StakeholderEntity.create().withExternalId(UUID.randomUUID().toString()).withExternalIdType("PRIVATE");
		final var description = "description";
		final var channel = "channel";
		final var externalTags = List.of(DbExternalTag.create().withKey("key").withValue("value"));
		final var id = UUID.randomUUID().toString();
		final var municipalityId = "municipalityId";
		final var priority = "priority";
		final var reporterUserId = "reporterUserId";
		final var resolution = "resolution";
		final var status = "status";
		final var title = "title";
		final var type = "type";
		final var escalationEmail = "escalation@email.com";
		final var errandNumber = "errandNumber";
		final var parameters = List.of(ParameterEntity.create());
		final var businessRelated = true;
		final var contactReason = ContactReasonEntity.create().withReason("reason");
		final var contactReasonDescription = "contactReasonDescription";
		final var previousStatus = "previousStatus";
		final var timeMeasure = List.of(TimeMeasurementEntity.create().withStartTime(now).withStopTime(now).withDescription("description").withAdministrator("administrator"));
		final var tempPreviousStatus = "tempPreviousStatus";
		final var labels = List.of("label1", "label2");
		final var notifications = List.of(NotificationEntity.create());

		final var errandEntity = ErrandEntity.create()
			.withAssignedGroupId(assignedGroupId)
			.withAssignedUserId(assignedUserId)
			.withAttachments(attachments)
			.withCategory(category)
			.withNamespace(namespace)
			.withStakeholders(List.of(stakeholder))
			.withDescription(description)
			.withChannel(channel)
			.withExternalTags(externalTags)
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withResolution(resolution)
			.withStatus(status)
			.withTitle(title)
			.withType(type)
			.withParameters(parameters)
			.withEscalationEmail(escalationEmail)
			.withBusinessRelated(businessRelated)
			.withContactReason(contactReason)
			.withContactReasonDescription(contactReasonDescription)
			.withErrandNumber(errandNumber)
			.withTouched(now)
			.withCreated(now)
			.withModified(now)
			.withSuspendedFrom(now)
			.withSuspendedTo(now)
			.withPreviousStatus(previousStatus)
			.withTimeMeasures(timeMeasure)
			.withTempPreviousStatus(tempPreviousStatus)
			.withNotifications(notifications)
			.withLabels(labels);

		assertThat(errandEntity).hasNoNullFieldsOrProperties();
		assertThat(errandEntity.getAssignedGroupId()).isEqualTo(assignedGroupId);
		assertThat(errandEntity.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(errandEntity.getAttachments()).isEqualTo(attachments);
		assertThat(errandEntity.getCategory()).isEqualTo(category);
		assertThat(errandEntity.getNamespace()).isEqualTo(namespace);
		assertThat(errandEntity.getStakeholders()).containsExactly(stakeholder);
		assertThat(errandEntity.getDescription()).isEqualTo(description);
		assertThat(errandEntity.getChannel()).isEqualTo(channel);
		assertThat(errandEntity.getExternalTags()).isEqualTo(externalTags);
		assertThat(errandEntity.getId()).isEqualTo(id);
		assertThat(errandEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(errandEntity.getPriority()).isEqualTo(priority);
		assertThat(errandEntity.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(errandEntity.getResolution()).isEqualTo(resolution);
		assertThat(errandEntity.getStatus()).isEqualTo(status);
		assertThat(errandEntity.getTitle()).isEqualTo(title);
		assertThat(errandEntity.getType()).isEqualTo(type);
		assertThat(errandEntity.getParameters()).isEqualTo(parameters);
		assertThat(errandEntity.getEscalationEmail()).isEqualTo(escalationEmail);
		assertThat(errandEntity.getErrandNumber()).isEqualTo(errandNumber);
		assertThat(errandEntity.getBusinessRelated()).isEqualTo(businessRelated);
		assertThat(errandEntity.getContactReason()).isEqualTo(contactReason);
		assertThat(errandEntity.getContactReasonDescription()).isEqualTo(contactReasonDescription);
		assertThat(errandEntity).extracting(ErrandEntity::getModified,
			ErrandEntity::getTouched, ErrandEntity::getSuspendedFrom, ErrandEntity::getSuspendedTo,
			ErrandEntity::getCreated).allSatisfy(date -> assertThat(date).isEqualTo(now));
		assertThat(errandEntity.getPreviousStatus()).isEqualTo(previousStatus);
		assertThat(errandEntity.getTimeMeasures()).isSameAs(timeMeasure);
		assertThat(errandEntity.getTempPreviousStatus()).isEqualTo(tempPreviousStatus);
		assertThat(errandEntity.getLabels()).isEqualTo(labels);
		assertThat(errandEntity.getNotifications()).isEqualTo(notifications);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ErrandEntity.create()).hasAllNullFieldsOrPropertiesExcept("timeMeasures");
		assertThat(new ErrandEntity()).hasAllNullFieldsOrPropertiesExcept("timeMeasures");
	}

}
