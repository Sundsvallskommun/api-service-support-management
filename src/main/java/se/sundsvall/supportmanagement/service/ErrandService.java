package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
public class ErrandService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";

	@Autowired
	private ErrandsRepository repository;

	public String createErrand(String namespace, String municipalityId, Errand errand) {
		return repository.save(toErrandEntity(namespace, municipalityId, errand)).getId();
	}

	public Page<Errand> findErrands(String namespace, String municipalityId, Specification<ErrandEntity> filter, Pageable pageable) {
		final var fullFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(filter);
		final var matches = repository.findAll(fullFilter, pageable);
		return new PageImpl<>(toErrands(matches.getContent()), pageable, repository.count(fullFilter));
	}

	public Errand readErrand(String namespace, String municipalityId, String id) {
		verifyExistingErrand(id, namespace, municipalityId);
		return toErrand(repository.getReferenceById(id));
	}

	public Errand updateErrand(String namespace, String municipalityId, String id, Errand errand) {
		verifyExistingErrand(id, namespace, municipalityId);
		final var entity = updateEntity(repository.getReferenceById(id), errand);
		return toErrand(repository.save(entity));
	}

	public void deleteErrand(String namespace, String municipalityId, String id) {
		verifyExistingErrand(id, namespace, municipalityId);
		repository.deleteById(id);
	}

	private void verifyExistingErrand(String id, String namespace, String municipalityId) {
		if (!repository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
	}
}
