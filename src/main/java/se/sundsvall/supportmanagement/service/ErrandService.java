package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.DELETE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.zalando.problem.Status.BAD_REQUEST;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationSubType.ERRAND;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandWithAccessControl;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.toErrandsWithAccessControl;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.updateEntity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

import generated.se.sundsvall.accessmapper.Access;
import java.util.Optional;
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
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.util.ErrandNumberGeneratorService;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.mapper.ErrandMapper;
import se.sundsvall.supportmanagement.service.model.ReferredFrom;

@Service
@Transactional
public class ErrandService {

	private static final String BAD_CONTACT_REASON = "'%s' is not a valid contact reason for namespace '%s' and municipality with id '%s'";
	private static final String EVENT_LOG_CREATE_ERRAND = "Ärendet har skapats.";
	private static final String EVENT_LOG_UPDATE_ERRAND = "Ärendet har uppdaterats.";
	private static final String EVENT_LOG_DELETE_ERRAND = "Ärendet har raderats.";

	private final ErrandsRepository repository;
	private final ContactReasonRepository contactReasonRepository;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final ErrandNumberGeneratorService errandNumberGeneratorService;
	private final ErrandAttachmentService errandAttachmentService;
	private final CommunicationService communicationService;
	private final AttachmentRepository attachmentRepository;
	private final ConversationService conversationService;
	private final NotesClient notesClient;
	private final AccessControlService accessControlService;
	private final RelationClient relationClient;

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
		final AccessControlService accessControlService,
		final RelationClient relationClient) {

		this.repository = repository;
		this.contactReasonRepository = contactReasonRepository;
		this.communicationService = communicationService;
		this.attachmentRepository = attachmentRepository;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.errandNumberGeneratorService = errandNumberGeneratorService;
		this.errandAttachmentService = errandAttachmentService;
		this.conversationService = conversationService;
		this.notesClient = notesClient;
		this.accessControlService = accessControlService;
		this.relationClient = relationClient;
	}

	public String createErrand(String namespace, String municipalityId, Errand errand) {
		return createErrand(namespace, municipalityId, errand, null);
	}

	public String createErrand(final String namespace, final String municipalityId, final Errand errand, final String referredFrom) {
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

		// Create a log event, but don't create a notification.
		eventService.createErrandEvent(CREATE, EVENT_LOG_CREATE_ERRAND, persistedEntity, revision.latest(), null, false, ERRAND);

		if (isNotBlank(referredFrom)) {
			final var expandedReferredFrom = expandReferredFrom(referredFrom);
			final var relation = ErrandMapper.toReferredFromRelation(namespace, expandedReferredFrom, persistedEntity.getId());
			relationClient.createRelation(municipalityId, relation);
		}

		return persistedEntity.getId();
	}

	public Page<Errand> findErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter, final Pageable pageable) {
		final var fullFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get())).and(filter);
		final var matches = repository.findAll(fullFilter, pageable);
		final var limitedMapping = accessControlService.limitedMappingPredicateByLabel(namespace, municipalityId, Identifier.get());

		return new PageImpl<>(toErrandsWithAccessControl(matches.getContent(), limitedMapping), pageable, matches.getTotalElements());
	}

	public Errand readErrand(final String namespace, final String municipalityId, final String id) {
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false);
		final var limitedMapping = accessControlService.limitedMappingPredicateByLabel(namespace, municipalityId, Identifier.get());
		return toErrandWithAccessControl(errandEntity, limitedMapping);
	}

	public Errand updateErrand(final String namespace, final String municipalityId, final String id, final Errand errand) {
		final var errandEntityToUpdate = accessControlService.getErrand(namespace, municipalityId, id, true, Access.AccessLevelEnum.RW);

		final var errandEntity = updateEntity(errandEntityToUpdate, errand);

		// Add contactReason
		Optional.ofNullable(errand.getContactReason()).ifPresent(reason -> {
			final var contactReason = contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId(reason, namespace, municipalityId)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_CONTACT_REASON.formatted(reason, namespace, municipalityId)));

			errandEntity.withContactReason(contactReason);
		});

		final var entity = repository.save(errandEntity);

		final var revisionResult = revisionService.createErrandRevision(entity);

		// Create a log event if the update has modified the errand (and thus has created a new revision)
		if (nonNull(revisionResult)) {
			eventService.createErrandEvent(UPDATE, EVENT_LOG_UPDATE_ERRAND, entity, revisionResult.latest(), revisionResult.previous(), ERRAND);
		}

		return toErrand(entity);
	}

	public void deleteErrand(final String namespace, final String municipalityId, final String id) {
		final var entity = accessControlService.getErrand(namespace, municipalityId, id, true, Access.AccessLevelEnum.RW);

		conversationService.deleteByErrandId(entity);

		communicationService.deleteAllCommunicationsByErrandNumber(entity.getErrandNumber());
		errandAttachmentService.readErrandAttachments(namespace, municipalityId, id)
			.forEach(attachment -> attachmentRepository.deleteById(attachment.getId()));

		final var notes = notesClient.findNotes(municipalityId, null, null, id, null, null, 1, 1000);
		notes.getNotes().forEach(note -> notesClient.deleteNoteById(municipalityId, note.getId()));

		// Delete errand
		repository.deleteById(id);

		// Create a log event
		final var latestRevision = revisionService.getLatestErrandRevision(entity);
		eventService.createErrandEvent(DELETE, EVENT_LOG_DELETE_ERRAND, entity, latestRevision, null, false, ERRAND);
	}

	public Long countErrands(final String namespace, final String municipalityId, final Specification<ErrandEntity> filter) {
		final var fullFilter = withNamespace(namespace).and(withMunicipalityId(municipalityId)).and(accessControlService.withAccessControl(namespace, municipalityId, Identifier.get())).and(filter);
		return repository.count(fullFilter);
	}

	ReferredFrom expandReferredFrom(final String referredFromAsString) {
		if (isNotBlank(referredFromAsString)) {
			var parts = referredFromAsString.split(",");
			if (parts.length == 3) {
				return new ReferredFrom(parts[0].trim(), parts[1].trim(), parts[2].trim());
			}
		}

		throw Problem.valueOf(BAD_REQUEST, "Referred from should be three comma-separated parts: <service>,<namespace>,<identifier>");
	}
}
