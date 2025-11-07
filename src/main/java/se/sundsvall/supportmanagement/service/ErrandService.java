package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static org.zalando.problem.Status.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrands;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import generated.se.sundsvall.accessmapper.Access;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;

@Service
@Transactional
public class ErrandService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ENTITY_NOT_ACCESSIBLE = "Errand not accessible by user '%s'";
	private static final String LABELS_NOT_FOUND = "The provided label-ID:s '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String BAD_CONTACT_REASON = "'%s' is not a valid contact reason for namespace '%s' and municipality with id '%s'";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";

	private final ErrandsRepository repository;
	private final ContactReasonRepository contactReasonRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final ErrandNumberGeneratorService errandNumberGeneratorService;
	private final ErrandAttachmentService errandAttachmentService;
	private final CommunicationService communicationService;
	private final AttachmentRepository attachmentRepository;
	private final ConversationService conversationService;
	private final NotesClient notesClient;
	private final AccessControlService accessControlService;

	public ErrandService(
		final ErrandsRepository repository,
		final ContactReasonRepository contactReasonRepository,
		final CommunicationService communicationService,
		final AttachmentRepository attachmentRepository,
		final RevisionService revisionService,
		final EventService eventService,
		final ErrandNumberGeneratorService errandNumberGeneratorService,
		final ErrandAttachmentService errandAttachmentService,
		final ConversationService conversationService,
		final NotesClient notesClient,
		final MetadataLabelRepository metadataLabelRepository,
		final AccessControlService accessControlService) {

		this.repository = repository;
		this.contactReasonRepository = contactReasonRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.communicationService = communicationService;
		this.attachmentRepository = attachmentRepository;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.errandNumberGeneratorService = errandNumberGeneratorService;
		this.errandAttachmentService = errandAttachmentService;
		this.conversationService = conversationService;
		this.notesClient = notesClient;
		this.accessControlService = accessControlService;
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

		validateMissingMetadataLabels(errandEntity);

		final var persistedEntity = repository.save(errandEntity);
		final var revision = revisionService.createErrandRevision(persistedEntity);

		// Create a log event, but don't create a notification.
		eventService.createErrandEvent(CREATE, EVENT_LOG_CREATE_ERRAND, persistedEntity, revision.latest(), null, false, ERRAND);

		return persistedEntity.getId();
	}

	public Page<Errand> findErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter, final Pageable pageable) {
		final var fullFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get())).and(filter);
		final var matches = repository.findAll(fullFilter, pageable);

		return new PageImpl<>(toErrands(matches.getContent()), pageable, matches.getTotalElements());
	}

	public Errand readErrand(final String namespace, final String municipalityId, final String id) {
		verifyExistingErrand(id, namespace, municipalityId, false);
		return toErrand(repository
			.findOne(withId(id).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get())))
			.orElseThrow(() -> Problem.valueOf(UNAUTHORIZED, ENTITY_NOT_ACCESSIBLE.formatted(Optional.ofNullable(Identifier.get())
				.map(Identifier::getValue)
				.orElse(null)))));
	}

	public Errand updateErrand(final String namespace, final String municipalityId, final String id, final Errand errand) {
		verifyExistingErrand(id, namespace, municipalityId, true);
		final var errandEntityToUpdate = repository
			.findOne(withId(id).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get(), Access.AccessLevelEnum.RW)))
			.orElseThrow(() -> Problem.valueOf(UNAUTHORIZED, ENTITY_NOT_ACCESSIBLE.formatted(Optional.ofNullable(Identifier.get())
				.map(Identifier::getValue)
				.orElse(null))));
		final var errandEntity = updateEntity(errandEntityToUpdate, errand);

		// Add contactReason
		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			final var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));

			errandEntity.withContactReason(contactReason);
		});

		validateMissingMetadataLabels(errandEntity);

		final var entity = repository.save(errandEntity);

		final var revisionResult = revisionService.createErrandRevision(entity);

		// Create a log event if the update has modified the errand (and thus has created a new revision)
		if (nonNull(revisionResult)) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, revisionResult.latest(), revisionResult.previous(), ERRAND);
		}

		return toErrand(entity);
	}

	public void deleteErrand(final String namespace, final String municipalityId, final String id) {
		verifyExistingErrand(id, namespace, municipalityId, true);

		final var entity = repository
			.findOne(withId(id).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get(), Access.AccessLevelEnum.RW)))
			.orElseThrow(() -> Problem.valueOf(UNAUTHORIZED, ENTITY_NOT_ACCESSIBLE.formatted(Optional.ofNullable(Identifier.get())
				.map(Identifier::getValue)
				.orElse(null))));

		conversationService.deleteByErrandId(municipalityId, namespace, id);

		communicationService.deleteAllCommunicationsByErrandNumber(entity.getErrandNumber());
		errandAttachmentService.readErrandAttachments(namespace, municipalityId, id)
			.forEach(attachment -> attachmentRepository.deleteById(attachment.getId()));

		final var notes = notesClient.findNotes(municipalityId, null, null, id, null, null, 1, 1000);
		notes.getNotes().forEach(note -> notesClient.deleteNoteById(municipalityId, note.getId()));

		// Delete errand
		repository.deleteById(id);

		// Create a log event
		final var latestRevision = revisionService.getLatestErrandRevision(namespace, municipalityId, id);
		eventService.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, latestRevision, null, false, ERRAND);
	}

	public Long countErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter) {
		final var fullFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(filter);
		return repository.count(fullFilter);
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

	// TODO: Remove when UF-17592 is implemented.
	private void validateMissingMetadataLabels(ErrandEntity errandEntity) {
		final var namespace = errandEntity.getNamespace();
		final var municipalityId = errandEntity.getMunicipalityId();

		final var missingIds = Optional.ofNullable(errandEntity.getLabels()).orElse(emptyList()).stream()
			.filter(errandLabelEmbeddable -> !metadataLabelRepository.existsByNamespaceAndMunicipalityIdAndId(namespace, municipalityId, errandLabelEmbeddable.getMetadataLabelId()))
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(toCollection(ArrayList::new));

		if (isNotEmpty(missingIds)) {
			throw Problem.valueOf(NOT_FOUND, LABELS_NOT_FOUND.formatted(missingIds.stream().collect(joining(", ")), namespace, municipalityId));
		}
	}
}
