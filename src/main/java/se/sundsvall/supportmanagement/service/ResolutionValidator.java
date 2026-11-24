package se.sundsvall.supportmanagement.service;

import java.util.Optional;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.ResolutionCodeRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static se.sundsvall.supportmanagement.model.enums.EntityType.RESOLUTION_CODE;
import static se.sundsvall.supportmanagement.model.enums.ProtectedResource.ERRAND;

/**
 * Judges the resolution of an errand against the resolution codes its namespace has registered, for the
 * ValidResolution annotation on the errand and on the source handling of a handover.
 * <p>
 * Like the other metadata, the codes are only enforced once the namespace config has turned validation on for
 * RESOLUTION_CODE, so that a namespace can register its codes before it is held to them. A resolution that is absent,
 * or empty and so clears the resolution of the errand, is left alone, and so is the resolution the errand already
 * holds, so that a code deprecated since then does not stop the rest of a patch.
 */
@Component
public class ResolutionValidator {

	private final MetadataService metadataService;
	private final ResolutionCodeRepository resolutionCodeRepository;
	private final AccessControlService accessControlService;

	ResolutionValidator(final MetadataService metadataService, final ResolutionCodeRepository resolutionCodeRepository, final AccessControlService accessControlService) {
		this.metadataService = metadataService;
		this.resolutionCodeRepository = resolutionCodeRepository;
		this.accessControlService = accessControlService;
	}

	/**
	 * @param  resolution     the resolution to judge
	 * @param  namespace      the namespace whose codes it is held to
	 * @param  municipalityId the municipality whose codes it is held to
	 * @param  errandId       the errand the resolution is set on, or null when the errand is yet to be created
	 * @return                true if the resolution may be set
	 */
	public boolean isValid(final String resolution, final String namespace, final String municipalityId, final String errandId) {
		return isEmpty(resolution)
			|| !metadataService.isValidated(namespace, municipalityId, RESOLUTION_CODE)
			|| resolutionCodeRepository.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(namespace, municipalityId, resolution)
			|| isHeldByErrand(resolution, namespace, municipalityId, errandId);
	}

	private boolean isHeldByErrand(final String resolution, final String namespace, final String municipalityId, final String errandId) {
		return ofNullable(errandId)
			.flatMap(id -> findChangeableErrand(namespace, municipalityId, id))
			.map(ErrandEntity::getResolution)
			.filter(resolution::equals)
			.isPresent();
	}

	/**
	 * The errand is read with the access both a patch and a handover require, so that the verdict tells a caller nothing
	 * about an errand they may not change: an errand that is missing or out of reach counts as not holding the resolution,
	 * and the request is refused for it before the service would refuse it for access.
	 */
	private Optional<ErrandEntity> findChangeableErrand(final String namespace, final String municipalityId, final String errandId) {
		try {
			return ofNullable(accessControlService.getErrand(namespace, municipalityId, errandId, false, ERRAND, RW));
		} catch (final ThrowableProblem _) {
			return Optional.empty();
		}
	}
}
