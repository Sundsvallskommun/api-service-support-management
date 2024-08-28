package se.sundsvall.supportmanagement.service;

import java.util.List;

import org.springframework.stereotype.Service;

import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
public class ErrandLabelService {

	private final ErrandsRepository errandsRepository;

	public ErrandLabelService(final ErrandsRepository errandsRepository) {this.errandsRepository = errandsRepository;}

	public List<String> getErrandLabels(final String namespace, final String municipalityId, final String id) {
		getErrandEntity(namespace, municipalityId, id);
		return List.of();
	}

	public void createErrandLabels(final String namespace, final String municipalityId, final String id, final List<String> labels) {
		final var errand = getErrandEntity(namespace, municipalityId, id);
		//errand.setLabels(labels);
		errandsRepository.save(errand);
	}


	public void deleteErrandLabel(final String namespace, final String municipalityId, final String id) {
		final var errand = getErrandEntity(namespace, municipalityId, id);
		//errand.setLabels(null);
		errandsRepository.save(errand);
	}

	public void updateErrandLabel(final String namespace, final String municipalityId, final String id, final List<String> labels) {
		final var errand = getErrandEntity(namespace, municipalityId, id);
		//errand.setLabels(labels);
		errandsRepository.save(errand);
	}


	private ErrandEntity getErrandEntity(final String namespace, final String municipalityId, final String id) {
		return errandsRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)
			.orElseThrow(() -> new RuntimeException("Errand not found"));
	}


}
