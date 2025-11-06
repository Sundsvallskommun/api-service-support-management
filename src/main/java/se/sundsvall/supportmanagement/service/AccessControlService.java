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
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;

@Component
public class AccessControlService {

	private final NamespaceConfigRepository namespaceConfigRepository;
	private final AccessMapperService accessMapperService;

	public AccessControlService(final NamespaceConfigRepository namespaceConfigRepository, final AccessMapperService accessMapperService) {
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.accessMapperService = accessMapperService;
	}

	// Assumes user has R, RW or LR for ErrandEntity. This predicate only determines full or limited mapping
	public Predicate<ErrandEntity> limitedMappingPredicateByLabel(String municipalityId, String namespace, Identifier user) {

		// Filter out all labels that is read or read/write. R/RW has precedence over LR.
		var fullReadMetadataLabels = accessMapperService.getAccessibleLabels(municipalityId, namespace, user, List.of(R, RW));

		// If ALL errand labels is a subset of R/RW fullReadMetadataLabels the errand should not be mapped as limited
		return errandEntity -> !fullReadMetadataLabels.containsAll(errandEntity.getLabels().stream()
			.map(ErrandLabelEmbeddable::getMetadataLabel).collect(Collectors.toSet()));
	}

	public Specification<ErrandEntity> withAccessControl(String namespace, String municipalityId, Identifier user, Access.AccessLevelEnum... accessLevelEnums) {

		var accessControlActive = namespaceConfigRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(NamespaceConfigEntity::getAccessControl)
			.orElse(false);

		if (accessControlActive) {
			var filter = accessLevelEnums.length == 0 ? List.of(LR, R, RW) : Arrays.stream(accessLevelEnums).toList();
			return hasAllowedMetadataLabels(accessMapperService.getAccessibleLabels(municipalityId, namespace, user, filter));
		} else {
			return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		}
	}
}
