package se.sundsvall.supportmanagement.integration.db.model.listener;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

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
		errandEntity.setPreviousStatus(errandEntity.getStatus());
	}

	@PrePersist
	void onCreate(final ErrandEntity errandEntity) {
		errandEntity.setCreated(now(systemDefault()).truncatedTo(MILLIS));
		Optional.ofNullable(errandEntity.getStakeholders())
			.ifPresent(st -> st.forEach(s -> s.setErrandEntity(errandEntity)));

		final var list = getTimeMeasures(errandEntity);
		list.add(startTimeEntry(errandEntity));
		errandEntity.setTimeMeasures(list);

	}

	@PreUpdate
	void onUpdate(final ErrandEntity errandEntity) {
		errandEntity.setModified(now(systemDefault()).truncatedTo(MILLIS));
		Optional.ofNullable(errandEntity.getStakeholders())
			.ifPresent(st -> st.forEach(s -> s.setErrandEntity(errandEntity)));

		// Status Changed
		if (!errandEntity.getStatus().equals(errandEntity.getPreviousStatus())) {
			final var list = getTimeMeasures(errandEntity);

			list.add(stopTimeEntry(findTimeMeasureEntityWithoutStopTime(errandEntity)));
			list.add(startTimeEntry(errandEntity));
			errandEntity.setTimeMeasures(list);

		}
	}

	@PreRemove
	void onDelete(final ErrandEntity errandEntity) {
		final var list = getTimeMeasures(errandEntity);
		list.add(stopTimeEntry(findTimeMeasureEntityWithoutStopTime(errandEntity)));
		errandEntity.setTimeMeasures(list);

	}

	private TimeMeasureEntity startTimeEntry(final ErrandEntity errandEntity) {
		return new TimeMeasureEntity()
			.withStatus(errandEntity.getStatus())
			.withStartTime(now());
	}

	private TimeMeasureEntity stopTimeEntry(final TimeMeasureEntity timeMeasureEntity) {
		return timeMeasureEntity.withStopTime(now());
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
