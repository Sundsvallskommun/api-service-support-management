package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.Measure;
import se.sundsvall.supportmanagement.api.model.errand.Measure.Field;
import se.sundsvall.supportmanagement.integration.db.MeasureTypeRepository;
import se.sundsvall.supportmanagement.integration.db.RoleRepository;
import se.sundsvall.supportmanagement.integration.db.model.MeasureEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Validates the metadata a measure refers to, and the attribution it carries, against the namespace it belongs to.
 * <p>
 * Measures reach the service through two entry points, on their own resource and as part of the errand, and both must
 * reach the same verdict on the same data. Keeping the rules here rather than in either service is what stops the two
 * from drifting apart, which is how an unknown role once got in through the errand while being rejected on the measure
 * resource.
 * <p>
 * A patch says nothing about the fields it omits, so an omitted field is left alone. A field the patch does supply is
 * checked even when it is null, which is how clearing the type or the creator is refused. Whether a field may be absent
 * altogether is a bean validation concern and is declared on {@link Measure}.
 * <p>
 * The type is read under a shared lock, so that the catalogue cannot delete it between this check and the commit of the
 * measure. See {@link MeasureTypeRepository#findWithSharedLockByNamespaceAndMunicipalityIdAndName}.
 */
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

	/**
	 * Validates new measures, such as the ones carried by an errand being created.
	 */
	public void validate(final List<Measure> measures, final String namespace, final String municipalityId) {
		validate(measures, emptyList(), namespace, municipalityId);
	}

	/**
	 * Validates measures carried by an errand being patched. A measure carrying the id of one of the existing measures
	 * is an update of that measure, any other is new, which is the same distinction the merge into the errand makes.
	 *
	 * @param measures the measures of the patch
	 * @param existing the measures the errand currently holds
	 */
	public void validate(final List<Measure> measures, final List<MeasureEntity> existing, final String namespace, final String municipalityId) {
		final var existingById = ofNullable(existing).orElse(emptyList()).stream()
			.filter(entity -> entity.getId() != null)
			.collect(toMap(MeasureEntity::getId, identity()));

		ofNullable(measures).orElse(emptyList())
			.forEach(measure -> validate(measure, existingById.get(measure.getId()), namespace, municipalityId));
	}

	/**
	 * Validates a new measure.
	 */
	public void validate(final Measure measure, final String namespace, final String municipalityId) {
		validate(measure, null, namespace, municipalityId);
	}

	/**
	 * Validates a measure against the one it updates, or as a new measure when there is none.
	 *
	 * @param measure  the measure sent in
	 * @param existing the measure being updated, or null when the measure is new
	 */
	public void validate(final Measure measure, final MeasureEntity existing, final String namespace, final String municipalityId) {
		if (measure == null) {
			return;
		}
		validateType(measure, existing, namespace, municipalityId);

		if (existing == null) {
			validateRole(measure.getAddedByRole(), namespace, municipalityId);
			accessControlService.verifyMeasureCreator(namespace, municipalityId, measure.getAddedByUser(), measure.getAddedByRole());
		} else {
			validateUnchanged("addedByUser", measure.hasField(Field.ADDED_BY_USER), measure.getAddedByUser(), existing.getAddedByUser());
			validateUnchanged("addedByRole", measure.hasField(Field.ADDED_BY_ROLE), measure.getAddedByRole(), existing.getAddedByRole());
		}
	}

	private void validateType(final Measure measure, final MeasureEntity existing, final String namespace, final String municipalityId) {
		if (!measure.hasField(Field.TYPE)) {
			return;
		}
		final var type = measure.getType();
		if (type == null || type.isBlank()) {
			throw Problem.valueOf(BAD_REQUEST, "Measure type must not be null or blank");
		}
		final var metadata = measureTypeRepository.findWithSharedLockByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, type)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, BAD_MEASURE_TYPE.formatted(type, namespace, municipalityId)));

		// A deprecated type can no longer be selected, but a measure that already has it keeps it and stays editable.
		final var alreadySelected = existing != null && Objects.equals(type, existing.getType());
		if (metadata.isDeprecated() && !alreadySelected) {
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
