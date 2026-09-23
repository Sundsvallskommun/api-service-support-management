package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Component
public class LabelMoveWorker {

	private static final Logger LOG = LoggerFactory.getLogger(LabelMoveWorker.class);

	private final ErrandsRepository errandsRepository;
	private final MetadataLabelRepository metadataLabelRepository;
	private final ErrandService errandService;

	LabelMoveWorker(final ErrandsRepository errandsRepository, final MetadataLabelRepository metadataLabelRepository, final ErrandService errandService) {
		this.errandsRepository = errandsRepository;
		this.metadataLabelRepository = metadataLabelRepository;
		this.errandService = errandService;
	}

	/**
	 * Re-computes the label set of all errands that reference the moved label, using the already
	 * re-parented MetadataLabelEntity tree. Access labels (leaves) are kept as-is; the full
	 * ancestor chain for each leaf is re-derived by walking getParent() on the live entity tree.
	 *
	 * @param  movedLabelId the label that was moved.
	 * @return              the ids of the errands that kept the labels they had: those without access labels to rebuild
	 *                      from, and those the move would have moved off the process they run.
	 */
	public List<String> migrateErrandsForMovedLabel(final String movedLabelId) {
		final var kept = errandsRepository.findAllByLabelsMetadataLabelId(movedLabelId).stream()
			.filter(errand -> !rebuildLabels(errand))
			.map(ErrandEntity::getId)
			.toList();

		if (!kept.isEmpty()) {
			LOG.warn("{} errands referencing the moved label {} kept the labels they had: {}", kept.size(), sanitizeForLogging(movedLabelId), kept);
		}

		return kept;
	}

	/**
	 * Rebuilds the labels of the errand as the ancestor chains of its access labels (the leaves), and persists them
	 * through {@link ErrandService#persistLabelUpdate}, which leaves an errand the change would move off its process as it
	 * is.
	 * <p>
	 * An errand without access labels is left untouched, with a warning, and keeps the labels it has.
	 *
	 * @param  errand the errand to rebuild the labels of.
	 * @return        true when the rebuilt labels were put on the errand, false when it kept the labels it had.
	 */
	boolean rebuildLabels(final ErrandEntity errand) {
		var accessLabels = ofNullable(errand.getAccessLabels()).orElse(emptyList());

		if (accessLabels.isEmpty()) {
			LOG.warn("Errand {} references the moved label but has no access labels to rebuild from - left untouched", errand.getId());
			return false;
		}

		var leafIds = accessLabels.stream()
			.map(AccessLabelEmbeddable::getMetadataLabelId)
			.toList();

		var labelEntities = metadataLabelRepository.findAllById(leafIds);
		return errandService.persistLabelUpdate(errand, buildAncestorChain(labelEntities));
	}

	private static List<ErrandLabelEmbeddable> buildAncestorChain(final List<MetadataLabelEntity> leaves) {
		var seen = new HashSet<String>();
		var result = new ArrayList<ErrandLabelEmbeddable>();

		for (var leaf : leaves) {
			walkAncestors(leaf, seen, result);
		}

		return result;
	}

	private static void walkAncestors(final MetadataLabelEntity start, final HashSet<String> seen, final List<ErrandLabelEmbeddable> result) {
		var current = start;
		var visited = new HashSet<String>();

		while (current != null) {
			var id = current.getId();
			if (id == null || !visited.add(id)) {
				break;
			}
			if (seen.add(id)) {
				result.add(ErrandLabelEmbeddable.create().withMetadataLabelId(id));
			}
			current = current.getParent();
		}
	}
}
