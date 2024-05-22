package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Objects.nonNull;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.distinct;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;

@Service
@Transactional
public class ErrandService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String BAD_CONTACT_REASON = "'%s' is not a valid contact reason for namespace '%s' and municipality with id '%s'";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";

	private final ErrandsRepository repository;
	private final ContactReasonRepository contactReasonRepository;

	private final RevisionService revisionService;
	private final EventService eventService;
	private final ErrandNumberGeneratorService errandNumberGeneratorService;

	public ErrandService(final ErrandsRepository repository,
		final ContactReasonRepository contactReasonRepository,
		final RevisionService revisionService, final EventService eventService,
		final ErrandNumberGeneratorService errandNumberGeneratorService) {
		this.repository = repository;
		this.contactReasonRepository = contactReasonRepository;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.errandNumberGeneratorService = errandNumberGeneratorService;
	}

	public String createErrand(String namespace, String municipalityId, Errand errand) {
		// Generate unique errand number
		errand.withErrandNumber(errandNumberGeneratorService.generateErrandNumber(namespace, municipalityId));


		final var errandEntity = repository.save(toErrandEntity(namespace, municipalityId, errand));
		//Validate ContactReason
		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));
			errandEntity.setContactReason(contactReason);
			repository.save(errandEntity);
		});


		final var revision = revisionService.createErrandRevision(errandEntity);

		// Create log event
		eventService.createErrandEvent(CREATE, EVENT_LOG_CREATE_ERRAND, errandEntity, revision.latest(), null);

		return errandEntity.getId();
	}

	public Page<Errand> findErrands(String namespace, String municipalityId, Specification<ErrandEntity> filter, Pageable pageable) {
		final var fullFilter = distinct().and(withNamespace(namespace)).and(withMunicipalityId(municipalityId)).and(filter);
		final var matches = repository.findAll(fullFilter, pageable);

		return new PageImpl<>(toErrands(matches.getContent()), pageable, repository.count(fullFilter));
	}

	public Errand readErrand(String namespace, String municipalityId, String id) {
		verifyExistingErrand(id, namespace, municipalityId, false);

		return toErrand(repository.getReferenceById(id));
	}

	public Errand updateErrand(String namespace, String municipalityId, String id, Errand errand) {
		verifyExistingErrand(id, namespace, municipalityId, true);


		final var errandEntity = updateEntity(repository.getReferenceById(id), errand);
		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));
			errandEntity.setContactReason(contactReason);
		});

		final var entity = repository.save(errandEntity);

		final var revisionResult = revisionService.createErrandRevision(entity);

		// Create log event if the update has modified the errand (and thus has created a new revision)
		if (nonNull(revisionResult)) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, revisionResult.latest(), revisionResult.previous());
		}

		return toErrand(entity);
	}

	public void deleteErrand(String namespace, String municipalityId, String id) {
		verifyExistingErrand(id, namespace, municipalityId, true);

		final var entity = repository.getReferenceById(id);

		// Delete errand
		repository.deleteById(id);

		// Create log event
		final var latestRevision = revisionService.getLatestErrandRevision(id);
		eventService.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, latestRevision, null);
	}

	private void verifyExistingErrand(String id, String namespace, String municipalityId, boolean lock) {

		Supplier<Boolean> exists;
		if (lock) {
			exists = () -> repository.existsWithLockingByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		} else {
			exists = () -> repository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		}

		if (Boolean.FALSE.equals(exists.get())) {
			throw Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
	}
}
