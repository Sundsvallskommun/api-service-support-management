package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.lang.Boolean.FALSE;
import static org.zalando.problem.Status.NOT_FOUND;
import static org.zalando.problem.Status.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.hasAllowedMetadataLabels;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

import generated.se.sundsvall.accessmapper.Access;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

@Component
public class AccessControlService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ENTITY_NOT_ACCESSIBLE = "Errand not accessible by user '%s'";

	private final AccessMapperService accessMapperService;
	private final NamespaceConfigService namespaceConfigService;
	private final ErrandsRepository errandsRepository;

	public AccessControlService(final AccessMapperService accessMapperService, final NamespaceConfigService namespaceConfigService, final ErrandsRepository errandsRepository) {
		this.accessMapperService = accessMapperService;
		this.namespaceConfigService = namespaceConfigService;
		this.errandsRepository = errandsRepository;
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

	/**
	 * Fetches ErrandEntity and checks user access, if enabled in namespace.
	 *
	 * @param  namespace              namespace
	 * @param  municipalityId         municipality id
	 * @param  id                     errand id
	 * @param  lock                   db row locking enable if true
	 * @param  accessLevelEnumsFilter filters errands base on user access level for specific errand. If empty, no filtering
	 *                                occurs (fetches errand if any level exists)
	 * @return                        errand entity
	 */
	public ErrandEntity getErrand(final String namespace, final String municipalityId, final String id, boolean lock, Access.AccessLevelEnum... accessLevelEnumsFilter) {
		verifyExistingErrand(id, namespace, municipalityId, lock);
		return errandsRepository
			.findOne(withId(id).and(withAccessControl(namespace, municipalityId, Identifier.get(), accessLevelEnumsFilter)))
			.orElseThrow(() -> Problem.valueOf(UNAUTHORIZED, ENTITY_NOT_ACCESSIBLE.formatted(Optional.ofNullable(Identifier.get())
				.map(Identifier::getValue)
				.orElse(null))));
	}

	private void verifyExistingErrand(final String id, final String namespace, final String municipalityId, final boolean lock) {

		final Supplier<Boolean> exists;
		if (lock) {
			exists = () -> errandsRepository.existsWithLockingByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		} else {
			exists = () -> errandsRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		}

		if (FALSE.equals(exists.get())) {
			throw Problem.valueOf(NOT_FOUND, ENTITY_NOT_FOUND.formatted(id, namespace, municipalityId));
		}
	}
}
