package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** The same reference and attribution rules apply to individual measures and measures carried on an errand. */
@Component
public class MeasureValidator {

	private static final String BAD_MEASURE_TYPE = "'%s' is not a valid measure type for namespace '%s' and municipality with id '%s'";
	private static final String BAD_ROLE = "'%s' is not a valid role for namespace '%s' and municipality with id '%s'";

	private final MeasureTypeRepository measureTypeRepository;
	private final RoleRepository roleRepository;
	private final AccessControlService accessControlService;

	MeasureValidator(final MeasureTypeRepository measureTypeRepository, final RoleRepository roleRepository, final AccessControlService accessControlService) {
		this.measureTypeRepository = measureTypeRepository;
		this.roleRepository = roleRepository;
		this.accessControlService = accessControlService;
	}

	public void validate(final List<Measure> measures, final String namespace, final String municipalityId) {
		validate(measures, emptyList(), namespace, municipalityId);
	}

	public void validate(final List<Measure> measures, final List<MeasureEntity> existing, final String namespace, final String municipalityId) {
		for (final var measure : ofNullable(measures).orElse(emptyList())) {
			final var previous = ofNullable(existing).orElse(emptyList()).stream()
				.filter(entity -> measure.getId() != null && measure.getId().equals(entity.getId()))
				.findFirst().orElse(null);
			validate(measure, previous, namespace, municipalityId);
		}
	}

	public void validate(final Measure measure, final String namespace, final String municipalityId) {
		validate(measure, null, namespace, municipalityId);
	}

	public void validate(final Measure measure, final MeasureEntity existing, final String namespace, final String municipalityId) {
		if (measure == null) {
			return;
		}
		if (measure.hasField(Measure.Field.TYPE) && (measure.getType() == null || measure.getType().isBlank())) {
			throw Problem.valueOf(BAD_REQUEST, "Measure type must not be null or blank");
		}
		validateMeasureType(measure.getType(), existing, namespace, municipalityId);
		if (existing == null) {
			validateRole(measure.getAddedByRole(), namespace, municipalityId);
			accessControlService.verifyMeasureCreator(namespace, municipalityId, measure.getAddedByUser(), measure.getAddedByRole());
		} else {
			validateUnchanged("addedByUser", measure.hasField(Measure.Field.ADDED_BY_USER), measure.getAddedByUser(), existing.getAddedByUser());
			validateUnchanged("addedByRole", measure.hasField(Measure.Field.ADDED_BY_ROLE), measure.getAddedByRole(), existing.getAddedByRole());
		}
	}

	private void validateMeasureType(final String type, final MeasureEntity existing, final String namespace, final String municipalityId) {
		// Omitting type leaves the existing selection untouched.
		if (type == null) {
			return;
		}
		final var metadata = measureTypeRepository.findWithLockingByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, type)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_MEASURE_TYPE.formatted(type, namespace, municipalityId)));
		if (metadata.isDeprecated() && (existing == null || !Objects.equals(type, existing.getType()))) {
			throw Problem.valueOf(BAD_REQUEST, "Measure type '%s' is deprecated and cannot be selected".formatted(type));
		}
	}

	private void validateRole(final String role, final String namespace, final String municipalityId) {
		if (role != null && !roleRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, role)) {
			throw Problem.valueOf(BAD_REQUEST, BAD_ROLE.formatted(role, namespace, municipalityId));
		}
	}

	private void validateUnchanged(final String field, final boolean supplied, final String value, final String previous) {
		if (supplied && !Objects.equals(value, previous)) {
			throw Problem.valueOf(BAD_REQUEST, "Measure %s cannot be changed after creation".formatted(field));
		}
	}
}
