package se.sundsvall.supportmanagement.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
public class ErrandLabelService {

	private final ErrandsRepository errandsRepository;

	public ErrandLabelService(final ErrandsRepository errandsRepository) {this.errandsRepository = errandsRepository;}

	public List<String> getErrandLabels(final String namespace, final String municipalityId, final String id) {
		final var errand = getErrandEntity(namespace, municipalityId, id);
		return errand.getLabels();
	}

	public void createErrandLabels(final String namespace, final String municipalityId, final String id, final List<String> labels) {
		errandsRepository.save(getErrandEntity(namespace, municipalityId, id).withLabels(labels));
	}

	public void deleteErrandLabel(final String namespace, final String municipalityId, final String id) {
		final var errand = getErrandEntity(namespace, municipalityId, id);
		errand.setLabels(null);
		errandsRepository.save(errand);
	}

	public void updateErrandLabel(final String namespace, final String municipalityId, final String id, final List<String> labels) {
		final var errand = getErrandEntity(namespace, municipalityId, id);
		errand.setLabels(labels);
		errandsRepository.save(errand);
	}

	private ErrandEntity getErrandEntity(final String namespace, final String municipalityId, final String id) {
		return errandsRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND, "Could not find errand with id " + id));
	}

}
