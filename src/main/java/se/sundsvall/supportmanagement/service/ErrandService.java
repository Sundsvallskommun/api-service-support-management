package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;

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

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found";

	@Autowired
	private ErrandsRepository repository;

	public String createErrand(final Errand errand) {
		return repository.save(toErrandEntity(errand)).getId();
	}

	public Page<Errand> findErrands(final Specification<ErrandEntity> filter, final Pageable pageable) {
		final var matches = repository.findAll(filter, pageable);
		return new PageImpl<>(toErrands(matches.getContent()), pageable, repository.count(filter));
	}

	public Errand readErrand(final String id) {
		verifyExistingId(id);
		return toErrand(repository.getReferenceById(id));
	}

	public Errand updateErrand(final String id, final Errand errand) {
		verifyExistingId(id);
		final var entity = updateEntity(repository.getReferenceById(id), errand);
		return toErrand(repository.save(entity));
	}

	public void deleteErrand(final String id) {
		verifyExistingId(id);
		repository.deleteById(id);
	}

	private void verifyExistingId(final String id) {
		if (!repository.existsById(id)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, id));
		}
	}
}
