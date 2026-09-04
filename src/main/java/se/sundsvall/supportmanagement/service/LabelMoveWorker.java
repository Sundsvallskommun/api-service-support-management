package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.MetadataLabelRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

@Component
public class LabelMoveWorker {

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
	 */
	public void migrateErrandsForMovedLabel(final String movedLabelId) {
		errandsRepository.findAllByLabelsMetadataLabelId(movedLabelId)
			.forEach(this::rebuildLabels);
	}

	void rebuildLabels(final ErrandEntity errand) {
		var leafIds = errand.getAccessLabels().stream()
			.map(a -> a.getMetadataLabelId())
			.toList();

		var labelEntities = metadataLabelRepository.findAllById(leafIds);
		var newLabels = buildAncestorChain(labelEntities);
		errand.setLabels(newLabels);
		errandService.persistLabelUpdate(errand);
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
