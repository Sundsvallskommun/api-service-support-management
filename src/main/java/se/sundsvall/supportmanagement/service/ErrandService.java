package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;

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

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found for municipality with id '%s'";

	@Autowired
	private ErrandsRepository repository;

	public String createErrand(String municipalityId, Errand errand) {
		return repository.save(toErrandEntity(municipalityId, errand)).getId();
	}

	public Page<Errand> findErrands(String municipalityId, Specification<ErrandEntity> filter, Pageable pageable) {
		var matches = repository.findAll(withMunicipalityId(municipalityId).and(filter), pageable);
		return new PageImpl<>(toErrands(matches.getContent()), pageable, repository.count(filter));
	}


	public Errand readErrand(String municipalityId, String id) {
		verifyExistingErrand(id, municipalityId);
		return toErrand(repository.getReferenceById(id));
	}

	public Errand updateErrand(String municipalityId, String id, Errand errand) {
		verifyExistingErrand(id, municipalityId);
		var entity = updateEntity(repository.getReferenceById(id), errand);
		return toErrand(repository.save(entity));
	}

	public void deleteErrand(String municipalityId, String id) {
		verifyExistingErrand(id, municipalityId);
		repository.deleteById(id);
	}

	private void verifyExistingErrand(String id, String municipalityId) {
		if (!repository.existsByIdAndMunicipalityId(id, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, id, municipalityId));
		}
	}
}
