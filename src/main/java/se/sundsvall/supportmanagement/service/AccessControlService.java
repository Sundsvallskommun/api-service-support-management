package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.hasAllowedMetadataLabels;

import generated.se.sundsvall.accessmapper.Access;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

@Component
public class AccessControlService {

	private final AccessMapperService accessMapperService;
	private final NamespaceConfigService namespaceConfigService;

	public AccessControlService(final AccessMapperService accessMapperService, final NamespaceConfigService namespaceConfigService) {
		this.accessMapperService = accessMapperService;
		this.namespaceConfigService = namespaceConfigService;
	}

	/**
	 * Assumes user has R, RW or LR for ErrandEntity. This predicate only determines full or limited mapping If access
	 * control is not active in namespace, full mapping will occur
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipalityId
	 * @param  user           user
	 * @return                predicate for full or limited mapping
	 */
	public Predicate<ErrandEntity> limitedMappingPredicateByLabel(String namespace, String municipalityId, Identifier user) {
		if (hasAccessControlActive(namespace, municipalityId)) {
			// Filter out all labels that is read or read/write. R/RW has precedence over LR.
			var fullReadMetadataLabels = accessMapperService.getAccessibleLabels(municipalityId, namespace, user, List.of(R, RW));

			// If ALL errand labels is a subset of R/RW fullReadMetadataLabels the errand should not be mapped as limited
			return errandEntity -> !fullReadMetadataLabels.containsAll(errandEntity.getLabels().stream()
				.map(ErrandLabelEmbeddable::getMetadataLabel).collect(Collectors.toSet()));
		} else {
			return errandEntity -> false;
		}
	}

	/**
	 * Creates specification filter ensuring user has access.
	 *
	 * @param  namespace        namespace
	 * @param  municipalityId   municipality id
	 * @param  user             user
	 * @param  accessLevelEnums filters access level. Defaults to { LR, R, RW } if left empty.
	 * @return                  specification if access control is enabled on namespace, conjunction otherwise
	 */
	public Specification<ErrandEntity> withAccessControl(String namespace, String municipalityId, Identifier user, Access.AccessLevelEnum... accessLevelEnums) {
		if (hasAccessControlActive(namespace, municipalityId)) {
			var filter = accessLevelEnums.length == 0 ? List.of(LR, R, RW) : Arrays.stream(accessLevelEnums).toList();
			return hasAllowedMetadataLabels(accessMapperService.getAccessibleLabels(municipalityId, namespace, user, filter));
		} else {
			return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		}
	}

	private boolean hasAccessControlActive(String namespace, String municipalityId) {
		return namespaceConfigService.get(namespace, municipalityId).isAccessControl();
	}
}
