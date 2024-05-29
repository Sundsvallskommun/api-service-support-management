package se.sundsvall.supportmanagement.integration.db.model.listener;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasurementEntity;

@Component
public class ErrandListener {

	@PostLoad
	void onLoad(final ErrandEntity errandEntity) {
		errandEntity.setTempPreviousStatus(errandEntity.getStatus());
	}

	@PrePersist
	void onCreate(final ErrandEntity errandEntity) {
		errandEntity.setCreated(now(systemDefault()).truncatedTo(MILLIS));
		Optional.ofNullable(errandEntity.getStakeholders())
			.ifPresent(st -> st.forEach(s -> s.setErrandEntity(errandEntity)));

		Optional.ofNullable(errandEntity.getTimeMeasures())
			.ifPresentOrElse(
				list -> list.add(startTimeEntry(errandEntity, now())),
				() -> errandEntity.setTimeMeasures(List.of(startTimeEntry(errandEntity, now()))));
	}

	@PreUpdate
	void onUpdate(final ErrandEntity errandEntity) {
		errandEntity.setModified(now(systemDefault()).truncatedTo(MILLIS));
		Optional.ofNullable(errandEntity.getStakeholders())
			.ifPresent(st -> st.forEach(s -> s.setErrandEntity(errandEntity)));

		// Status Changed
		// The second statement is to prevent the same status from being added multiple times as preUpdate is called multiple times during the same transaction
		if (!errandEntity.getStatus().equals(errandEntity.getTempPreviousStatus()) && !errandEntity.getPreviousStatus().equals(errandEntity.getTempPreviousStatus())) {
			final var now = now();
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
			.withDescription(errandEntity.getDescription())
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
