package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.HashMap;
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
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;

/**
 * Settles the labels of an errand, and what they mean for who may reach it.
 * <p>
 * Held apart from {@link ErrandService} because every step of it reads the label tree from the database: the versions a
 * request carries are held against the stored ones, an errand labelled with a leaf is expanded to the ancestors of that
 * leaf, and the access labels are the labels left once the ancestors are taken out again. None of it can be settled
 * from the errand alone, which is why it is not the work of a mapper.
 */
@Service
public class ErrandLabelService {

	private final MetadataLabelRepository metadataLabelRepository;

	public ErrandLabelService(final MetadataLabelRepository metadataLabelRepository) {
		this.metadataLabelRepository = metadataLabelRepository;
	}

	/**
	 * Expands the labels of the errand to their ancestors, and settles from them which labels decide who reaches it.
	 * <p>
	 * Run once the errand carries the labels it is going to keep, since both answers are read off them.
	 *
	 * @param errandEntity errand whose labels have been set
	 */
	public void settleAccessLabels(final ErrandEntity errandEntity) {
		expandLabelsToAncestorChain(errandEntity);
		computeAndSetAccessLabels(errandEntity);
	}

	public void validateVersions(final List<ErrandLabel> labels) {
		var labelsWithVersion = ofNullable(labels).orElse(emptyList()).stream()
			.filter(label -> label.getVersion() != null)
			.toList();

		if (labelsWithVersion.isEmpty()) {
			return;
		}

		var labelIds = labelsWithVersion.stream().map(ErrandLabel::getId).toList();
		var currentVersionById = metadataLabelRepository.findAllById(labelIds).stream()
			.collect(HashMap::new, (m, e) -> m.put(e.getId(), e.getVersion()), HashMap::putAll);

		labelsWithVersion.stream()
			.filter(label -> {
				var current = currentVersionById.get(label.getId());
				return current != null && !current.equals(label.getVersion());
			})
			.findFirst()
			.ifPresent(label -> {
				throw Problem.valueOf(PRECONDITION_FAILED,
					"Label with id '%s' has been modified — expected version %d but current version is %d"
						.formatted(label.getId(), label.getVersion(), currentVersionById.get(label.getId())));
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

	private void computeAndSetAccessLabels(final ErrandEntity errandEntity) {
		final var allLabelIds = ofNullable(errandEntity.getLabels())
			.orElse(emptyList())
			.stream()
			.map(ErrandLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet());

		if (allLabelIds.isEmpty()) {
			errandEntity.setAccessLabels(new ArrayList<>());
			return;
		}

		// Repository lookup is needed because ErrandLabelEmbeddable's @ManyToOne metadataLabel
		// is only populated by Hibernate on load. For freshly created/updated labels (from the mapper),
		// getMetadataLabel() returns null.
		final var resourcePathById = metadataLabelRepository.findAllById(allLabelIds).stream()
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
