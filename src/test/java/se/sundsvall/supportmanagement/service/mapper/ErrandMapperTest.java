package se.sundsvall.supportmanagement.service.mapper;

import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.errand.ContactChannel;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.api.model.errand.Stakeholder;
import se.sundsvall.supportmanagement.integration.db.model.ContactChannelEntity;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;

class ErrandMapperTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ASSIGNED_GROUP_ID = "assignedGroupId";
	private static final String ASSIGNED_USER_ID = "assignedUserId";
	private static final String CATEGORY_TAG = "categoryTag";
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
	private static final String STATUS_TAG = "statusTag";
	private static final String TITLE = "title";
	private static final OffsetDateTime TOUCHED = now().plusWeeks(1);
	private static final String TYPE_TAG = "typeTag";

	private static final String DESCRIPTION = "description";
	private static final String RESOLUTION = "resolution";

	private static final String FIRST_NAME = "firstName";
	private static final String LAST_NAME = "lastName";
	private static final String ADDRESS = "address";
	private static final String CARE_OF = "careOf";
	private static final String ZIP_CODE = "zipCode";
	private static final String COUNTRY = "country";
	private static final String CONTACT_CHANNEL_TYPE = "contactChannelType";
	private static final String CONTACT_CHANNEL_VALUE = "contactChannelValue";

	@Test
	void testToErrand() {
		final var errand = toErrand(createEntity());

		assertThat(errand.getAssignedGroupId()).isEqualTo(ASSIGNED_GROUP_ID);
		assertThat(errand.getAssignedUserId()).isEqualTo(ASSIGNED_USER_ID);
		assertThat(errand.getCategoryTag()).isEqualTo(CATEGORY_TAG);
		assertThat(errand.getCreated()).isCloseTo(CREATED, within(2, SECONDS));
		assertThat(errand.getStakeholders()).hasSize(1)
				.extracting(Stakeholder::getExternalId, Stakeholder::getexternalIdTypeTag, Stakeholder::getFirstName, Stakeholder::getLastName, Stakeholder::getAddress, Stakeholder::getCareOf, Stakeholder::getZipCode, Stakeholder::getCountry, Stakeholder::getContactChannels)
				.contains(tuple(EXTERNAL_ID, String.valueOf(EXTERNAL_ID_TYPE_TAG), FIRST_NAME, LAST_NAME, ADDRESS, CARE_OF, ZIP_CODE, COUNTRY, List.of(ContactChannel.create().withType(CONTACT_CHANNEL_TYPE).withValue(CONTACT_CHANNEL_VALUE))));
		assertThat(errand.getExternalTags()).hasSize(1)
				.extracting(ExternalTag::getKey, ExternalTag::getValue)
				.contains(tuple(TAG_KEY, TAG_VALUE));
		assertThat(errand.getId()).isEqualTo(ID);
		assertThat(errand.getModified()).isCloseTo(MODIFIED, within(2, SECONDS));
		assertThat(errand.getPriority()).isEqualTo(Priority.valueOf(PRIORITY));
		assertThat(errand.getReporterUserId()).isEqualTo(REPORTER_USER_ID);
		assertThat(errand.getStatusTag()).isEqualTo(STATUS_TAG);
		assertThat(errand.getTitle()).isEqualTo(TITLE);
		assertThat(errand.getTouched()).isEqualTo(TOUCHED);
		assertThat(errand.getTypeTag()).isEqualTo(TYPE_TAG);
		assertThat(errand.getResolution()).isEqualTo(RESOLUTION);
		assertThat(errand.getDescription()).isEqualTo(DESCRIPTION);
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
						Errand::getCategoryTag,
						Errand::getCreated,
						Errand::getStakeholders,
						Errand::getExternalTags,
						Errand::getId,
						Errand::getModified,
						Errand::getPriority,
						Errand::getReporterUserId,
						Errand::getStatusTag,
						Errand::getTitle,
						Errand::getTouched,
						Errand::getTypeTag,
						Errand::getResolution,
						Errand::getDescription)
				.containsExactly(tuple(
						ASSIGNED_GROUP_ID,
						ASSIGNED_USER_ID,
						CATEGORY_TAG,
						CREATED,
						List.of(createStakeHolder()),
						List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
						ID,
						MODIFIED,
						Priority.valueOf(PRIORITY),
						REPORTER_USER_ID,
						STATUS_TAG,
						TITLE,
						TOUCHED,
						TYPE_TAG,
						RESOLUTION,
						DESCRIPTION));
	}

	@Test
	void testToErrandsFromNull() {
		assertThat(toErrands(null)).isEmpty();
	}

	@Test
	void testToErrandEntity() {
		final var entity = toErrandEntity(NAMESPACE, MUNICIPALITY_ID, createErrand());

		assertThat(entity)
				.extracting(
						ErrandEntity::getAssignedGroupId,
						ErrandEntity::getAssignedUserId,
						ErrandEntity::getCategoryTag,
						ErrandEntity::getStakeholders,
						ErrandEntity::getExternalTags,
						ErrandEntity::getMunicipalityId,
						ErrandEntity::getNamespace,
						ErrandEntity::getPriority,
						ErrandEntity::getReporterUserId,
						ErrandEntity::getStatusTag,
						ErrandEntity::getTitle,
						ErrandEntity::getTypeTag,
						ErrandEntity::getResolution,
						ErrandEntity::getDescription)
				.containsExactly(
						ASSIGNED_GROUP_ID,
						ASSIGNED_USER_ID,
						CATEGORY_TAG,
						List.of(createStakeHolderEntity()),
						List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
						MUNICIPALITY_ID,
						NAMESPACE,
						PRIORITY,
						REPORTER_USER_ID,
						STATUS_TAG,
						TITLE,
						TYPE_TAG,
						RESOLUTION,
						DESCRIPTION);

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
	}

	@Test
	void testUpdateEmptyEntity() {
		final var entity = updateEntity(ErrandEntity.create(), createErrand());

		assertThat(entity)
				.extracting(
						ErrandEntity::getAssignedGroupId,
						ErrandEntity::getAssignedUserId,
						ErrandEntity::getCategoryTag,
						ErrandEntity::getStakeholders,
						ErrandEntity::getExternalTags,
						ErrandEntity::getPriority,
						ErrandEntity::getStatusTag,
						ErrandEntity::getTitle,
						ErrandEntity::getTypeTag,
						ErrandEntity::getResolution,
						ErrandEntity::getDescription)
				.containsExactly(
						ASSIGNED_GROUP_ID,
						ASSIGNED_USER_ID,
						CATEGORY_TAG,
						List.of(createStakeHolderEntity()),
						List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
						PRIORITY,
						STATUS_TAG,
						TITLE,
						TYPE_TAG,
						RESOLUTION,
						DESCRIPTION);

		assertThat(entity.getNamespace()).isNull();
		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getId()).isNull();
		assertThat(entity.getReporterUserId()).isNull();
		assertThat(entity.getModified()).isNull();
		assertThat(entity.getTouched()).isNull();
	}

	@Test
	void testUpdateEntityWithBlank() {
		final var entity = updateEntity(createEntity(), Errand.create().withAssignedGroupId("").withAssignedUserId("").withResolution("").withDescription(""));

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("assignedGroupId", "assignedUserId", "attachments", "resolution", "description");
		assertThat(entity.getAssignedGroupId()).isNull();
		assertThat(entity.getAssignedUserId()).isNull();
		assertThat(entity.getAttachments()).isNull();
		assertThat(entity.getResolution()).isNull();
		assertThat(entity.getDescription()).isNull();
	}

	@Test
	void testUpdateEntityWithEmptyRequest() {
		assertThat(updateEntity(createEntity(), Errand.create())).usingRecursiveComparison().isEqualTo(createEntity());
	}

	@Test
	void testUpdateEntityWithNull() {
		assertThat(updateEntity(createEntity(), null)).usingRecursiveComparison().isEqualTo(createEntity());
	}

	private static Errand createErrand() {
		return Errand.create()
				.withAssignedGroupId(ASSIGNED_GROUP_ID)
				.withAssignedUserId(ASSIGNED_USER_ID)
				.withCategoryTag(CATEGORY_TAG)
				.withCreated(CREATED)
				.withStakeholders(List.of(createStakeHolder()))
				.withExternalTags(List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
				.withId(ID)
				.withModified(MODIFIED)
				.withPriority(Priority.valueOf(PRIORITY))
				.withReporterUserId(REPORTER_USER_ID)
				.withStatusTag(STATUS_TAG)
				.withTitle(TITLE)
				.withTouched(TOUCHED)
				.withTypeTag(TYPE_TAG)
				.withResolution(RESOLUTION)
				.withDescription(DESCRIPTION);
	}

	private static Stakeholder createStakeHolder() {
		return Stakeholder.create()
				.withExternalId(EXTERNAL_ID)
				.withexternalIdTypeTag("PRIVATE")
				.withFirstName(FIRST_NAME)
				.withLastName(LAST_NAME)
				.withAddress(ADDRESS)
				.withCareOf(CARE_OF)
				.withZipCode(ZIP_CODE)
				.withCountry(COUNTRY)
				.withContactChannels(List.of(ContactChannel.create().withType(CONTACT_CHANNEL_TYPE).withValue(CONTACT_CHANNEL_VALUE)));
	}

	private static ErrandEntity createEntity() {
		return ErrandEntity.create()
				.withAssignedGroupId(ASSIGNED_GROUP_ID)
				.withAssignedUserId(ASSIGNED_USER_ID)
				.withCategoryTag(CATEGORY_TAG)
				.withNamespace(CLIENT_ID_TAG)
				.withCreated(CREATED)
				.withStakeholders(List.of(createStakeHolderEntity()))
				.withExternalTags(List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
				.withId(ID)
				.withMunicipalityId(MUNICIPALITY_ID)
				.withPriority(PRIORITY)
				.withReporterUserId(REPORTER_USER_ID)
				.withStatusTag(STATUS_TAG)
				.withTitle(TITLE)
				.withTypeTag(TYPE_TAG)
				.withTouched(TOUCHED)
				.withModified(MODIFIED)
				.withResolution(RESOLUTION)
				.withDescription(DESCRIPTION);
	}

	private static StakeholderEntity createStakeHolderEntity() {
		return StakeholderEntity.create()
				.withExternalId(EXTERNAL_ID)
				.withexternalIdTypeTag(EXTERNAL_ID_TYPE_TAG)
				.withFirstName(FIRST_NAME)
				.withLastName(LAST_NAME)
				.withAddress(ADDRESS)
				.withCareOf(CARE_OF)
				.withZipCode(ZIP_CODE)
				.withCountry(COUNTRY)
				.withContactChannels(List.of(ContactChannelEntity.create().withType(CONTACT_CHANNEL_TYPE).withValue(CONTACT_CHANNEL_VALUE)));
	}
}
