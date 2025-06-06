package se.sundsvall.supportmanagement.service.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertNull;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ERRAND;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.EventType;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.notification.Notification;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType;

class NotificationMapperTest {

	private static final String ID = "id";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String FIRST_NAME = "firstName";
	private static final String LAST_NAME = "lastName";
	private static final String OWNER_FULL_NAME = FIRST_NAME + " " + LAST_NAME;
	private static final String OWNER_ID = "ownerId";
	private static final String CREATED_BY = "createdBy";
	private static final String CREATED_BY_FULL_NAME = "createdByFullName";
	private static final String TYPE = "type";
	private static final NotificationSubType SUBTYPE = ERRAND;
	private static final String DESCRIPTION = "description";
	private static final String CONTENT = "content";
	private static final String ERRAND_ID = "errandId";
	private static final String ERRAND_NUMBER = "errandNumber";
	private static final boolean ACKNOWLEDGED = false;
	private static final boolean GLOBAL_ACKNOWLEDGED = false;
	private static final OffsetDateTime CREATED = now();
	private static final OffsetDateTime MODIFIED = now();
	private static final OffsetDateTime EXPIRES = now().plusDays(30);
	private static final String NEW_OWNER_FULL_NAME = "newOwnerFullName";
	private static final String NEW_OWNER_ID = "newOwnerId";
	private static final String NEW_CREATED_BY = "newCreatedBy";
	private static final String NEW_CREATED_BY_FULL_NAME = "newCreatedByFullName";
	private static final String NEW_TYPE = "newType";
	private static final String NEW_SUBTYPE = "newSubtype";
	private static final String NEW_DESCRIPTION = "newDescription";
	private static final String NEW_CONTENT = "newContent";
	private static final OffsetDateTime NEW_EXPIRES = now().plusDays(10);
	private static final boolean NEW_ACKNOWLEDGED = true;
	private static final boolean NEW_GLOBAL_ACKNOWLEDGED = true;
	private static final ErrandEntity ERRAND_ENTITY = ErrandEntity.create()
		.withId(ERRAND_ID)
		.withErrandNumber("ERRAND-NUMBER");

	private static final String NEW_ERRAND_NUMBER = "newErrandNumber";

	private static final EventType EVENT_TYPE = EventType.CREATE;

	private static Notification createNotification() {
		return Notification.create()
			.withOwnerFullName(OWNER_FULL_NAME)
			.withOwnerId(OWNER_ID)
			.withCreatedBy(CREATED_BY)
			.withCreatedByFullName(CREATED_BY_FULL_NAME)
			.withType(TYPE)
			.withSubtype(SUBTYPE.getValue())
			.withDescription(DESCRIPTION)
			.withContent(CONTENT)
			.withErrandId(ERRAND_ID)
			.withErrandNumber(ERRAND_NUMBER);
	}

	private static NotificationEntity createEntity() {
		return NotificationEntity.create()
			.withId(ID)
			.withAcknowledged(ACKNOWLEDGED)
			.withGlobalAcknowledged(GLOBAL_ACKNOWLEDGED)
			.withCreated(CREATED)
			.withModified(MODIFIED)
			.withExpires(EXPIRES)
			.withOwnerFullName(OWNER_FULL_NAME)
			.withOwnerId(OWNER_ID)
			.withCreatedBy(CREATED_BY)
			.withCreatedByFullName(CREATED_BY_FULL_NAME)
			.withType(TYPE)
			.withSubtype(SUBTYPE.getValue())
			.withDescription(DESCRIPTION)
			.withContent(CONTENT)
			.withErrandEntity(ERRAND_ENTITY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE);
	}

	@Test
	void testToNotification() {
		final var notification = NotificationMapper.toNotification(createEntity());

		assertThat(notification).isNotNull().hasNoNullFieldsOrPropertiesExcept("errandNumber");
		assertThat(notification.getOwnerFullName()).isEqualTo(OWNER_FULL_NAME);
		assertThat(notification.getOwnerId()).isEqualTo(OWNER_ID);
		assertThat(notification.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(notification.getCreatedByFullName()).isEqualTo(CREATED_BY_FULL_NAME);
		assertThat(notification.getType()).isEqualTo(TYPE);
		assertThat(notification.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(notification.getContent()).isEqualTo(CONTENT);
		assertThat(notification.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(notification.isAcknowledged()).isEqualTo(ACKNOWLEDGED);
		assertThat(notification.isGlobalAcknowledged()).isEqualTo(GLOBAL_ACKNOWLEDGED);
		assertThat(notification.getSubtype()).isEqualTo(SUBTYPE.getValue());
	}

	@Test
	void testToNotificationFromNull() {
		assertNull(NotificationMapper.toNotification(null));
	}

	@Test
	void testToNotificationEntity() {
		final var defaultNotificationTTLInDays = 30;
		final var entity = NotificationMapper.toNotificationEntity(NAMESPACE, MUNICIPALITY_ID, defaultNotificationTTLInDays, createNotification(), ERRAND_ENTITY);

		assertThat(entity).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "created", "modified", "expires", "ownerFullName", "createdByFullName");
		assertThat(entity.getOwnerFullName()).isNull();
		assertThat(entity.getOwnerId()).isEqualTo(OWNER_ID);
		assertThat(entity.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(entity.getCreatedByFullName()).isNull();
		assertThat(entity.getExpires()).isCloseTo(now().plusDays(defaultNotificationTTLInDays), within(2, SECONDS));
		assertThat(entity.getType()).isEqualTo(TYPE);
		assertThat(entity.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(entity.getContent()).isEqualTo(CONTENT);
		assertThat(entity.getErrandEntity().getId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entity.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(entity.isAcknowledged()).isEqualTo(ACKNOWLEDGED);
		assertThat(entity.isGlobalAcknowledged()).isEqualTo(GLOBAL_ACKNOWLEDGED);
	}

	@Test
	void updateEntity() {
		final var entity = createEntity();
		final var notification = createNotification();
		notification.setOwnerFullName(NEW_OWNER_FULL_NAME);
		notification.setOwnerId(NEW_OWNER_ID);
		notification.setCreatedBy(NEW_CREATED_BY);
		notification.setCreatedByFullName(NEW_CREATED_BY_FULL_NAME);
		notification.setType(NEW_TYPE);
		notification.setSubtype(NEW_SUBTYPE);
		notification.setDescription(NEW_DESCRIPTION);
		notification.setContent(NEW_CONTENT);
		notification.setExpires(NEW_EXPIRES);
		notification.setAcknowledged(NEW_ACKNOWLEDGED);
		notification.setGlobalAcknowledged(NEW_GLOBAL_ACKNOWLEDGED);
		notification.setErrandNumber(NEW_ERRAND_NUMBER);

		final var updatedEntity = NotificationMapper.updateEntity(entity, notification);

		assertThat(updatedEntity).isNotNull().hasNoNullFieldsOrPropertiesExcept();
		assertThat(updatedEntity.getId()).isEqualTo(ID);
		assertThat(updatedEntity.getCreated()).isEqualTo(CREATED);
		assertThat(updatedEntity.getModified()).isEqualTo(MODIFIED);
		assertThat(updatedEntity.getExpires()).isEqualTo(NEW_EXPIRES);
		assertThat(updatedEntity.getOwnerFullName()).isEqualTo(OWNER_FULL_NAME);
		assertThat(updatedEntity.getOwnerId()).isEqualTo(NEW_OWNER_ID);
		assertThat(updatedEntity.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(updatedEntity.getCreatedByFullName()).isEqualTo(CREATED_BY_FULL_NAME);
		assertThat(updatedEntity.getType()).isEqualTo(NEW_TYPE);
		assertThat(updatedEntity.getSubtype()).isEqualTo(NEW_SUBTYPE);
		assertThat(updatedEntity.getDescription()).isEqualTo(NEW_DESCRIPTION);
		assertThat(updatedEntity.getContent()).isEqualTo(NEW_CONTENT);
		assertThat(updatedEntity.getExpires()).isEqualTo(NEW_EXPIRES);
		assertThat(updatedEntity.isAcknowledged()).isEqualTo(NEW_ACKNOWLEDGED);
		assertThat(updatedEntity.isGlobalAcknowledged()).isEqualTo(NEW_GLOBAL_ACKNOWLEDGED);
	}

	@Test
	void toNotificationFromEvent() {

		// Arrange
		final var event = new Event()
			.type(EVENT_TYPE)
			.created(CREATED)
			.expires(EXPIRES)
			.message(DESCRIPTION);

		final var errandEntity = new ErrandEntity()
			.withId(ERRAND_ID)
			.withAssignedUserId(OWNER_ID)
			.withErrandNumber(ERRAND_NUMBER);

		// Act
		final var notification = NotificationMapper.toNotification(event, errandEntity, CREATED_BY, SUBTYPE);

		// Assert
		assertThat(notification).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "content", "expires", "ownerFullName", "createdByFullName");
		assertThat(notification.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(notification.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(notification.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(notification.getType()).isEqualTo(EVENT_TYPE.getValue());
		assertThat(notification.getExpires()).isEqualTo(EXPIRES);
		assertThat(notification.getCreated()).isEqualTo(CREATED);
		assertThat(notification.getOwnerId()).isEqualTo(OWNER_ID);
		assertThat(notification.getOwnerFullName()).isNull();
		assertThat(notification.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(notification.getCreatedByFullName()).isNull();
		assertThat(notification.getSubtype()).isEqualTo(SUBTYPE.getValue());
	}

	@Test
	void toNotificationFromEventOwnerAndCreatorIsNull() {

		// Arrange
		final var event = new Event()
			.type(EVENT_TYPE)
			.created(CREATED)
			.expires(EXPIRES)

			.message(DESCRIPTION);

		final var errandEntity = new ErrandEntity()
			.withId(ERRAND_ID)
			.withAssignedUserId(OWNER_ID)
			.withErrandNumber(ERRAND_NUMBER);

		// Act
		final var notification = NotificationMapper.toNotification(event, errandEntity, CREATED_BY, SUBTYPE);

		// Assert
		assertThat(notification).isNotNull().hasNoNullFieldsOrPropertiesExcept("id", "content", "expires", "ownerFullName", "createdByFullName");
		assertThat(notification.getDescription()).isEqualTo(DESCRIPTION);
		assertThat(notification.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(notification.getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(notification.getType()).isEqualTo(EVENT_TYPE.getValue());
		assertThat(notification.getExpires()).isEqualTo(EXPIRES);
		assertThat(notification.getCreated()).isEqualTo(CREATED);
		assertThat(notification.getOwnerId()).isEqualTo(OWNER_ID);
		assertThat(notification.getOwnerFullName()).isNull();
		assertThat(notification.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(notification.getCreatedByFullName()).isNull();
		assertThat(notification.getSubtype()).isEqualTo(SUBTYPE.getValue());
	}
}
