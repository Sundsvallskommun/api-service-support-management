package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.ResolutionCodeRepository;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Validates the resolution of an errand against the resolution codes its namespace has registered.
 * <p>
 * Used both when an errand is created or updated and when a handover closes the source errand, so that both reach the
 * same verdict on the same data.
 * <p>
 * Only a namespace that has registered codes is held to them. A resolution that is absent, or empty and so clears the
 * resolution of the errand, is left alone.
 */
@Component
public class ResolutionValidator {

	private static final String BAD_RESOLUTION = "'%s' is not a valid resolution for namespace '%s' and municipality with id '%s'";

	private final ResolutionCodeRepository resolutionCodeRepository;

	ResolutionValidator(final ResolutionCodeRepository resolutionCodeRepository) {
		this.resolutionCodeRepository = resolutionCodeRepository;
	}

	public void validate(final String resolution, final String namespace, final String municipalityId) {
		ofNullable(resolution)
			.filter(value -> !isEmpty(value))
			.filter(_ -> resolutionCodeRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId))
			.filter(value -> !resolutionCodeRepository.existsByNamespaceAndMunicipalityIdAndNameAndDeprecatedFalse(namespace, municipalityId, value))
			.ifPresent(value -> {
				throw Problem.valueOf(BAD_REQUEST, BAD_RESOLUTION.formatted(value, namespace, municipalityId));
			});
	}
}
