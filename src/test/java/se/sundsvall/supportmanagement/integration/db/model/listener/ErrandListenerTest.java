package se.sundsvall.supportmanagement.integration.db.model.listener;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasurementEntity;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class ErrandListenerTest {

	@InjectMocks
	private ErrandListener errandListener;

	@Test
	void onLoad() {

		// Arrange
		final var status = "status";
		final var entity = new ErrandEntity().withStatus(status);
		// Act
		errandListener.onLoad(entity);

		// Assert
		assertThat(entity.getTempPreviousStatus()).isEqualTo(status);
	}

	@Test
	void onCreate() {

		// Arrange
		final var status = "status";
		final var loginName = "loginName";
		final var entity = new ErrandEntity()
			.withAssignedUserId(loginName)
			.withStakeholders(List.of(StakeholderEntity.create()))
			.withStatus(status);

		// Act
		errandListener.onCreate(entity);

		// Assert
		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getTouched()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getStakeholders().getFirst().getErrandEntity()).isSameAs(entity);
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "touched", "stakeholders", "timeMeasures", "status", "previousStatus", "assignedUserId");
		assertThat(entity.getTimeMeasures()).isNotEmpty().hasSize(1);
		assertThat(entity.getTimeMeasures().getFirst()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStartTime()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStatus()).isEqualTo(status);
		assertThat(entity.getTimeMeasures().getFirst().getAdministrator()).isEqualTo(loginName);
	}

	@Test
	void onUpdate() {

		// Arrange
		final var status = "status";
		final var loginName = "loginName";
		final var previousStatus = "previousStatus";
		final var entity = new ErrandEntity()
			.withAssignedUserId(loginName)
			.withStakeholders(List.of(StakeholderEntity.create()))
			.withStatus(status)
			.withPreviousStatus(previousStatus);

		// Act
		errandListener.onUpdate(entity);

		// Assert
		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getTouched()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getStakeholders().getFirst().getErrandEntity()).isSameAs(entity);
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "touched", "stakeholders", "status", "previousStatus", "timeMeasures", "assignedUserId");
		assertThat(entity.getTimeMeasures()).isNotEmpty().hasSize(1);
		assertThat(entity.getTimeMeasures().getFirst()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStartTime()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getTimeMeasures().getFirst().getStopTime()).isNull();
		assertThat(entity.getTimeMeasures().getFirst().getAdministrator()).isEqualTo(loginName);
		assertThat(entity.getTimeMeasures().getFirst().getStatus()).isEqualTo(status);
	}

	@Test
	void onDelete() {

		// Arrange
		final var status = "status";
		final var loginName = "loginName";
		final var entity = new ErrandEntity()
			.withAssignedUserId(loginName)
			.withStakeholders(List.of(StakeholderEntity.create()))
			.withTimeMeasures(List.of(TimeMeasurementEntity.create().withStartTime(now()).withAdministrator(loginName).withStatus(status)))
			.withStatus(status);
		// Act
		errandListener.onDelete(entity);

		// Assert
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("stakeholders", "status", "timeMeasures", "assignedUserId");
		assertThat(entity.getTimeMeasures()).isNotEmpty().hasSize(1);
		assertThat(entity.getTimeMeasures().getFirst()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStartTime()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStopTime()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getTimeMeasures().getFirst().getStatus()).isEqualTo(status);
		assertThat(entity.getTimeMeasures().getFirst().getAdministrator()).isEqualTo(loginName);

	}

}
