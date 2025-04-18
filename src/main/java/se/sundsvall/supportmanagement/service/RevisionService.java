package se.sundsvall.supportmanagement.service;

import static com.flipkart.zjsonpatch.DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE;
import static com.flipkart.zjsonpatch.DiffFlags.OMIT_COPY_OPERATION;
import static com.flipkart.zjsonpatch.DiffFlags.OMIT_MOVE_OPERATION;
import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toRevision;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toRevisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toSerializedSnapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.DiffFlags;
import com.flipkart.zjsonpatch.JsonDiff;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.revision.DifferenceResponse;
import se.sundsvall.supportmanagement.api.model.revision.Operation;
import se.sundsvall.supportmanagement.api.model.revision.Revision;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;
import se.sundsvall.supportmanagement.integration.notes.NotesClient;
import se.sundsvall.supportmanagement.service.mapper.ErrandNoteMapper;
import se.sundsvall.supportmanagement.service.mapper.RevisionMapper;

@Service
@Transactional
public class RevisionService {

	private static final EnumSet<DiffFlags> DIFF_SETTINGS = EnumSet.of(ADD_ORIGINAL_VALUE_ON_REPLACE, OMIT_COPY_OPERATION, OMIT_MOVE_OPERATION);

	private static final Configuration JSONPATH_CONFIG = defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS);

	private static final Logger LOG = LoggerFactory.getLogger(RevisionService.class);

	private static final List<String> EXCLUDED_ATTRIBUTES = List.of("$..stakeholders[*].id", "$..attachments[*].id", "$..attachments[*].file", "$..modified", "$..touched");

	private static final String COMPARISON_ERROR_LOG_MESSAGE = "An error occurred during comparison";

	private static final String COMPARISON_ERROR_PROBLEM = "An error occurred when comparing version %s to version %s of entityId '%s'";

	private static final String VERSION_DOES_NOT_EXIST = "The version requested for the %s revision does not exist";

	private static final String ERRAND_NOT_FOUND = "An errand with id '%s' could not be found";

	private final ErrandsRepository errandsRepository;

	private final RevisionRepository revisionRepository;

	private final ObjectMapper objectMapper;

	private final NotesClient notesClient;

	public RevisionService(final ErrandsRepository errandsRepository,
		final RevisionRepository revisionRepository, final ObjectMapper objectMapper,
		final NotesClient notesClient) {
		this.errandsRepository = errandsRepository;
		this.revisionRepository = revisionRepository;
		this.objectMapper = objectMapper;
		this.notesClient = notesClient;
	}

	/**
	 * Create a new revision.
	 * <p>
	 * A new revision will be created if: - the last revisions serialized-snapshot differs from the current (i.e. provided)
	 * entity. - no previous revisions exist for the provided entity.
	 *
	 * @param  entity the entity that will have a new revision.
	 * @return        the created revision.
	 */
	public RevisionResult createErrandRevision(final ErrandEntity entity) {

		final var lastRevision = revisionRepository.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(entity.getNamespace(), entity.getMunicipalityId(), entity.getId());
		final Revision newRevision;

		if (lastRevision.isPresent()) {
			// No changes since last revision, return.
			if (jsonEquals(lastRevision.get().getSerializedSnapshot(), toSerializedSnapshot(entity))) {
				return null;
			}

			// Create revision <lastRevision.version + 1>
			newRevision = toRevision(createRevision(entity, lastRevision.get().getVersion() + 1));
		} else {
			// No previous revisions exist. Create revision 0
			newRevision = toRevision(createRevision(entity, 0));
		}

		return new RevisionResult(lastRevision.map(RevisionMapper::toRevision).orElse(null), newRevision);
	}

	private RevisionEntity createRevision(final ErrandEntity entity, final int version) {
		return revisionRepository.save(toRevisionEntity(entity, version));
	}

	private boolean jsonEquals(final String currentSnapshot, final String previousSnapshot) {
		if (anyNull(currentSnapshot, previousSnapshot)) {
			return false;
		}

		try {
			return toJsonNode(currentSnapshot).equals(toJsonNode(previousSnapshot));
		} catch (final Exception e) { // If something fails, log and return the json objects as unequal to force creation of a new revision
			LOG.error(COMPARISON_ERROR_LOG_MESSAGE, e);
		}

		return false;
	}

	/**
	 * Returns all existing revisions for an errand.
	 *
	 * @param  errandId id of the errand to fetch revisions for.
	 * @return          a list of Revision objects containing information on every revision of the errand.
	 */
	public List<Revision> getErrandRevisions(final String namespace, final String municipalityId, final String errandId) {
		verifyExistingErrand(errandId, namespace, municipalityId);

		return RevisionMapper.toRevisions(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(namespace, municipalityId, errandId));
	}

	/**
	 * Returns the latest (current) revision of the errand
	 *
	 * @param  namespace      namespace of the errand owning the note to compare.
	 * @param  municipalityId id of the municipality owning the errand.
	 * @param  errandId       id of the errand to fetch latest revision for.
	 * @return                the latest revision for the errand or null if errand does not exist.
	 */
	public Revision getLatestErrandRevision(final String namespace, final String municipalityId, final String errandId) {
		return revisionRepository.findFirstByNamespaceAndMunicipalityIdAndEntityIdOrderByVersionDesc(namespace, municipalityId, errandId)
			.map(RevisionMapper::toRevision)
			.orElse(null);
	}

	/**
	 * Returns requested revision of the errand
	 *
	 * @param  errandId id of the errand to fetch revision for.
	 * @param  version  the revision version to fetch.
	 * @return          requested revision for the errand or null if errand or revision does not exist.
	 */
	public Revision getErrandRevisionByVersion(final String namespace, final String municipalityId, final String errandId, final int version) {
		return revisionRepository.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(namespace, municipalityId, errandId, version)
			.map(RevisionMapper::toRevision)
			.orElse(null);
	}

	/**
	 * Compares two revision versions of an errand.
	 *
	 * @param  namespace      namespace of the errand owning the note to compare.
	 * @param  municipalityId id of the municipality owning the errand.
	 * @param  errandId       id of the errand to compare.
	 * @param  sourceVersion  version that will act as source in the comparison.
	 * @param  targetVersion  version that will act as target in the comparison.
	 * @return                response containing the difference between the source version and the target version.
	 */
	public DifferenceResponse compareErrandRevisionVersions(final String namespace, final String municipalityId, final String errandId, final int sourceVersion, final int targetVersion) {
		verifyExistingErrand(errandId, namespace, municipalityId);

		final var sourceRevision = revisionRepository.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(namespace, municipalityId, errandId, sourceVersion)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(VERSION_DOES_NOT_EXIST, "source")));

		final var targetRevision = revisionRepository.findByNamespaceAndMunicipalityIdAndEntityIdAndVersion(namespace, municipalityId, errandId, targetVersion)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(VERSION_DOES_NOT_EXIST, "target")));

		try {
			// Deserialize revisions to JsonNodes
			final var sourceJson = toJsonNode(sourceRevision.getSerializedSnapshot());
			final var targetJson = toJsonNode(targetRevision.getSerializedSnapshot());

			// Perform diff
			final var differences = JsonDiff.asJson(sourceJson, targetJson, DIFF_SETTINGS);

			// Return result
			return DifferenceResponse.create().withOperations(List.of(objectMapper.readValue(differences.toString(), Operation[].class)));
		} catch (final Exception e) {
			LOG.error(COMPARISON_ERROR_LOG_MESSAGE, e);
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, String.format(COMPARISON_ERROR_PROBLEM, sourceVersion, targetVersion, errandId));
		}
	}

	/**
	 * Returns all existing revisions for a note.
	 *
	 * @param  namespace      namespace of the errand owning the note to compare.
	 * @param  municipalityId id of the municipality owning the errand.
	 * @param  errandId       id of the errand owning the note to compare.
	 * @param  noteId         id of the note to fetch revisions for.
	 * @return                a list of Revision objects containing information on every revision of the note.
	 */
	public List<Revision> getNoteRevisions(final String namespace, final String municipalityId, final String errandId, final String noteId) {
		verifyExistingErrand(errandId, namespace, municipalityId);

		return ErrandNoteMapper.toRevisions(notesClient.findAllNoteRevisions(municipalityId, noteId));
	}

	/**
	 * Compares two revision versions of a note.
	 *
	 * @param  namespace      namespace of the errand owning the note to compare.
	 * @param  municipalityId id of the municipality owning the errand.
	 * @param  errandId       id of the errand owning the note to compare.
	 * @param  noteId         id of the note to compare.
	 * @param  sourceVersion  version that will act as source in the comparison.
	 * @param  targetVersion  version that will act as target in the comparison.
	 * @return                response containing the difference between the source version and the target version.
	 */
	public DifferenceResponse compareNoteRevisionVersions(final String namespace, final String municipalityId, final String errandId, final String noteId, final int sourceVersion, final int targetVersion) {
		verifyExistingErrand(errandId, namespace, municipalityId);

		return ErrandNoteMapper.toDifferenceResponse(notesClient.compareNoteRevisions(municipalityId, noteId, sourceVersion, targetVersion));
	}

	private void verifyExistingErrand(final String errandId, final String namespace, final String municipalityId) {
		if (!errandsRepository.existsByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_NOT_FOUND, errandId));
		}
	}

	private JsonNode toJsonNode(final String value) {
		try {
			final var document = JsonPath.using(JSONPATH_CONFIG).parse(value);
			EXCLUDED_ATTRIBUTES.forEach(document::delete);
			return objectMapper.readTree(document.jsonString());
		} catch (final Exception e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

}
