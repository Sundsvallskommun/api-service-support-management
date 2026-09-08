package se.sundsvall.supportmanagement.service;

import java.util.HashSet;
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

/** Shared registration and update rules for both the protected measure resource and embedded errand writes. */
@Component
public class MeasureValidator {

	private final MeasureTypeRepository measureTypeRepository;
	private final RoleRepository roleRepository;
	private final AccessControlService accessControlService;

	MeasureValidator(final MeasureTypeRepository measureTypeRepository, final RoleRepository roleRepository, final AccessControlService accessControlService) {
		this.measureTypeRepository = measureTypeRepository;
		this.roleRepository = roleRepository;
		this.accessControlService = accessControlService;
	}

	public void validate(final List<Measure> measures, final String namespace, final String municipalityId) {
		ofNullable(measures).orElse(emptyList()).forEach(measure -> validate(measure, namespace, municipalityId));
	}

	/** The requesting identity owns attribution. Client-supplied attribution must agree with it. */
	public void validate(final Measure measure, final String namespace, final String municipalityId) {
		if (measure == null) {
			throw Problem.valueOf(BAD_REQUEST, "A measure entry must not be null");
		}
		validateTypeForRole(measure.getMeasureTypeId(), measure.getAddedByRole(), namespace, municipalityId);
		validateDates(measure, null);
		measure.setAddedByUser(accessControlService.verifyMeasureCreator(namespace, municipalityId, measure.getAddedByUser(), measure.getAddedByRole()));
	}

	/** An unchanged historical type remains editable; changing type must satisfy the current registration rules. */
	public void validateUpdate(final Measure measure, final MeasureEntity existing, final String namespace, final String municipalityId) {
		if (measure.getAddedByUser() != null && !Objects.equals(measure.getAddedByUser(), existing.getAddedByUser()) ||
			measure.getAddedByRole() != null && !Objects.equals(measure.getAddedByRole(), existing.getAddedByRole())) {
			throw Problem.valueOf(BAD_REQUEST, "Measure creator and registration role cannot be changed");
		}
		if (measure.getMeasureTypeId() != null && !Objects.equals(measure.getMeasureTypeId(), existing.getMeasureTypeId())) {
			validateTypeForRole(measure.getMeasureTypeId(), existing.getAddedByRole(), namespace, municipalityId);
		}
		validateDates(measure, existing);
	}

	public void validateUpdate(final List<Measure> measures, final List<MeasureEntity> existing, final String namespace, final String municipalityId) {
		final var seenIds = new HashSet<String>();
		for (final var measure : ofNullable(measures).orElse(emptyList())) {
			if (measure == null) {
				throw Problem.valueOf(BAD_REQUEST, "A measure entry must not be null");
			}
			if (measure.getId() == null) {
				validate(measure, namespace, municipalityId);
				continue;
			}
			if (!seenIds.add(measure.getId())) {
				throw Problem.valueOf(BAD_REQUEST, "A measure ID may only occur once in an errand update");
			}
			final var saved = ofNullable(existing).orElse(emptyList()).stream()
				.filter(candidate -> Objects.equals(candidate.getId(), measure.getId()))
				.findFirst().orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "The measure ID does not belong to this errand"));
			validateUpdate(measure, saved, namespace, municipalityId);
		}
	}

	private void validateTypeForRole(final String measureTypeId, final String roleName, final String namespace, final String municipalityId) {
		if (measureTypeId == null || roleName == null) {
			throw Problem.valueOf(BAD_REQUEST, "Measure type ID and registration role are required");
		}
		final var type = measureTypeRepository.findByIdAndNamespaceAndMunicipalityId(measureTypeId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "Measure type does not exist in this municipality and namespace"));
		final var role = roleRepository.findByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, roleName)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "Registration role does not exist in this municipality and namespace"));
		if (type.isDeprecated() || role.isDeprecated() || !type.getAllowedRoleIds().contains(role.getId())) {
			throw Problem.valueOf(BAD_REQUEST, "Select an active measure type assigned to the active registration role");
		}
	}

	private void validateDates(final Measure measure, final MeasureEntity existing) {
		final var start = ofNullable(measure.getPlannedStart()).orElseGet(() -> existing == null ? null : existing.getPlannedStart());
		final var complete = ofNullable(measure.getPlannedComplete()).orElseGet(() -> existing == null ? null : existing.getPlannedComplete());
		if (start != null && complete != null && complete.isBefore(start)) {
			throw Problem.valueOf(BAD_REQUEST, "Planned completion must not precede planned start");
		}
	}
}
