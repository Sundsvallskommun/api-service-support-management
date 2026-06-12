package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrand;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverErrandRequest;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverInclude;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverMapping;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverSourceAction;
import se.sundsvall.supportmanagement.api.model.errand.handover.HandoverTarget;
import se.sundsvall.supportmanagement.api.model.note.CreateErrandNoteRequest;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.HandoverIdempotencyRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.HandoverIdempotencyEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EntityType;
import se.sundsvall.supportmanagement.integration.relation.RelationClient;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.mapper.HandoverMapper;

import static generated.se.sundsvall.eventlog.EventType.CREATE;
import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_IN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.HANDOVER_OUT;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getAdUser;

@Service
public class HandoverService {

	private static final Logger LOG = LoggerFactory.getLogger(HandoverService.class);

	private static final String RELATION_TYPE_HANDOVER = "HANDOVER";
	private static final String RELATION_RESOURCE_TYPE = "case";
	private static final String RELATION_SERVICE = "support-management";
	private static final String EVENT_HANDOVER_OUT = "Ärendet har överlåtits till ett annat namespace.";
	private static final String EVENT_HANDOVER_IN = "Ärendet har mottagits via överlåtelse.";

	private final AccessControlService accessControlService;
	private final NamespaceConfigService namespaceConfigService;
	private final ErrandService errandService;
	private final ErrandNoteService errandNoteService;
	private final MetadataService metadataService;
	private final ErrandsRepository errandsRepository;
	private final AttachmentRepository attachmentRepository;
	private final RevisionService revisionService;
	private final EventService eventService;
	private final RelationClient relationClient;
	private final HandoverIdempotencyRepository idempotencyRepository;

	public HandoverService(
		final AccessControlService accessControlService,
		final NamespaceConfigService namespaceConfigService,
		final ErrandService errandService,
		final ErrandNoteService errandNoteService,
		final MetadataService metadataService,
		final ErrandsRepository errandsRepository,
		final AttachmentRepository attachmentRepository,
		final RevisionService revisionService,
		final EventService eventService,
		final RelationClient relationClient,
		final HandoverIdempotencyRepository idempotencyRepository) {

		this.accessControlService = accessControlService;
		this.namespaceConfigService = namespaceConfigService;
		this.errandService = errandService;
		this.errandNoteService = errandNoteService;
		this.metadataService = metadataService;
		this.errandsRepository = errandsRepository;
		this.attachmentRepository = attachmentRepository;
		this.revisionService = revisionService;
		this.eventService = eventService;
		this.relationClient = relationClient;
		this.idempotencyRepository = idempotencyRepository;
	}

	@Transactional
	public HandoverErrand handover(
		final String namespace,
		final String municipalityId,
		final String errandId,
		final HandoverErrandRequest request) {

		if (namespace.equals(request.getTarget().getNamespace()) && municipalityId.equals(request.getTarget().getMunicipalityId())) {
			throw Problem.valueOf(BAD_REQUEST, "Source and target namespace/municipalityId must differ");
		}

		final var existing = idempotencyRepository.findBySourceErrandIdAndTargetNamespaceAndTargetMunicipalityId(
			errandId, request.getTarget().getNamespace(), request.getTarget().getMunicipalityId());
		if (existing.isPresent()) {
			LOG.debug("Handover already performed for errand '{}' to '{}/{}', returning existing result", errandId, request.getTarget().getNamespace(), request.getTarget().getMunicipalityId());
			return toHandoverErrand(existing.get());
		}

		if (HandoverSourceAction.SUSPEND.equals(Optional.ofNullable(request.getSourceHandling()).map(s -> s.getAction()).orElse(null))) {
			throw Problem.valueOf(NOT_IMPLEMENTED, "SUSPEND source action is not yet supported");
		}
		if (HandoverSourceAction.CLOSE.equals(Optional.ofNullable(request.getSourceHandling()).map(s -> s.getAction()).orElse(null))
			&& isBlank(Optional.ofNullable(request.getSourceHandling()).map(s -> s.getStatus()).orElse(null))) {
			throw Problem.valueOf(BAD_REQUEST, "status is required when sourceHandling action is CLOSE");
		}

		try {
			namespaceConfigService.get(request.getTarget().getNamespace(), request.getTarget().getMunicipalityId());
		} catch (final Exception e) {
			throw Problem.valueOf(BAD_REQUEST, "Target namespace '%s' with municipalityId '%s' has no configuration".formatted(request.getTarget().getNamespace(), request.getTarget().getMunicipalityId()));
		}

		validateMappings(request.getTarget().getNamespace(), request.getTarget().getMunicipalityId(), request);

		final var idempotencyEntity = idempotencyRepository.save(HandoverIdempotencyEntity.create()
			.withSourceErrandId(errandId)
			.withTargetNamespace(request.getTarget().getNamespace())
			.withTargetMunicipalityId(request.getTarget().getMunicipalityId()));

		final var source = accessControlService.getErrand(namespace, municipalityId, errandId, false);

		final var targetErrand = HandoverMapper.buildTargetErrand(source, request);

		final var newErrandId = errandService.createErrand(
			request.getTarget().getNamespace(),
			request.getTarget().getMunicipalityId(),
			targetErrand,
			null);

		final var targetEntity = errandsRepository.findById(newErrandId)
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to fetch created errand with id '%s'".formatted(newErrandId)));

		if (isIncludeAttachments(request)) {
			copyAttachments(source, targetEntity, request.getTarget().getNamespace(), request.getTarget().getMunicipalityId());
		}

		final var relationId = createHandoverRelation(
			source, newErrandId, namespace, request.getTarget().getNamespace(), request.getTarget().getMunicipalityId());

		logHandoverEvents(source, targetEntity);

		final var warnings = HandoverMapper.buildWarnings(source, request);
		final var appliedMappings = HandoverMapper.buildAppliedMappings(request);
		final var response = HandoverErrand.create()
			.withNewErrandId(newErrandId)
			.withNewErrandNumber(targetEntity.getErrandNumber())
			.withTarget(request.getTarget())
			.withRelationId(relationId)
			.withAppliedMappings(appliedMappings)
			.withWarnings(warnings);

		updateIdempotencyRecord(idempotencyEntity, response);

		handleSourceErrand(namespace, municipalityId, errandId, request);

		return response;
	}

	private void validateMappings(final String targetNamespace, final String targetMunicipalityId, final HandoverErrandRequest request) {
		final var mapping = request.getMapping();

		if (isBlank(mapping.getStatus())) {
			throw Problem.valueOf(BAD_REQUEST, "Required mapping 'status' is missing");
		}
		if (metadataService.isValidated(targetNamespace, targetMunicipalityId, EntityType.STATUS)) {
			final var validStatus = metadataService.findStatuses(targetNamespace, targetMunicipalityId, Sort.unsorted()).stream()
				.anyMatch(s -> s.getName().equals(mapping.getStatus()));
			if (!validStatus) {
				throw Problem.valueOf(BAD_REQUEST, "Status '%s' does not exist in target namespace '%s'".formatted(mapping.getStatus(), targetNamespace));
			}
		}

		if (isNull(mapping.getClassification())) {
			throw Problem.valueOf(BAD_REQUEST, "Required mapping 'classification' is missing");
		}
		final var category = mapping.getClassification().getCategory();
		final var type = mapping.getClassification().getType();
		if (metadataService.isValidated(targetNamespace, targetMunicipalityId, EntityType.CATEGORY)) {
			final var validCategory = metadataService.findCategories(targetNamespace, targetMunicipalityId, Sort.unsorted()).stream()
				.anyMatch(c -> c.getName().equals(category));
			if (!validCategory) {
				throw Problem.valueOf(BAD_REQUEST, "Classification category '%s' does not exist in target namespace '%s'".formatted(category, targetNamespace));
			}
		}
		if (metadataService.isValidated(targetNamespace, targetMunicipalityId, EntityType.TYPE)) {
			final var validType = metadataService.findTypes(targetNamespace, targetMunicipalityId, category).stream()
				.anyMatch(t -> t.getName().equals(type));
			if (!validType) {
				throw Problem.valueOf(BAD_REQUEST, "Classification type '%s' does not exist under category '%s' in target namespace '%s'".formatted(type, category, targetNamespace));
			}
		}

		if (isNull(mapping.getLabels())) {
			throw Problem.valueOf(BAD_REQUEST, "Required mapping 'labels' is missing (provide an empty list if the target namespace does not use labels)");
		}
		if (metadataService.hasLabels(targetNamespace, targetMunicipalityId)) {
			mapping.getLabels().stream()
				.filter(id -> !metadataService.labelExistsById(id, targetNamespace, targetMunicipalityId))
				.findFirst()
				.ifPresent(id -> {
					throw Problem.valueOf(BAD_REQUEST, "Label '%s' does not exist in target namespace '%s'".formatted(id, targetNamespace));
				});
		}

		validateContactReasonMapping(targetNamespace, targetMunicipalityId, mapping);
	}

	private void validateContactReasonMapping(final String targetNamespace, final String targetMunicipalityId, final HandoverMapping mapping) {
		if (isBlank(mapping.getContactReason()) || !metadataService.isValidated(targetNamespace, targetMunicipalityId, EntityType.CONTACT_REASON)) {
			return;
		}
		final var validContactReason = metadataService.findContactReasons(targetNamespace, targetMunicipalityId, Sort.unsorted()).stream()
			.anyMatch(cr -> cr.getReason().equalsIgnoreCase(mapping.getContactReason()));
		if (!validContactReason) {
			throw Problem.valueOf(BAD_REQUEST, "Contact reason '%s' does not exist in target namespace '%s'".formatted(mapping.getContactReason(), targetNamespace));
		}
	}

	private boolean isIncludeAttachments(final HandoverErrandRequest request) {
		return Optional.ofNullable(request.getInclude())
			.map(HandoverInclude::isAttachments)
			.orElse(false);
	}

	private void copyAttachments(final ErrandEntity source, final ErrandEntity target, final String targetNamespace, final String targetMunicipalityId) {
		final var freshSource = errandsRepository.findByIdAndNamespaceAndMunicipalityId(source.getId(), source.getNamespace(), source.getMunicipalityId())
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to re-fetch source errand '%s' for attachment copy".formatted(source.getId())));
		final var sourceAttachments = Optional.ofNullable(freshSource.getAttachments()).orElse(List.of());
		for (final var sourceAttachment : sourceAttachments) {
			final var sourceData = sourceAttachment.getAttachmentData();
			if (isNull(sourceData) || isNull(sourceData.getFile())) {
				continue;
			}
			try {
				final var blob = sourceData.getFile();
				try (final var inputStream = blob.getBinaryStream()) {
					final var newBlob = Hibernate.getLobHelper().createBlob(inputStream, blob.length());
					final var newAttachment = AttachmentEntity.create()
						.withErrandEntity(target)
						.withNamespace(targetNamespace)
						.withMunicipalityId(targetMunicipalityId)
						.withFileName(sourceAttachment.getFileName())
						.withMimeType(sourceAttachment.getMimeType())
						.withChannel(sourceAttachment.getChannel())
						.withFileSize(sourceAttachment.getFileSize())
						.withAttachmentData(AttachmentDataEntity.create().withFile(newBlob));
					attachmentRepository.saveAndFlush(newAttachment);
					target.getAttachments().add(newAttachment);
				}
			} catch (final Exception e) {
				throw Problem.valueOf(INTERNAL_SERVER_ERROR,
					"Failed to copy attachment '%s': %s".formatted(sourceAttachment.getFileName(), e.getMessage()));
			}
		}
	}

	private String createHandoverRelation(
		final ErrandEntity source,
		final String newErrandId,
		final String sourceNamespace,
		final String targetNamespace,
		final String targetMunicipalityId) {

		final var relation = new Relation()
			.type(RELATION_TYPE_HANDOVER)
			.source(new ResourceIdentifier()
				.resourceId(source.getId())
				.type(RELATION_RESOURCE_TYPE)
				.service(RELATION_SERVICE)
				.namespace(sourceNamespace))
			.target(new ResourceIdentifier()
				.resourceId(newErrandId)
				.type(RELATION_RESOURCE_TYPE)
				.service(RELATION_SERVICE)
				.namespace(targetNamespace));
		final var response = relationClient.createRelation(targetMunicipalityId, relation);
		return Optional.ofNullable(response.getHeaders().getLocation())
			.map(URI::getPath)
			.map(path -> path.substring(path.lastIndexOf('/') + 1))
			.orElse(null);
	}

	private void handleSourceErrand(
		final String namespace,
		final String municipalityId,
		final String errandId,
		final HandoverErrandRequest request) {

		if (isNull(request.getSourceHandling())) {
			return;
		}
		final var action = request.getSourceHandling().getAction();
		if (HandoverSourceAction.CLOSE.equals(action)) {
			errandService.updateErrand(namespace, municipalityId, errandId, Errand.create()
				.withStatus(request.getSourceHandling().getStatus())
				.withResolution(request.getSourceHandling().getResolution()));
			if (!isBlank(request.getSourceHandling().getClosingComment())) {
				errandNoteService.createErrandNote(namespace, municipalityId, errandId, CreateErrandNoteRequest.create()
					.withContext("SUPPORT")
					.withRole("SYSTEM")
					.withCreatedBy(getAdUser())
					.withSubject("Handover closing comment")
					.withBody(request.getSourceHandling().getClosingComment()));
			}
		}
	}

	private void logHandoverEvents(final ErrandEntity source, final ErrandEntity target) {
		try {
			final var sourceRevision = revisionService.getLatestErrandRevision(source);
			eventService.createErrandEvent(UPDATE, EVENT_HANDOVER_OUT, source, sourceRevision, null, false, HANDOVER_OUT);
		} catch (final Exception e) {
			LOG.warn("Failed to log HANDOVER_OUT event for errand '{}': {}", source.getId(), e.getMessage());
		}
		try {
			final var targetRevision = revisionService.getLatestErrandRevision(target);
			eventService.createErrandEvent(CREATE, EVENT_HANDOVER_IN, target, targetRevision, null, false, HANDOVER_IN);
		} catch (final Exception e) {
			LOG.warn("Failed to log HANDOVER_IN event for errand '{}': {}", target.getId(), e.getMessage());
		}
	}

	private void updateIdempotencyRecord(final HandoverIdempotencyEntity entity, final HandoverErrand response) {
		entity.setNewErrandId(response.getNewErrandId());
		entity.setNewErrandNumber(response.getNewErrandNumber());
		entity.setRelationId(response.getRelationId());
		entity.setWarnings(HandoverMapper.encodeWarnings(response.getWarnings()));
		idempotencyRepository.save(entity);
	}

	private HandoverErrand toHandoverErrand(final HandoverIdempotencyEntity entity) {
		return HandoverErrand.create()
			.withNewErrandId(entity.getNewErrandId())
			.withNewErrandNumber(entity.getNewErrandNumber())
			.withTarget(HandoverTarget.create()
				.withNamespace(entity.getTargetNamespace())
				.withMunicipalityId(entity.getTargetMunicipalityId()))
			.withRelationId(entity.getRelationId())
			.withWarnings(HandoverMapper.decodeWarnings(entity.getWarnings()));
	}
}
