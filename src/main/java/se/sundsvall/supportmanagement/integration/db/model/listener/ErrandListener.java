package se.sundsvall.supportmanagement.integration.db.model.listener;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasureEntity;

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

		final var list = getTimeMeasures(errandEntity);
		list.add(startTimeEntry(errandEntity, now()));
		errandEntity.setTimeMeasures(list);

	}

	@PreUpdate
	void onUpdate(final ErrandEntity errandEntity) {
		errandEntity.setModified(now(systemDefault()).truncatedTo(MILLIS));
		Optional.ofNullable(errandEntity.getStakeholders())
			.ifPresent(st -> st.forEach(s -> s.setErrandEntity(errandEntity)));

		// Status Changed
		if (!errandEntity.getStatus().equals(errandEntity.getTempPreviousStatus())) {
			final var list = getTimeMeasures(errandEntity);
			final var now = now();
			list.add(stopTimeEntry(findTimeMeasureEntityWithoutStopTime(errandEntity), now));
			list.add(startTimeEntry(errandEntity, now));
			errandEntity.setTimeMeasures(list);
			errandEntity.setPreviousStatus(errandEntity.getTempPreviousStatus());
		}
	}

	@PreRemove
	void onDelete(final ErrandEntity errandEntity) {
		final var list = getTimeMeasures(errandEntity);
		list.add(stopTimeEntry(findTimeMeasureEntityWithoutStopTime(errandEntity), now()));
		errandEntity.setTimeMeasures(list);

	}

	private TimeMeasureEntity startTimeEntry(final ErrandEntity errandEntity, final OffsetDateTime now) {
		return new TimeMeasureEntity()
			.withStatus(errandEntity.getStatus())
			.withStartTime(now);
	}

	private TimeMeasureEntity stopTimeEntry(final TimeMeasureEntity timeMeasureEntity, final OffsetDateTime now) {
		return timeMeasureEntity.withStopTime(now);
	}

	private TimeMeasureEntity findTimeMeasureEntityWithoutStopTime(final ErrandEntity errandEntity) {
		return getTimeMeasures(errandEntity).stream()
			.filter(tm -> tm.getStatus().equals(errandEntity.getStatus()))
			.filter(tm -> tm.getStopTime() == null)
			.findFirst()
			.orElse(new TimeMeasureEntity()
				.withStatus(errandEntity.getStatus())
				.withStartTime(now()));
	}

	private List<TimeMeasureEntity> getTimeMeasures(final ErrandEntity errandEntity) {
		return Optional.ofNullable(errandEntity.getTimeMeasures()).orElseGet(ArrayList::new);
	}

}