package se.sundsvall.supportmanagement.integration.db;

import static java.time.OffsetDateTime.now;
import static java.time.OffsetDateTime.of;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.NotificationEntity;

@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-junit.sql"
})
@Transactional
class NotificationRepositoryTest {

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void createNotification() {

		// Arrange
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var timestamp = of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC); // fixed timestamp
		final var owner = "Test Testorsson";
		final var ownerId = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
		final var createdBy = "TestUser";
		final var type = "SomeType";
		final var description = "Some description of the notification";
		final var content = "Some content of the notification";
		final var acknowledged = true;
		final var errandEntity = ErrandEntity.create()
			.withId("ERRAND_ID-1")
			.withErrandNumber("KC-23020001");
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";

		final var notification = NotificationEntity.create()
			.withId(id)
			.withCreated(timestamp)
			.withModified(timestamp)
			.withOwnerFullName(owner)
			.withOwnerId(ownerId)
			.withCreatedBy(createdBy)
			.withType(type)
			.withDescription(description)
			.withContent(content)
			.withExpires(timestamp)
			.withAcknowledged(acknowledged)
			.withErrandEntity(errandEntity)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);

		// Act
		final var result = notificationRepository.save(notification);

		// Assert
		assertThat(result).isNotNull();
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId() {

		// Act
		final var notification = notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId("1", "namespace-1", "municipalityId-1", "ERRAND_ID-1");

		// Assert
		assertThat(notification).isPresent();
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdAndErrandEntityId() {

		// Act
		final var result = notificationRepository.findAllByNamespaceAndMunicipalityIdAndErrandEntityId("namespace-1", "municipalityId-1", "ERRAND_ID-1", Sort.by("modified").descending());

		// Assert
		assertThat(result).hasSize(1);
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdAndOwnerId() {

		// Act
		final var notifications = notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId("namespace-1", "municipalityId-1", "owner_id-1");

		// Assert
		assertThat(notifications).hasSize(1);
	}

	@Test
	void findByNamespaceAndMunicipalityIdAndOwnerIdAndAcknowledgedAndErrandEntityIdAndType() {

		// Act
		final var notification = notificationRepository.findByNamespaceAndMunicipalityIdAndOwnerIdAndAcknowledgedAndErrandEntityIdAndType("namespace-1", "municipalityId-1", "owner_id-1", false, "ERRAND_ID-1", "type-1");

		// Assert
		assertThat(notification).isPresent();
	}

	@Test
	void findByExpiresBefore() {

		// Arrange
		final var timestamp = of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// Act
		final var notifications = notificationRepository.findByExpiresBefore(timestamp);

		// Assert
		assertThat(notifications).hasSize(1);
	}

	@Test
	void updateNotification() {

		// Arrange
		final var notification = notificationRepository.findById("1").orElseThrow();
		notification.setDescription("Updated description");

		// Act
		notificationRepository.save(notification);

		// Assert
		final var updatedNotification = notificationRepository.findById("1").orElseThrow();
		assertThat(updatedNotification.getDescription()).isEqualTo("Updated description");
	}

	@Test
	void existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId() {

		// Act
		final boolean exists = notificationRepository.existsByIdAndNamespaceAndMunicipalityIdAndErrandEntityId("1", "namespace-1", "municipalityId-1", "ERRAND_ID-1");

		// Assert
		assertThat(exists).isTrue();
	}

	@Test
	void existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescription() {

		// Arrange
		final var errandEntity = ErrandEntity.create()
			.withId("ERRAND_ID-1")
			.withErrandNumber("KC-23020001");

		// Act
		final boolean exists = notificationRepository.existsByNamespaceAndMunicipalityIdAndOwnerIdAndErrandEntityAndDescriptionAndCreatedIsAfter("namespace-1", "municipalityId-1", "owner_id-1", errandEntity, "description-1", now().minusHours(6));

		// Assert
		assertThat(exists).isTrue();
	}

	@Test
	void deleteNotification() {

		// Arrange
		final var notification = notificationRepository.findById("1").orElseThrow();

		// Act
		notificationRepository.delete(notification);

		// Assert
		assertThat(notificationRepository.findById("1")).isNotPresent();
	}
}
