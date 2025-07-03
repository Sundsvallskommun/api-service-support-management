package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Objects.nonNull;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
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

	public String createErrand(final String namespace, final String municipalityId, final Errand errand) {
		// Generate unique errand number
		errand.withErrandNumber(errandNumberGeneratorService.generateErrandNumber(namespace, municipalityId));

		final var errandEntity = toErrandEntity(namespace, municipalityId, errand);
		// Validate ContactReason
		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			final var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));

			errandEntity
				.withContactReason(contactReason)
				.withContactReasonDescription(errand.getContactReasonDescription());
		});

		final var persistedEntity = repository.save(errandEntity);
		final var revision = revisionService.createErrandRevision(persistedEntity);

		// Create log event, but don't create a notification.
		eventService.createErrandEvent(CREATE, EVENT_LOG_CREATE_ERRAND, persistedEntity, revision.latest(), null, false, ERRAND);

		return persistedEntity.getId();
	}

	public Page<Errand> findErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter, final Pageable pageable) {
		final var fullFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(filter);
		final var matches = repository.findAll(fullFilter, pageable);

		return new PageImpl<>(toErrands(matches.getContent()), pageable, matches.getTotalElements());
	}

	public Errand readErrand(final String namespace, final String municipalityId, final String id) {
		verifyExistingErrand(id, namespace, municipalityId, false);
		return toErrand(repository.getReferenceById(id));
	}

	public Errand updateErrand(final String namespace, final String municipalityId, final String id, final Errand errand) {
		verifyExistingErrand(id, namespace, municipalityId, true);

		final var errandEntity = updateEntity(repository.getReferenceById(id), errand);
		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			final var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));

			errandEntity.withContactReason(contactReason);
		});

		final var entity = repository.save(errandEntity);

		final var revisionResult = revisionService.createErrandRevision(entity);

		// Create log event if the update has modified the errand (and thus has created a new revision)
		if (nonNull(revisionResult)) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, revisionResult.latest(), revisionResult.previous(), ERRAND);
		}

		return toErrand(entity);
	}

	public void deleteErrand(final String namespace, final String municipalityId, final String id) {
		verifyExistingErrand(id, namespace, municipalityId, true);

		final var entity = repository.getReferenceById(id);
		// Delete errand
		repository.deleteById(id);

		// Create log event
		final var latestRevision = revisionService.getLatestErrandRevision(namespace, municipalityId, id);
		eventService.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, latestRevision, null, false, ERRAND);
	}

	private void verifyExistingErrand(final String id, final String namespace, final String municipalityId, final boolean lock) {

		final Supplier<Boolean> exists;
		if (lock) {
			exists = () -> repository.existsWithLockingByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		} else {
			exists = () -> repository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		}

		if (Boolean.FALSE.equals(exists.get())) {
			throw Problem.valueOf(NOT_FOUND, ENTITY_NOT_FOUND.formatted(id, namespace, municipalityId));
		}
	}

	public Long countErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter) {
		final var fullFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(filter);
		return repository.count(fullFilter);
	}
}
