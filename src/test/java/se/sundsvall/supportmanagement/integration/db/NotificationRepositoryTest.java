package se.sundsvall.supportmanagement.integration.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

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
	void createNotificationTest() {

		// Arrange
		final var id = "123e4567-e89b-12d3-a456-426614174000";
		final var timestamp = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC); // fixed timestamp
		final var owner = "Test Testorsson";
		final var ownerId = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
		final var createdBy = "TestUser";
		final var type = "SomeType";
		final var description = "Some description of the notification";
		final var content = "Some content of the notification";
		final var acknowledged = true;
		final var errandId = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
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
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace);

		// Act
		final var result = notificationRepository.save(notification);

		// Assert
		assertThat(result).isNotNull();
	}

	@Test
	void updateNotificationTest() {

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
	void existsByIdAndNamespaceAndMunicipalityIdTest() {

		// Act
		final boolean exists = notificationRepository.existsByIdAndNamespaceAndMunicipalityId("1", "namespace-1", "municipalityId-1");

		// Assert
		assertThat(exists).isTrue();
	}

	@Test
	void deleteNotificationTest() {

		// Arrange
		final var notification = notificationRepository.findById("1").orElseThrow();

		// Act
		notificationRepository.delete(notification);

		// Assert
		assertThat(notificationRepository.findById("1")).isNotPresent();
	}

}
