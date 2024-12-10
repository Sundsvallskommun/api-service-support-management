package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.TimeMeasurementMapper.toTimeMeasurements;

import java.util.List;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.TimeMeasurement;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

@Service
public class TimeMeasurementService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";

	private final ErrandsRepository repository;

	public TimeMeasurementService(final ErrandsRepository repository) {
		this.repository = repository;
	}

	public List<TimeMeasurement> getErrandTimeMeasurements(final String namespace, final String municipalityId, final String errandId) {
		return repository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.map(errandEntity -> toTimeMeasurements(errandEntity.getTimeMeasures()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, errandId, namespace, municipalityId)));
	}

}
