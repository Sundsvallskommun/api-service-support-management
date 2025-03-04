package se.sundsvall.supportmanagement.integration.db.model.listener;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasurementEntity;

public class ErrandListener {

	@PostLoad
	void onLoad(final ErrandEntity errandEntity) {
		errandEntity.setTempPreviousStatus(errandEntity.getStatus());
	}

	@PrePersist
	void onCreate(final ErrandEntity errandEntity) {
		final var now = now(systemDefault()).truncatedTo(MILLIS);
		errandEntity.setCreated(now);
		errandEntity.setTouched(now);
		Optional.ofNullable(errandEntity.getStakeholders())
			.ifPresent(st -> st.forEach(s -> s.setErrandEntity(errandEntity)));

		Optional.ofNullable(errandEntity.getTimeMeasures())
			.ifPresentOrElse(
				list -> list.add(startTimeEntry(errandEntity, now())),
				() -> errandEntity.setTimeMeasures(List.of(startTimeEntry(errandEntity, now()))));
	}

	@PreUpdate
	void onUpdate(final ErrandEntity errandEntity) {
		final var now = now(systemDefault()).truncatedTo(MILLIS);
		errandEntity.setModified(now);
		errandEntity.setTouched(now);
		Optional.ofNullable(errandEntity.getStakeholders())
			.ifPresent(st -> st.forEach(s -> s.setErrandEntity(errandEntity)));

		// Status Changed
		// The second statement is to prevent the same status from being added multiple times as preUpdate is called multiple
		// times during the same transaction
		if (!errandEntity.getStatus().equals(errandEntity.getTempPreviousStatus()) && !Objects.equals(errandEntity.getTempPreviousStatus(), errandEntity.getPreviousStatus())) {
			Optional.ofNullable(errandEntity.getTimeMeasures())
				.ifPresentOrElse(list -> {
					findTimeMeasureEntityWithoutStopTime(errandEntity, now);
					list.add(startTimeEntry(errandEntity, now));
				}, () -> errandEntity.setTimeMeasures(List.of((startTimeEntry(errandEntity, now)))));
			errandEntity.setPreviousStatus(errandEntity.getTempPreviousStatus());
		}
	}

	@PreRemove
	void onDelete(final ErrandEntity errandEntity) {
		Optional.ofNullable(errandEntity.getTimeMeasures())
			.ifPresent(tm -> findTimeMeasureEntityWithoutStopTime(errandEntity, now()));
	}

	private TimeMeasurementEntity startTimeEntry(final ErrandEntity errandEntity, final OffsetDateTime now) {
		return new TimeMeasurementEntity()
			.withAdministrator(errandEntity.getAssignedUserId())
			.withStatus(errandEntity.getStatus())
			.withStartTime(now);
	}

	private void findTimeMeasureEntityWithoutStopTime(final ErrandEntity errandEntity, final OffsetDateTime now) {
		errandEntity.getTimeMeasures()
			.stream()
			.filter(tm -> tm.getStopTime() == null)
			.findFirst()
			.ifPresent(tm -> tm.setStopTime(now));
	}
}
