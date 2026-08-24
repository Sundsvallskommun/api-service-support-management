package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Validates the metadata a measure refers to against the namespace it belongs to.
 * <p>
 * Measures reach the service through two entry points, on their own resource and as part of the errand, and both must
 * reach the same verdict on the same data. Keeping the rules here rather than in either service is what stops the two
 * from drifting apart, which is how an unknown role once got in through the errand while being rejected on the measure
 * resource.
 * <p>
 * A null field is left alone, since a patch says nothing about the fields it omits. Whether a field may be absent
 * altogether is a bean validation concern and is declared on {@link Measure}.
 */
@Component
public class MeasureValidator {

	private static final String BAD_MEASURE_TYPE = "'%s' is not a valid measure type for namespace '%s' and municipality with id '%s'";
	private static final String BAD_ROLE = "'%s' is not a valid role for namespace '%s' and municipality with id '%s'";

	private final MeasureTypeRepository measureTypeRepository;
	private final RoleRepository roleRepository;

	MeasureValidator(final MeasureTypeRepository measureTypeRepository, final RoleRepository roleRepository) {
		this.measureTypeRepository = measureTypeRepository;
		this.roleRepository = roleRepository;
	}

	public void validate(final List<Measure> measures, final String namespace, final String municipalityId) {
		ofNullable(measures).orElse(emptyList())
			.forEach(measure -> validate(measure, namespace, municipalityId));
	}

	public void validate(final Measure measure, final String namespace, final String municipalityId) {
		ofNullable(measure).ifPresent(value -> {
			validateMeasureType(value.getType(), namespace, municipalityId);
			validateRole(value.getAddedByRole(), namespace, municipalityId);
		});
	}

	private void validateMeasureType(final String type, final String namespace, final String municipalityId) {
		ofNullable(type).ifPresent(value -> {
			if (!measureTypeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, value)) {
				throw Problem.valueOf(BAD_REQUEST, BAD_MEASURE_TYPE.formatted(value, namespace, municipalityId));
			}
		});
	}

	private void validateRole(final String role, final String namespace, final String municipalityId) {
		ofNullable(role).ifPresent(value -> {
			if (!roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, value)) {
				throw Problem.valueOf(BAD_REQUEST, BAD_ROLE.formatted(value, namespace, municipalityId));
			}
		});
	}
}
