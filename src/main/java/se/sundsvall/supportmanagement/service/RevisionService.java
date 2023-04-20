package se.sundsvall.supportmanagement.service;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toRevisionEntity;
import static se.sundsvall.supportmanagement.service.mapper.RevisionMapper.toSerializedSnapshot;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

@Service
public class RevisionService {
	private static final String ERROR_MESSAGE = "An error occured when comparing previous and current entity as json";
	private static final List<String> EXCLUDED_ATTRIBUTES = List.of("$.created", "$.modified", "$.touched", "$..stakeholders[*].id", "$..attachments[*].id");
	private static final Configuration JSONPATH_CONFIG = defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS);
	private static final Logger LOG = LoggerFactory.getLogger(RevisionService.class);

	private final ObjectMapper objectMapper;

	@Autowired
	private RevisionRepository revisionRepository;

	public RevisionService() {
		objectMapper = new ObjectMapper();
	}

	/**
	 * Create a new revision.
	 *
	 * A new revision will be created if:
	 * - the last revisions serialized-snapshot differs from the current (i.e. provided) entity.
	 * - no previous revisions exist for the provided entity.
	 *
	 * @param entity the entity that will have a new revision.
	 */
	public void createRevision(ErrandEntity entity) {
		revisionRepository.findFirstByEntityIdOrderByVersionDesc(entity.getId()).ifPresentOrElse(
			latestRevision -> {
				if (!jsonEquals(toSerializedSnapshot(entity), latestRevision.getSerializedSnapshot())) {
					// Differences exists, create new revision <lastRevision.version + 1>
					revisionRepository.save(toRevisionEntity(entity, latestRevision.getVersion() + 1));
				}
			},
			() -> // No previous revisions exist. Create revision 0
			revisionRepository.save(toRevisionEntity(entity, 0)));
	}

	private boolean jsonEquals(String currentSnapshot, String previousSnapshot) {
		if (anyNull(currentSnapshot, previousSnapshot)) {
			return false;
		}

		try {
			final var currentJson = JsonPath.using(JSONPATH_CONFIG).parse(currentSnapshot);
			final var previousJson = JsonPath.using(JSONPATH_CONFIG).parse(previousSnapshot);

			EXCLUDED_ATTRIBUTES.forEach(excludedAttribute -> {
				currentJson.delete(excludedAttribute);
				previousJson.delete(excludedAttribute);
			});

			return objectMapper.readTree(currentJson.jsonString()).equals(objectMapper.readTree(previousJson.jsonString()));
		} catch (Exception e) { // If something fails, log and return the json objects as unequal to force creation of a new revision
			LOG.error(ERROR_MESSAGE, e);
			return false;
		}
	}
}
