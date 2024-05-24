package se.sundsvall.supportmanagement.service.scheduler.supensions;


import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.Suspension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TimeMeasureEntity;
import se.sundsvall.supportmanagement.service.ErrandService;

@Component
public class SuspensionWorker {

	private final ErrandsRepository errandsRepository;

	private final ErrandService errandService;

	public SuspensionWorker(final ErrandsRepository errandsRepository, final ErrandService errandService) {
		this.errandsRepository = errandsRepository;
		this.errandService = errandService;
	}

	@Transactional
	public void cleanUpSuspensions() {

		errandsRepository
			.findAllBySuspendedToBefore(now())
			.forEach(entity -> {
				final var errand = Errand.create()
					.withSuspension(new Suspension())
					.withStatus(getPreviousStatus(entity));
				errandService.updateErrand(entity.getNamespace(), entity.getMunicipalityId(), entity.getId(), errand);
			});
	}

	// This method is used to get the previous status of an errand
	// The current status is the status of the time measure that is currently running, the one that does not have a stop time
	// The previous status is the status of the time measure that was stopped at the same time as the current time measure was started
	public String getPreviousStatus(final ErrandEntity entity) {
		return Optional.ofNullable(entity.getTimeMeasures())
			.orElse(emptyList()).stream()
			.filter(measure -> measure.getStopTime() == null)
			.findFirst()
			.flatMap(current -> entity.getTimeMeasures().stream()
				.filter(measure -> current.getStartTime().equals(measure.getStopTime()))
				.findFirst())
			.map(TimeMeasureEntity::getStatus)
			.orElse(null);
	}

}
