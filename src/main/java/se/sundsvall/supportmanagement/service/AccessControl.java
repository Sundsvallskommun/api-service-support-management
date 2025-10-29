package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.AccessGroup;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;

@Component
public class AccessControl {

	private static final String ACCESS_TYPE_LABEL = "label";
	private final MetadataService metadataService;

	public AccessControl(MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	// Assumes accessGroups has R, RW or LR for ErrandEntity. This predicate only determines full or limited mapping
	public Predicate<ErrandEntity> limitedMappingPredicateByLabel(String municipalityId, String namespace, List<AccessGroup> accessGroups) {

		// Filter out all access (with type label) that is read or read/write. R/RW has precedence over LR.
		var fullReadPatterns = accessGroups.stream().flatMap(accessGroup -> accessGroup.getAccessByType().stream())
			.filter(accessType -> ACCESS_TYPE_LABEL.equals(accessType.getType()))
			.flatMap(accessType -> accessType.getAccess().stream())
			.filter(access -> R.equals(access.getAccessLevel()) || RW.equals(access.getAccessLevel()))
			.map(Access::getPattern)
			.toList();

		// Convert patterns to labels
		var fullReadMetadataLabels = metadataService.patternToLabels(namespace, municipalityId, fullReadPatterns);

		// If ALL errand labels is a subset of R/RW fullReadMetadataLabels the errand should not be mapped as limited
		return errandEntity -> !fullReadMetadataLabels.containsAll(errandEntity.getLabels().stream()
			.map(ErrandLabelEmbeddable::getMetadataLabel).collect(Collectors.toSet()));
	}

}
