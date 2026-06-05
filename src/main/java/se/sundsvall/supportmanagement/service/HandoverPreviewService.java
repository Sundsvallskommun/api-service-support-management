package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreview;
import se.sundsvall.supportmanagement.api.model.handover.HandoverPreviewRequest;
import se.sundsvall.supportmanagement.api.model.handover.MappingRequired;
import se.sundsvall.supportmanagement.api.model.handover.Warning;
import se.sundsvall.supportmanagement.integration.db.CategoryRepository;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.StatusRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RoleEntity;
import se.sundsvall.supportmanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatusEntity;
import se.sundsvall.supportmanagement.integration.jsonschema.JsonSchemaClient;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toClassificationCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toClassificationMapping;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toContactReasonCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toContactReasonMapping;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toDirectlyCopyable;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toLabelMappings;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toNotCopyable;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toStatusCandidates;
import static se.sundsvall.supportmanagement.service.mapper.HandoverPreviewMapper.toStatusMapping;

/**
 * Builds a side-effect free preview describing how a source errand would be handed over to another namespace.
 *
 * <p>
 * The preview projects the directly copyable fields, the candidate lists for the namespace-bound fields read from the
 * target namespace metadata, the fields that can not be copied, and warnings (unknown stakeholder roles and parameter
 * json schemas that are not registered in the target).
 * </p>
 *
 * <p>
 * {@link AccessControlService} is used solely to verify that the caller is authorized to hand over the source errand;
 * the errand itself is then loaded through {@link ErrandsRepository} for building the preview.
 * </p>
 */
@Service
public class HandoverPreviewService {

	private static final Sort SORT_BY_SORT_ORDER = Sort.by("sortOrder");
	private static final String SCHEMA_NOT_REGISTERED = "jsonSchema '%s' not registered in target";
	private static final String ERRAND_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";

	private final AccessControlService accessControlService;
	private final ErrandsRepository errandsRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final StatusRepository statusRepository;
	private final CategoryRepository categoryRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ContactReasonRepository contactReasonRepository;
	private final RoleRepository roleRepository;
	private final JsonSchemaClient jsonSchemaClient;

	public HandoverPreviewService(
		final AccessControlService accessControlService,
		final ErrandsRepository errandsRepository,
		final NamespaceConfigRepository namespaceConfigRepository,
		final StatusRepository statusRepository,
		final CategoryRepository categoryRepository,
		final MetadataLabelRepository metadataLabelRepository,
		final ContactReasonRepository contactReasonRepository,
		final RoleRepository roleRepository,
		final JsonSchemaClient jsonSchemaClient) {
		this.accessControlService = accessControlService;
		this.errandsRepository = errandsRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.statusRepository = statusRepository;
		this.categoryRepository = categoryRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.contactReasonRepository = contactReasonRepository;
		this.roleRepository = roleRepository;
		this.jsonSchemaClient = jsonSchemaClient;
	}

	/**
	 * Builds a handover preview for the given source errand against the target namespace described in the request.
	 *
	 * @param  namespace      the source namespace
	 * @param  municipalityId the source municipality id
	 * @param  errandId       the source errand id
	 * @param  request        the target namespace/municipality to preview a handover to
	 * @return                a {@link HandoverPreview} describing copyable, mappable and non-copyable fields
	 */
	@Transactional(readOnly = true)
	public HandoverPreview previewHandover(final String namespace, final String municipalityId, final String errandId, final HandoverPreviewRequest request) {
		final var targetNamespace = request.getTargetNamespace();
		final var targetMunicipalityId = request.getTargetMunicipalityId();

		if (Objects.equals(namespace, targetNamespace) && Objects.equals(municipalityId, targetMunicipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, "Target namespace and municipalityId must differ from the source errand");
		}

		// Verify the caller is authorized to hand over the source errand (404 if missing, 401 if the user lacks access).
		// Full read access (R or RW) is required; limited-read (LR) is intentionally excluded since a handover concerns
		// the whole errand. Executing the actual handover (separate story) may instead require write access (RW).
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, errandId, R, RW);

		// The target namespace must be configured to be a valid handover destination
		if (!namespaceConfigRepository.existsByNamespaceAndMunicipalityId(targetNamespace, targetMunicipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, "Target namespace '%s' for municipality '%s' does not exist".formatted(targetNamespace, targetMunicipalityId));
		}

		final var errand = errandsRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND.formatted(errandId, namespace, municipalityId)));

		return HandoverPreview.create()
			.withDirectlyCopyable(toDirectlyCopyable(errand))
			.withMappingRequired(buildMappingRequired(namespace, municipalityId, targetNamespace, targetMunicipalityId, errand))
			.withNotCopyable(toNotCopyable())
			.withWarnings(buildWarnings(errand, targetNamespace, targetMunicipalityId));
	}

	private MappingRequired buildMappingRequired(final String sourceNamespace, final String sourceMunicipalityId, final String targetNamespace, final String targetMunicipalityId, final ErrandEntity errand) {
		// Candidate lists are the selectable options configured in the target namespace
		final var statusCandidates = toStatusCandidates(statusRepository.findAllByNamespaceAndMunicipalityId(targetNamespace, targetMunicipalityId, SORT_BY_SORT_ORDER));
		final var classificationCandidates = toClassificationCandidates(categoryRepository.findAllByNamespaceAndMunicipalityId(targetNamespace, targetMunicipalityId, SORT_BY_SORT_ORDER));
		final var labelCandidates = toLabelCandidates(metadataLabelRepository.findByNamespaceAndMunicipalityId(targetNamespace, targetMunicipalityId));
		final var contactReasonCandidates = toContactReasonCandidates(contactReasonRepository.findAllByNamespaceAndMunicipalityId(targetNamespace, targetMunicipalityId, SORT_BY_SORT_ORDER));

		final var sourceStatusDisplayName = resolveSourceStatusDisplayName(sourceNamespace, sourceMunicipalityId, errand.getStatus());

		return MappingRequired.create()
			.withStatus(toStatusMapping(errand, sourceStatusDisplayName, statusCandidates))
			.withClassification(toClassificationMapping(errand, classificationCandidates))
			.withLabels(toLabelMappings(errand, labelCandidates))
			.withContactReason(toContactReasonMapping(errand, contactReasonCandidates));
	}

	/**
	 * The errand only stores the status technical name, so the human readable display name of the source status is looked
	 * up in the source namespace to render {@code status.source.displayName}. Returns {@code null} when the errand has no
	 * status or the status is not (or no longer) configured in the source namespace.
	 */
	private String resolveSourceStatusDisplayName(final String sourceNamespace, final String sourceMunicipalityId, final String statusName) {
		return ofNullable(statusName)
			.flatMap(name -> statusRepository.findAllByNamespaceAndMunicipalityId(sourceNamespace, sourceMunicipalityId, SORT_BY_SORT_ORDER).stream()
				.filter(status -> name.equals(status.getName()))
				.findFirst()
				.map(StatusEntity::getDisplayName))
			.orElse(null);
	}

	private List<Warning> buildWarnings(final ErrandEntity errand, final String targetNamespace, final String targetMunicipalityId) {
		final var warnings = new ArrayList<Warning>();
		warnings.addAll(roleWarnings(errand, targetNamespace, targetMunicipalityId));
		warnings.addAll(schemaWarnings(errand, targetMunicipalityId));
		return warnings;
	}

	private List<Warning> roleWarnings(final ErrandEntity errand, final String targetNamespace, final String targetMunicipalityId) {
		final var targetRoles = roleRepository.findAllByNamespaceAndMunicipalityId(targetNamespace, targetMunicipalityId, SORT_BY_SORT_ORDER).stream()
			.map(RoleEntity::getName)
			.collect(toSet());

		return ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(StakeholderEntity::getRole)
			.filter(StringUtils::isNotBlank)
			.distinct()
			.filter(role -> !targetRoles.contains(role))
			.map(Warning::roleNotInTarget)
			.toList();
	}

	private List<Warning> schemaWarnings(final ErrandEntity errand, final String targetMunicipalityId) {
		final var warnings = new ArrayList<Warning>();
		// Cache the registration check per schemaId so a schema referenced by several parameters is only looked up once
		final var registrationBySchemaId = new HashMap<String, Boolean>();

		for (final var jsonParameter : ofNullable(errand.getJsonParameters()).orElse(emptyList())) {
			final var schemaId = jsonParameter.getSchemaId();
			if (isBlank(schemaId)) {
				continue;
			}
			final boolean registered = registrationBySchemaId.computeIfAbsent(schemaId, id -> isSchemaRegistered(targetMunicipalityId, id));
			if (!registered) {
				warnings.add(Warning.parameterSchemaMismatch(jsonParameter.getKey(), SCHEMA_NOT_REGISTERED.formatted(schemaId)));
			}
		}

		return warnings;
	}

	/**
	 * Checks whether a json schema is registered in the target municipality. Uses the read-only, side-effect free
	 * {@code getSchemaById} endpoint; a {@code 404 Not Found} means the schema is not registered. Any other error is
	 * propagated.
	 */
	private boolean isSchemaRegistered(final String municipalityId, final String schemaId) {
		try {
			jsonSchemaClient.getSchemaById(municipalityId, schemaId);
			return true;
		} catch (final ThrowableProblem e) {
			if (e.getStatus() == NOT_FOUND) {
				return false;
			}
			throw e;
		}
	}
}
