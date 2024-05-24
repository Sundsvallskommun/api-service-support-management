package se.sundsvall.supportmanagement.integration.db.model.listener;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;

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
		assertThat(entity.getPreviousStatus()).isEqualTo(status);
	}

	@Test
	void onCreate() {

		// Arrange
		final var status = "status";
		final var entity = new ErrandEntity()
			.withStakeholders(List.of(StakeholderEntity.create())
			).withStatus(status);
		// Act
		errandListener.onCreate(entity);

		// Assert
		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getStakeholders().getFirst().getErrandEntity()).isSameAs(entity);
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "stakeholders", "timeMeasures", "status", "previousStatus");

		assertThat(entity.getTimeMeasures()).isNotEmpty().hasSize(1);
		assertThat(entity.getTimeMeasures().getFirst()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStartTime()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStatus()).isEqualTo(status);
	}

	@Test
	void onUpdate() {

		// Arrange
		final var status = "status";
		final var previousStatus = "previousStatus";
		final var entity = new ErrandEntity()
			.withStakeholders(List.of(StakeholderEntity.create()))
			.withStatus(status)
			.withPreviousStatus(previousStatus);

		errandListener.onUpdate(entity);

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getStakeholders().getFirst().getErrandEntity()).isSameAs(entity);
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified", "stakeholders", "status", "previousStatus", "timeMeasures");

		assertThat(entity.getTimeMeasures()).isNotEmpty().hasSize(2);
		assertThat(entity.getTimeMeasures().getFirst()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStartTime()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getTimeMeasures().getFirst().getStopTime()).isCloseTo(now(), within(1, SECONDS));

		assertThat(entity.getTimeMeasures().getFirst().getStatus()).isEqualTo(status);
		assertThat(entity.getTimeMeasures().getLast()).isNotNull();
		assertThat(entity.getTimeMeasures().getLast().getStartTime()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity.getTimeMeasures().getLast().getStopTime()).isNull();
		assertThat(entity.getTimeMeasures().getLast().getStatus()).isEqualTo(status);

	}

	@Test
	void onDelete() {

		// Arrange
		final var status = "status";

		final var entity = new ErrandEntity()
			.withStatus(status);
		errandListener.onDelete(entity);

		// Assert
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("status", "timeMeasures");
		assertThat(entity.getTimeMeasures()).isNotEmpty().hasSize(1);
		assertThat(entity.getTimeMeasures().getFirst()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStartTime()).isNotNull();
		assertThat(entity.getTimeMeasures().getFirst().getStatus()).isEqualTo(status);

	}

}
