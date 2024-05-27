package se.sundsvall.supportmanagement.service.scheduler.supensions;


import static java.time.OffsetDateTime.now;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.Suspension;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
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
					.withStatus(entity.getPreviousStatus());
				errandService.updateErrand(entity.getNamespace(), entity.getMunicipalityId(), entity.getId(), errand);
			});
	}

}
