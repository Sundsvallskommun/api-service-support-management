package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.api.model.errand.Classification;
import se.sundsvall.supportmanagement.api.model.errand.ContactChannel;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.api.model.errand.Suspension;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.ContactReasonEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

class ErrandMapperTest {

	private static final String NAMESPACE = "namespace";

	private static final String MUNICIPALITY_ID = "municipalityId";

	private static final String ASSIGNED_GROUP_ID = "assignedGroupId";

	private static final String ASSIGNED_USER_ID = "assignedUserId";

	private static final String CATEGORY = "category";

	private static final String CLIENT_ID_TAG = "clientIdTag";

	private static final OffsetDateTime CREATED = now().minusWeeks(1);

	private static final String EXTERNAL_ID = "externalId";

	private static final String EXTERNAL_ID_TYPE_TAG = "PRIVATE";

	private static final String TAG_KEY = "tagKey";

	private static final String TAG_VALUE = "tagValue";

	private static final String ID = "id";

	private static final OffsetDateTime MODIFIED = now();

	private static final String PRIORITY = HIGH.name();

	private static final String REPORTER_USER_ID = "reporterUserId";

	private static final String STATUS = "status";

	private static final String TITLE = "title";

	private static final OffsetDateTime TOUCHED = now().plusWeeks(1);

	private static final String TYPE = "type";

	private static final String DESCRIPTION = "description";

	private static final String CHANNEL = "channel";

	private static final String RESOLUTION = "resolution";

	private static final String FIRST_NAME = "firstName";

	private static final String LAST_NAME = "lastName";

	private static final String ADDRESS = "address";

	private static final String CARE_OF = "careOf";

	private static final String ZIP_CODE = "zipCode";

	private static final String COUNTRY = "country";

	private static final String CONTACT_CHANNEL_TYPE = "contactChannelType";

	private static final String CONTACT_CHANNEL_VALUE = "contactChannelValue";

	private static final String ESCALATION_EMAIL = "escalation@email.com";

	private static final String STAKEHOLDER_ROLE = "role";

	private static final String PARAMETER_VALUE = "parameterValue";

	private static final String PARAMETER_NAME = "parameterName";

	private static final OffsetDateTime SUSPENDED_FROM = now().plusDays(1);

	private static final OffsetDateTime SUSPENDED_TO = now().plusDays(2);

	private static final String CONTACT_REASON = "contactReason";

	private static final String CONTACT_REASON_DESCRIPTION = "contactReasonDescription";

	private static final String ERRAND_NUMBER = "errandNumber";

	private static final Boolean BUSINESS_RELATED = true;

	private static Errand createErrand() {
		return Errand.create()
			.withAssignedGroupId(ASSIGNED_GROUP_ID)
			.withAssignedUserId(ASSIGNED_USER_ID)
			.withClassification(Classification.create().withCategory(CATEGORY).withType(TYPE))
			.withCreated(CREATED)
			.withStakeholders(List.of(createStakeHolder()))
			.withExternalTags(List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withParameters(Map.of(PARAMETER_NAME, List.of(PARAMETER_VALUE)))
			.withId(ID)
			.withModified(MODIFIED)
			.withPriority(Priority.valueOf(PRIORITY))
			.withReporterUserId(REPORTER_USER_ID)
			.withStatus(STATUS)
			.withTitle(TITLE)
			.withTouched(TOUCHED)
			.withResolution(RESOLUTION)
			.withDescription(DESCRIPTION)
			.withEscalationEmail(ESCALATION_EMAIL)
			.withErrandNumber(ERRAND_NUMBER)
			.withBusinessRelated(BUSINESS_RELATED)
			.withSuspension(Suspension.create().withSuspendedFrom(SUSPENDED_FROM).withSuspendedTo(SUSPENDED_TO))
			.withContactReason(CONTACT_REASON)
			.withContactReasonDescription(CONTACT_REASON_DESCRIPTION);
	}

	private static Stakeholder createStakeHolder() {
		return Stakeholder.create()
			.withExternalId(EXTERNAL_ID)
			.withExternalIdType("PRIVATE")
			.withFirstName(FIRST_NAME)
			.withLastName(LAST_NAME)
			.withAddress(ADDRESS)
			.withCareOf(CARE_OF)
			.withZipCode(ZIP_CODE)
			.withCountry(COUNTRY)
			.withContactChannels(List.of(ContactChannel.create().withType(CONTACT_CHANNEL_TYPE).withValue(CONTACT_CHANNEL_VALUE)))
			.withRole(STAKEHOLDER_ROLE);
	}

	private static ErrandEntity createEntity() {
		return ErrandEntity.create()
			.withId(ID)
			.withAssignedGroupId(ASSIGNED_GROUP_ID)
			.withAssignedUserId(ASSIGNED_USER_ID)
			.withCategory(CATEGORY)
			.withNamespace(CLIENT_ID_TAG)
			.withCreated(CREATED)
			.withStakeholders(List.of(createStakeHolderEntity()))
			.withExternalTags(List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withParameters(Map.of(PARAMETER_NAME, ParameterEntity.create().withValues(List.of(PARAMETER_VALUE))))
			.withMunicipalityId(MUNICIPALITY_ID)
			.withPriority(PRIORITY)
			.withReporterUserId(REPORTER_USER_ID)
			.withStatus(STATUS)
			.withTitle(TITLE)
			.withType(TYPE)
			.withTouched(TOUCHED)
			.withModified(MODIFIED)
			.withResolution(RESOLUTION)
			.withDescription(DESCRIPTION)
			.withChannel(CHANNEL)
			.withEscalationEmail(ESCALATION_EMAIL)
			.withContactReason(ContactReasonEntity.create().withReason(CONTACT_REASON))
			.withContactReasonDescription(CONTACT_REASON_DESCRIPTION)
			.withSuspendedFrom(SUSPENDED_FROM)
			.withSuspendedTo(SUSPENDED_TO)
			.withErrandNumber(ERRAND_NUMBER)
			.withBusinessRelated(BUSINESS_RELATED);
	}

	private static StakeholderEntity createStakeHolderEntity() {
		return StakeholderEntity.create()
			.withExternalId(EXTERNAL_ID)
			.withExternalIdType(EXTERNAL_ID_TYPE_TAG)
			.withFirstName(FIRST_NAME)
			.withLastName(LAST_NAME)
			.withAddress(ADDRESS)
			.withCareOf(CARE_OF)
			.withZipCode(ZIP_CODE)
			.withCountry(COUNTRY)
			.withContactChannels(List.of(ContactChannelEntity.create().withType(CONTACT_CHANNEL_TYPE).withValue(CONTACT_CHANNEL_VALUE)))
			.withRole(STAKEHOLDER_ROLE);
	}

	@Test
	void testToErrand() {
		final var errand = toErrand(createEntity());

		assertThat(errand.getAssignedGroupId()).isEqualTo(ASSIGNED_GROUP_ID);
		assertThat(errand.getAssignedUserId()).isEqualTo(ASSIGNED_USER_ID);
		assertThat(errand.getClassification().getCategory()).isEqualTo(CATEGORY);
		assertThat(errand.getClassification().getType()).isEqualTo(TYPE);
		assertThat(errand.getCreated()).isCloseTo(CREATED, within(2, SECONDS));
		assertThat(errand.getStakeholders()).hasSize(1)
			.extracting(Stakeholder::getExternalId, Stakeholder::getExternalIdType, Stakeholder::getFirstName, Stakeholder::getLastName, Stakeholder::getAddress, Stakeholder::getCareOf, Stakeholder::getZipCode, Stakeholder::getCountry,
				Stakeholder::getContactChannels)
			.contains(tuple(EXTERNAL_ID, EXTERNAL_ID_TYPE_TAG, FIRST_NAME, LAST_NAME, ADDRESS, CARE_OF, ZIP_CODE, COUNTRY, List.of(ContactChannel.create().withType(CONTACT_CHANNEL_TYPE).withValue(CONTACT_CHANNEL_VALUE))));
		assertThat(errand.getExternalTags()).hasSize(1)
			.extracting(ExternalTag::getKey, ExternalTag::getValue)
			.contains(tuple(TAG_KEY, TAG_VALUE));
		assertThat(errand.getId()).isEqualTo(ID);
		assertThat(errand.getModified()).isCloseTo(MODIFIED, within(2, SECONDS));
		assertThat(errand.getPriority()).isEqualTo(Priority.valueOf(PRIORITY));
		assertThat(errand.getReporterUserId()).isEqualTo(REPORTER_USER_ID);
		assertThat(errand.getStatus()).isEqualTo(STATUS);
		assertThat(errand.getTitle()).isEqualTo(TITLE);
		assertThat(errand.getTouched()).isEqualTo(TOUCHED);
		assertThat(errand.getResolution()).isEqualTo(RESOLUTION);
		assertThat(errand.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(errand.getChannel()).isEqualTo(CHANNEL);
		assertThat(errand.getEscalationEmail()).isEqualTo(ESCALATION_EMAIL);
		assertThat(errand.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(errand.getParameters()).hasSize(1).containsEntry(PARAMETER_NAME, List.of(PARAMETER_VALUE));
		assertThat(errand.getBusinessRelated()).isEqualTo(BUSINESS_RELATED);
		assertThat(errand.getContactReason()).isEqualTo(CONTACT_REASON);
		assertThat(errand.getContactReasonDescription()).isEqualTo(CONTACT_REASON_DESCRIPTION);
		assertThat(errand.getLabels()).isNull();
		assertThat(errand).hasNoNullFieldsOrPropertiesExcept("labels");
	}

	@Test
	void testToErrandFromNull() {
		assertThat(toErrand(null)).isNull();
	}

	@Test
	void testToErrands() {
		final var errands = toErrands(List.of(createEntity()));

		assertThat(errands).hasSize(1)
			.extracting(
				Errand::getAssignedGroupId,
				Errand::getAssignedUserId,
				Errand::getClassification,
				Errand::getCreated,
				Errand::getStakeholders,
				Errand::getExternalTags,
				Errand::getParameters,
				Errand::getId,
				Errand::getModified,
				Errand::getPriority,
				Errand::getReporterUserId,
				Errand::getStatus,
				Errand::getTitle,
				Errand::getTouched,
				Errand::getResolution,
				Errand::getDescription,
				Errand::getChannel,
				Errand::getEscalationEmail,
				Errand::getBusinessRelated,
				Errand::getContactReason,
				Errand::getErrandNumber)
			.containsExactly(tuple(
				ASSIGNED_GROUP_ID,
				ASSIGNED_USER_ID,
				Classification.create().withCategory(CATEGORY).withType(TYPE),
				CREATED,
				List.of(createStakeHolder()),
				List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
				Map.of(PARAMETER_NAME, List.of(PARAMETER_VALUE)),
				ID,
				MODIFIED,
				Priority.valueOf(PRIORITY),
				REPORTER_USER_ID,
				STATUS,
				TITLE,
				TOUCHED,
				RESOLUTION,
				DESCRIPTION,
				CHANNEL,
				ESCALATION_EMAIL,
				BUSINESS_RELATED,
				CONTACT_REASON,
				ERRAND_NUMBER));

		assertThat(errands.getFirst().getLabels()).isNull();
		assertThat(errands.getFirst()).hasNoNullFieldsOrPropertiesExcept("labels");
	}

	@Test
	void testToErrandsFromNull() {
		assertThat(toErrands(null)).isEmpty();
	}

	@Test
	void testToErrandEntity() {
		final var entity = toErrandEntity(NAMESPACE, MUNICIPALITY_ID, createErrand());

		assertThat(entity)
			.isNotNull()
			.extracting(
				ErrandEntity::getAssignedGroupId,
				ErrandEntity::getAssignedUserId,
				ErrandEntity::getAttachments,
				ErrandEntity::getCategory,
				ErrandEntity::getExternalTags,
				ErrandEntity::getMunicipalityId,
				ErrandEntity::getNamespace,
				ErrandEntity::getPriority,
				ErrandEntity::getReporterUserId,
				ErrandEntity::getStatus,
				ErrandEntity::getTitle,
				ErrandEntity::getType,
				ErrandEntity::getResolution,
				ErrandEntity::getDescription,
				ErrandEntity::getEscalationEmail,
				ErrandEntity::getBusinessRelated,
				ErrandEntity::getErrandNumber,
				ErrandEntity::getParameters)
			.containsExactly(
				ASSIGNED_GROUP_ID,
				ASSIGNED_USER_ID,
				emptyList(),
				CATEGORY,
				List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
				MUNICIPALITY_ID,
				NAMESPACE,
				PRIORITY,
				REPORTER_USER_ID,
				STATUS,
				TITLE,
				TYPE,
				RESOLUTION,
				DESCRIPTION,
				ESCALATION_EMAIL,
				BUSINESS_RELATED,
				ERRAND_NUMBER,
				Map.of(PARAMETER_NAME, ParameterEntity.create().withValues(List.of(PARAMETER_VALUE))));

		assertThat(entity.getStakeholders()).hasSize(1).extracting(
				StakeholderEntity::getAddress,
				StakeholderEntity::getCareOf,
				StakeholderEntity::getCountry,
				StakeholderEntity::getExternalId,
				StakeholderEntity::getExternalIdType,
				StakeholderEntity::getFirstName,
				StakeholderEntity::getLastName,
				StakeholderEntity::getZipCode,
				StakeholderEntity::getRole)
			.containsExactly(tuple(ADDRESS,
				CARE_OF,
				COUNTRY,
				EXTERNAL_ID,
				EXTERNAL_ID_TYPE_TAG,
				FIRST_NAME,
				LAST_NAME,
				ZIP_CODE,
				STAKEHOLDER_ROLE));

		assertThat(entity.getStakeholders().getFirst().getContactChannels()).hasSize(1).extracting(
				ContactChannelEntity::getType,
				ContactChannelEntity::getValue)
			.containsExactly(tuple(
				CONTACT_CHANNEL_TYPE,
				CONTACT_CHANNEL_VALUE));

		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getId()).isNull();
		assertThat(entity.getModified()).isNull();
		assertThat(entity.getTouched()).isNull();
	}

	@Test
	void testToErrandEntityFromNull() {
		assertThat(toErrandEntity(null, null, null)).isNull();
		assertThat(toErrandEntity(null, null, Errand.create())).isNull();
		assertThat(toErrandEntity(NAMESPACE, null, null)).isNull();
		assertThat(toErrandEntity(NAMESPACE, null, Errand.create())).isNull();
		assertThat(toErrandEntity(NAMESPACE, MUNICIPALITY_ID, null)).isNull();
		assertThat(toErrandEntity(null, MUNICIPALITY_ID, null)).isNull();
		assertThat(toErrandEntity(NAMESPACE, MUNICIPALITY_ID, null)).isNull();
		assertThat(toErrandEntity(null, MUNICIPALITY_ID, Errand.create())).isNull();
		assertThat(toErrandEntity(null, null, null)).isNull();
	}

	@Test
	void testUpdateEmptyEntity() {
		final List<AttachmentEntity> attachments = new ArrayList<>();
		final List<StakeholderEntity> stakeholders = new ArrayList<>();
		final var entity = updateEntity(ErrandEntity.create().withErrandNumber(ERRAND_NUMBER).withAttachments(attachments).withStakeholders(stakeholders), createErrand());

		assertThat(entity.getAttachments()).isSameAs(attachments); // Test to verify that list has not been replaced
		assertThat(entity.getStakeholders()).isSameAs(stakeholders); // Test to verify that list has not been replaced
		assertThat(entity.getErrandNumber()).isSameAs(ERRAND_NUMBER); // Test to verify that errandNumber has not been replaced
		assertThat(entity)
			.extracting(
				ErrandEntity::getAssignedGroupId,
				ErrandEntity::getAssignedUserId,
				ErrandEntity::getCategory,
				ErrandEntity::getExternalTags,
				ErrandEntity::getPriority,
				ErrandEntity::getStatus,
				ErrandEntity::getTitle,
				ErrandEntity::getType,
				ErrandEntity::getResolution,
				ErrandEntity::getDescription,
				ErrandEntity::getEscalationEmail,
				ErrandEntity::getSuspendedFrom,
				ErrandEntity::getSuspendedTo,
				ErrandEntity::getBusinessRelated)
			.containsExactly(
				ASSIGNED_GROUP_ID,
				ASSIGNED_USER_ID,
				CATEGORY,
				List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
				PRIORITY,
				STATUS,
				TITLE,
				TYPE,
				RESOLUTION,
				DESCRIPTION,
				ESCALATION_EMAIL,
				SUSPENDED_FROM,
				SUSPENDED_TO,
				BUSINESS_RELATED);

		assertThat(entity.getStakeholders()).hasSize(1).extracting(
				StakeholderEntity::getAddress,
				StakeholderEntity::getCareOf,
				StakeholderEntity::getCountry,
				StakeholderEntity::getExternalId,
				StakeholderEntity::getExternalIdType,
				StakeholderEntity::getFirstName,
				StakeholderEntity::getLastName,
				StakeholderEntity::getZipCode,
				StakeholderEntity::getRole)
			.containsExactly(tuple(ADDRESS,
				CARE_OF,
				COUNTRY,
				EXTERNAL_ID,
				EXTERNAL_ID_TYPE_TAG,
				FIRST_NAME,
				LAST_NAME,
				ZIP_CODE,
				STAKEHOLDER_ROLE));

		assertThat(entity.getStakeholders().getFirst().getContactChannels()).hasSize(1).extracting(
				ContactChannelEntity::getType,
				ContactChannelEntity::getValue)
			.containsExactly(tuple(
				CONTACT_CHANNEL_TYPE,
				CONTACT_CHANNEL_VALUE));

		assertThat(entity.getNamespace()).isNull();
		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getId()).isNull();
		assertThat(entity.getReporterUserId()).isNull();
		assertThat(entity.getModified()).isNull();
		assertThat(entity.getTouched()).isNull();
	}

	@Test
	void testUpdateEntityWithBlank() {
		final var entity = updateEntity(createEntity(), Errand.create()
			.withAssignedGroupId("")
			.withAssignedUserId("")
			.withErrandNumber("")
			.withResolution("")
			.withDescription("")
			.withChannel("")
			.withEscalationEmail("")
			.withContactReason("")
			.withContactReasonDescription(""));

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept(
			"assignedGroupId", "assignedUserId", "attachments", "resolution", "description", "channel", "escalationEmail", "parameters", "businessRelated", "suspend", "previousStatus", "tempPreviousStatus", "timeMeasures", "contactReasonDescription");
		assertThat(entity.getAssignedGroupId()).isNull();
		assertThat(entity.getAssignedUserId()).isNull();
		assertThat(entity.getAttachments()).isNull();
		assertThat(entity.getResolution()).isNull();
		assertThat(entity.getDescription()).isNull();
		assertThat(entity.getEscalationEmail()).isNull();
		assertThat(entity.getContactReasonDescription()).isNull();
	}

	@Test
	void testUpdateEntityWithEmptyRequest() {
		assertThat(updateEntity(createEntity(), Errand.create())).usingRecursiveComparison().isEqualTo(createEntity());
	}

	@Test
	void testUpdateEntityWithNull() {
		assertThat(updateEntity(createEntity(), null)).usingRecursiveComparison().isEqualTo(createEntity());
	}

}
