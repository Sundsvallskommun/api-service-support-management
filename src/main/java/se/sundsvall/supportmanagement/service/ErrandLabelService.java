package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.ErrandLabel;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

/**
 * Settles the labels of an errand, and what they mean for who may reach it.
 * <p>
 * Held apart from {@link ErrandService} because every step of it reads the label tree from the database: the labels a
 * request names are held to the namespace and to the versions stored, an errand labelled with a leaf is expanded to the
 * ancestors of that leaf, and the access labels are the labels left once the ancestors are taken out again. None of it
 * can be settled from the errand alone, which is why it is not the work of a mapper.
 */
@Service
public class ErrandLabelService {

	private static final String MISSING_LABEL_ID = "Every label must carry the id of the label it refers to";
	private static final String UNKNOWN_LABEL = "Label with id '%s' does not exist in namespace '%s' for municipality '%s'";
	private static final String MOVED_VERSION = "Label with id '%s' has been modified — expected version %d but current version is %d";

	private final MetadataLabelRepository metadataLabelRepository;

	public ErrandLabelService(final MetadataLabelRepository metadataLabelRepository) {
		this.metadataLabelRepository = metadataLabelRepository;
	}

	/**
	 * Expands the labels of the errand to their ancestors, gives every label the metadata it points at, and settles from
	 * them which labels decide who reaches it.
	 * <p>
	 * Run once the errand carries the labels it is going to keep, since every answer is read off them. The metadata is
	 * filled in because Hibernate does so only when the errand is read, and a label the request has just set would
	 * otherwise answer with nothing but its id - in the response to the very write that set it.
	 *
	 * @param errandEntity errand whose labels have been set
	 */
	public void settleAccessLabels(final ErrandEntity errandEntity) {
		expandLabelsToAncestorChain(errandEntity);

		final var labelsById = lookUpLabelsOf(errandEntity);

		attachMetadataLabels(errandEntity, labelsById);
		computeAndSetAccessLabels(errandEntity, labelsById);
	}

	/**
	 * Holds the labels a request names to the namespace the errand lives in, and to the versions the request says they
	 * are at.
	 * <p>
	 * A label is referred to by its id alone, and the id reaches a label in any namespace. Taken as it stands, an errand
	 * could be given a label of another namespace - and with it the access rules and the process key of that namespace.
	 * An id that names no label of the namespace is refused before anything is written, the same way whether the label
	 * does not exist or belongs elsewhere, so that the answer says nothing about other namespaces. A label without an id
	 * is refused as well, and a label whose version has moved on is answered with 412.
	 *
	 * @param namespace      the namespace of the errand.
	 * @param municipalityId the municipality of the errand.
	 * @param labels         the labels the request names, or null when it names none.
	 */
	public void validateLabels(final String namespace, final String municipalityId, final List<ErrandLabel> labels) {
		final var requested = ofNullable(labels).orElse(emptyList());

		if (requested.isEmpty()) {
			return;
		}

		if (requested.stream().anyMatch(label -> isNull(label) || isNull(label.getId()))) {
			throw Problem.valueOf(BAD_REQUEST, MISSING_LABEL_ID);
		}

		final var labelsById = metadataLabelRepository.findAllById(requested.stream().map(ErrandLabel::getId).collect(Collectors.toSet())).stream()
			.filter(label -> namespace.equals(label.getNamespace()) && municipalityId.equals(label.getMunicipalityId()))
			.collect(Collectors.toMap(MetadataLabelEntity::getId, identity(), (first, _) -> first));

		requested.stream()
			.filter(label -> !labelsById.containsKey(label.getId()))
			.findFirst()
			.ifPresent(label -> {
				throw Problem.valueOf(BAD_REQUEST, UNKNOWN_LABEL.formatted(label.getId(), namespace, municipalityId));
			});

		requested.stream()
			.filter(label -> nonNull(label.getVersion()))
			.filter(label -> {
				final var current = labelsById.get(label.getId()).getVersion();
				return nonNull(current) && !current.equals(label.getVersion());
			})
			.findFirst()
			.ifPresent(label -> {
				throw Problem.valueOf(PRECONDITION_FAILED, MOVED_VERSION.formatted(label.getId(), label.getVersion(), labelsById.get(label.getId()).getVersion()));
			});
	}

	void expandLabelsToAncestorChain(final ErrandEntity errandEntity) {
		var currentIds = ofNullable(errandEntity.getLabels()).orElse(emptyList()).stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet());

		if (currentIds.isEmpty()) {
			return;
		}

		var ancestorPaths = metadataLabelRepository.findAllById(currentIds).stream()
			.map(MetadataLabelEntity::getResourcePath)
			.flatMap(path -> ancestorResourcePaths(path).stream())
			.collect(Collectors.toSet());

		if (ancestorPaths.isEmpty()) {
			return;
		}

		var missingAncestors = metadataLabelRepository
			.findByNamespaceAndMunicipalityIdAndResourcePathIn(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), ancestorPaths)
			.stream()
			.filter(a -> !currentIds.contains(a.getId()))
			.map(a -> ErrandLabelEmbeddable.create().withMetadataLabelId(a.getId()))
			.toList();

		if (missingAncestors.isEmpty()) {
			return;
		}

		var expanded = new ArrayList<>(errandEntity.getLabels());
		expanded.addAll(missingAncestors);
		errandEntity.setLabels(expanded);
	}

	private static Set<String> ancestorResourcePaths(final String resourcePath) {
		var parts = resourcePath.split("/");
		var paths = new HashSet<String>();
		var sb = new StringBuilder();
		for (int i = 0; i < parts.length - 1; i++) {
			if (i > 0) {
				sb.append("/");
			}
			sb.append(parts[i]);
			paths.add(sb.toString());
		}
		return paths;
	}

	/**
	 * The labels the errand wears, read by id: a label the mapper has just put together carries no metadata label of its
	 * own, so the errand cannot answer for them.
	 */
	private Map<String, MetadataLabelEntity> lookUpLabelsOf(final ErrandEntity errandEntity) {
		final var ids = labelIdsOf(errandEntity);

		if (ids.isEmpty()) {
			return Map.of();
		}

		return metadataLabelRepository.findAllById(ids).stream()
			.collect(Collectors.toMap(MetadataLabelEntity::getId, identity(), (first, _) -> first));
	}

	private static void attachMetadataLabels(final ErrandEntity errandEntity, final Map<String, MetadataLabelEntity> labelsById) {
		ofNullable(errandEntity.getLabels()).orElse(emptyList()).stream()
			.filter(label -> isNull(label.getMetadataLabel()))
			.forEach(label -> label.setMetadataLabel(labelsById.get(label.getMetadataLabelId())));
	}

	private static Set<String> labelIdsOf(final ErrandEntity errandEntity) {
		return ofNullable(errandEntity.getLabels()).orElse(emptyList()).stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet());
	}

	private static void computeAndSetAccessLabels(final ErrandEntity errandEntity, final Map<String, MetadataLabelEntity> labelsById) {
		final var allLabelIds = labelIdsOf(errandEntity);

		if (allLabelIds.isEmpty()) {
			errandEntity.setAccessLabels(new ArrayList<>());
			return;
		}

		final var resourcePathById = labelsById.values().stream()
			.filter(label -> nonNull(label.getResourcePath()))
			.collect(Collectors.toMap(MetadataLabelEntity::getId, MetadataLabelEntity::getResourcePath));

		final var ancestorIds = resourcePathById.entrySet().stream()
			.filter(entry -> resourcePathById.values().stream()
				.anyMatch(otherPath -> !otherPath.equals(entry.getValue()) && otherPath.startsWith(entry.getValue() + "/")))
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		final var accessLabels = allLabelIds.stream()
			.filter(id -> !ancestorIds.contains(id))
			.map(id -> AccessLabelEmbeddable.create().withMetadataLabelId(id))
			.collect(Collectors.toCollection(ArrayList::new));

		errandEntity.setAccessLabels(accessLabels);
	}
}
