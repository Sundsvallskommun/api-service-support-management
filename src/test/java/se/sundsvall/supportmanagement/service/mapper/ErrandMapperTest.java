package se.sundsvall.supportmanagement.service.mapper;

import org.junit.jupiter.api.Test;

import se.sundsvall.supportmanagement.api.model.errand.Customer;
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.Priority;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.EmbeddableCustomer;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.PRIVATE;
import static se.sundsvall.supportmanagement.api.model.errand.Priority.HIGH;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;

class ErrandMapperTest {
	private static final String ASSIGNED_GROUP_ID = "assignedGroupId";
	private static final String ASSIGNED_USER_ID = "assignedUserId";
	private static final String CATEGORY_TAG = "categoryTag";
	private static final String CLIENT_ID_TAG = "clientIdTag";
	private static final OffsetDateTime CREATED = now().minusWeeks(1);
	private static final String CUSTOMER_ID = "customerId";
	private static final String CUSTOMER_TYPE = PRIVATE.toString();
	private static final String TAG_KEY = "tagKey";
	private static final String TAG_VALUE = "tagValue";
	private static final String ID = "id";
	private static final OffsetDateTime MODIFIED = now();
	private static final String PRIORITY = HIGH.name();
	private static final String REPORTER_USER_ID = "reporterUserId";
	private static final String STATUS_TAG = "statusTag";
	private static final String TITLE = "title";
	private static final String TYPE_TAG = "typeTag";

	@Test
	void testToErrand() {
		final var errand = toErrand(createEntity());

		assertThat(errand.getAssignedGroupId()).isEqualTo(ASSIGNED_GROUP_ID);
		assertThat(errand.getAssignedUserId()).isEqualTo(ASSIGNED_USER_ID);
		assertThat(errand.getCategoryTag()).isEqualTo(CATEGORY_TAG);
		assertThat(errand.getClientIdTag()).isEqualTo(CLIENT_ID_TAG);
		assertThat(errand.getCreated()).isCloseTo(CREATED, within(2, SECONDS));
		assertThat(errand.getCustomer()).isNotNull();
		assertThat(errand.getCustomer().getId()).isEqualTo(CUSTOMER_ID);
		assertThat(errand.getCustomer().getType()).isEqualTo(CustomerType.valueOf(CUSTOMER_TYPE));
		assertThat(errand.getExternalTags()).hasSize(1)
			.extracting(ExternalTag::getKey, ExternalTag::getValue)
			.contains(tuple(TAG_KEY, TAG_VALUE));
		assertThat(errand.getId()).isEqualTo(ID);
		assertThat(errand.getModified()).isCloseTo(MODIFIED, within(2, SECONDS));
		assertThat(errand.getPriority()).isEqualTo(Priority.valueOf(PRIORITY));
		assertThat(errand.getReporterUserId()).isEqualTo(REPORTER_USER_ID);
		assertThat(errand.getStatusTag()).isEqualTo(STATUS_TAG);
		assertThat(errand.getTitle()).isEqualTo(TITLE);
		assertThat(errand.getTypeTag()).isEqualTo(TYPE_TAG);
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
				Errand::getClientIdTag,
				Errand::getCreated,
				Errand::getCustomer,
				Errand::getExternalTags,
				Errand::getId,
				Errand::getModified,
				Errand::getPriority,
				Errand::getReporterUserId,
				Errand::getStatusTag,
				Errand::getTitle,
				Errand::getTypeTag)
			.containsExactly(tuple(
				ASSIGNED_GROUP_ID,
				ASSIGNED_USER_ID,
				CATEGORY_TAG,
				CLIENT_ID_TAG,
				CREATED,
				Customer.create().withId(CUSTOMER_ID).withType(CustomerType.valueOf(CUSTOMER_TYPE)),
				List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
				ID,
				MODIFIED,
				Priority.valueOf(PRIORITY),
				REPORTER_USER_ID,
				STATUS_TAG,
				TITLE,
				TYPE_TAG));
	}

	@Test
	void testToErrandsFromNull() {
		assertThat(toErrands(null)).isEmpty();
	}

	@Test
	void testToErrandEntity() {
		final var entity = toErrandEntity(createErrand());

		assertThat(entity)
			.extracting(
				ErrandEntity::getAssignedGroupId,
				ErrandEntity::getAssignedUserId,
				ErrandEntity::getCategoryTag,
				ErrandEntity::getCustomer,
				ErrandEntity::getClientIdTag,
				ErrandEntity::getExternalTags,
				ErrandEntity::getPriority,
				ErrandEntity::getReporterUserId,
				ErrandEntity::getStatusTag,
				ErrandEntity::getTitle,
				ErrandEntity::getTypeTag)
			.containsExactly(
				ASSIGNED_GROUP_ID,
				ASSIGNED_USER_ID,
				CATEGORY_TAG,
				EmbeddableCustomer.create().withId(CUSTOMER_ID).withType(CUSTOMER_TYPE),
				CLIENT_ID_TAG,
				List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
				PRIORITY,
				REPORTER_USER_ID,
				STATUS_TAG,
				TITLE,
				TYPE_TAG);

		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getId()).isNull();
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testToErrandEntityFromNull() {
		assertThat(toErrandEntity(null)).isNull();
	}

	@Test
	void testUpdateEmptyEntity() {
		final var entity = updateEntity(ErrandEntity.create(), createErrand());

		assertThat(entity)
			.extracting(
				ErrandEntity::getAssignedGroupId,
				ErrandEntity::getAssignedUserId,
				ErrandEntity::getCategoryTag,
				ErrandEntity::getCustomer,
				ErrandEntity::getExternalTags,
				ErrandEntity::getPriority,
				ErrandEntity::getStatusTag,
				ErrandEntity::getTitle,
				ErrandEntity::getTypeTag)
			.containsExactly(
				ASSIGNED_GROUP_ID,
				ASSIGNED_USER_ID,
				CATEGORY_TAG,
				EmbeddableCustomer.create().withId(CUSTOMER_ID).withType(CUSTOMER_TYPE),
				List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)),
				PRIORITY,
				STATUS_TAG,
				TITLE,
				TYPE_TAG);

		assertThat(entity.getClientIdTag()).isNull();
		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getId()).isNull();
		assertThat(entity.getReporterUserId()).isNull();
		assertThat(entity.getModified()).isNull();
	}

	@Test
	void testUpdateEntityWithBlankAssignedGroupIdAndBlankAssignedUserId() {
		final var entity = updateEntity(createEntity(), Errand.create().withAssignedGroupId("").withAssignedUserId(""));

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("assignedGroupId", "assignedUserId", "attachments");
		assertThat(entity.getAssignedGroupId()).isNull();
		assertThat(entity.getAssignedUserId()).isNull();
		assertThat(entity.getAttachments()).isNull();
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
			.withClientIdTag(CLIENT_ID_TAG)
			.withCreated(CREATED)
			.withCustomer(Customer.create().withId(CUSTOMER_ID).withType(CustomerType.valueOf(CUSTOMER_TYPE)))
			.withExternalTags(List.of(ExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withId(ID)
			.withModified(MODIFIED)
			.withPriority(Priority.valueOf(PRIORITY))
			.withReporterUserId(REPORTER_USER_ID)
			.withStatusTag(STATUS_TAG)
			.withTitle(TITLE)
			.withTypeTag(TYPE_TAG);
	}

	private static ErrandEntity createEntity() {
		return ErrandEntity.create()
			.withAssignedGroupId(ASSIGNED_GROUP_ID)
			.withAssignedUserId(ASSIGNED_USER_ID)
			.withCategoryTag(CATEGORY_TAG)
			.withClientIdTag(CLIENT_ID_TAG)
			.withCreated(CREATED)
			.withCustomer(EmbeddableCustomer.create().withId(CUSTOMER_ID).withType(CUSTOMER_TYPE))
			.withExternalTags(List.of(DbExternalTag.create().withKey(TAG_KEY).withValue(TAG_VALUE)))
			.withId(ID)
			.withPriority(PRIORITY)
			.withReporterUserId(REPORTER_USER_ID)
			.withStatusTag(STATUS_TAG)
			.withTitle(TITLE)
			.withTypeTag(TYPE_TAG)
			.withModified(MODIFIED);
	}
}
