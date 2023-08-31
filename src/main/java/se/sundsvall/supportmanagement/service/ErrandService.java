package se.sundsvall.supportmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Objects.nonNull;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.distinct;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

@Service
@Transactional
public class ErrandService {
	private static final Logger LOG = LoggerFactory.getLogger(ErrandService.class);

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";

	@Autowired
	private ErrandsRepository repository;

	@Autowired
	private RevisionService revisionService;

	@Autowired
	private EventService eventService;

	public String createErrand(String namespace, String municipalityId, Errand errand) {
		// Create new errand and revision
		final var entity = repository.save(toErrandEntity(namespace, municipalityId, errand));
		final var revision = revisionService.createErrandRevision(entity);

		// Create log event
		eventService.createErrandEvent(CREATE, EVENT_LOG_CREATE_ERRAND, entity, revision.latest(), null);

		return entity.getId();
	}

	public Page<Errand> findErrands(String namespace, String municipalityId, Specification<ErrandEntity> filter, Pageable pageable) {
		final var fullFilter = distinct().and(withNamespace(namespace)).and(withMunicipalityId(municipalityId)).and(filter);
		final var matches = repository.findAll(fullFilter, pageable);

		return new PageImpl<>(toErrands(matches.getContent()), pageable, repository.count(fullFilter));
	}

	public Errand readErrand(String namespace, String municipalityId, String id) {
		verifyExistingErrand(id, namespace, municipalityId);

		return toErrand(repository.getReferenceById(id));
	}

	public Errand updateErrand(String namespace, String municipalityId, String id, Errand errand) {
		verifyExistingErrand(id, namespace, municipalityId);

		// Update errand and create new revision
		final var entity = repository.save(updateEntity(repository.getReferenceById(id), errand));
		final var revisionResult = revisionService.createErrandRevision(entity);

		// Create log event if the update has modified the errand (and thus has created a new revision)
		if (nonNull(revisionResult)) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, revisionResult.latest(), revisionResult.previous());
		}

		return toErrand(entity);
	}

	public void deleteErrand(String namespace, String municipalityId, String id) {
		verifyExistingErrand(id, namespace, municipalityId);

		final var entity = repository.getReferenceById(id);

		// Delete errand
		repository.deleteById(id);

		// Create log event
		final var latestRevision = revisionService.getLatestErrandRevision(id);
		eventService.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, latestRevision, null);
	}

	private void verifyExistingErrand(String id, String namespace, String municipalityId) {
		if (!repository.existsWithLockingByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
	}
}
