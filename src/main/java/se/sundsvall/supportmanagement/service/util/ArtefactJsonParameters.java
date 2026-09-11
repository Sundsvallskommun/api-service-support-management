package se.sundsvall.supportmanagement.service.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterLink;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.changedJsonParameterKeys;

/**
 * The JSON parameters a handling artefact owns, as the errand holding them sees them: removed with the artefact, and
 * left alone by a patch of the errand. In one place, so that the services cannot drift apart on either.
 */
public final class ArtefactJsonParameters {

	private static final String OWNED_BY_ARTEFACT = "JSON parameter '%s' belongs to a handling artefact of the errand and is written through that artefact";

	private ArtefactJsonParameters() {}

	/**
	 * Names the JSON parameters an artefact owns, before the artefact is removed and the links naming them go with it.
	 *
	 * @param  links the links of the artefact being removed.
	 * @return       the ids of the parameters it owns.
	 */
	public static Set<String> ownedParameterIds(final List<? extends JsonParameterLink> links) {
		return ofNullable(links).orElse(emptyList()).stream()
			.map(JsonParameterLink::getJsonParameterEntity)
			.filter(Objects::nonNull)
			.map(JsonParameterEntity::getId)
			.collect(toSet());
	}

	/**
	 * Removes those parameters from the collection of the errand, which is what owns the rows.
	 * <p>
	 * Removing them anywhere else would not work: {@code ErrandEntity.jsonParameters} cascades everything, so a row taken
	 * out while that collection still holds it is written back by the next flush.
	 * <p>
	 * <b>Call this after the artefact has been removed, not before.</b> Removing the artefact takes its links with it,
	 * and doing it in that order means the parameters are unlinked by the time they are removed - so nothing reaches a
	 * link row twice.
	 *
	 * @param errandEntity the errand owning the parameters.
	 * @param parameterIds the parameters to remove, as named by {@link #ownedParameterIds(List)}.
	 */
	public static void removeParameters(final ErrandEntity errandEntity, final Set<String> parameterIds) {
		if (parameterIds.isEmpty()) {
			return;
		}
		ofNullable(errandEntity.getJsonParameters())
			.ifPresent(parameters -> parameters.removeIf(parameter -> parameterIds.contains(parameter.getId())));
	}

	/**
	 * Holds a patch of the errand off the JSON parameters its handling artefacts own.
	 * <p>
	 * The errand holds those rows and shows them, but their content is the artefact's. A patch carrying one as it stands,
	 * or not carrying it at all, leaves it as it is - the way the merge leaves any key the caller may not change - and a
	 * patch that would change one is refused. Without this, a patch replacing the parameters of the errand would take the
	 * content of every artefact with it, and could rewrite it past the version the artefact answers with.
	 * <p>
	 * Keys are compared without regard to case, as the database compares them.
	 *
	 * @param  errandEntity   the errand as it stands, before the patch is applied.
	 * @param  jsonParameters the parameters of the patch, or null when it leaves them alone.
	 * @param  writableKey    the keys the caller may change, per field.
	 * @return                the keys the patch may change, per field: the same, less the ones artefacts own.
	 */
	public static Function<ErrandField, Predicate<String>> withoutArtefactParameters(final ErrandEntity errandEntity, final List<JsonParameter> jsonParameters,
		final Function<ErrandField, Predicate<String>> writableKey) {

		if (isNull(jsonParameters)) {
			return writableKey;
		}

		final var ownedKeys = ofNullable(errandEntity.getJsonParameters()).orElse(emptyList()).stream()
			.filter(ArtefactJsonParameters::isOwnedByArtefact)
			.map(JsonParameterEntity::getKey)
			.collect(toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

		changedJsonParameterKeys(errandEntity, jsonParameters).stream()
			.filter(ownedKeys::contains)
			.findFirst()
			.ifPresent(key -> {
				throw Problem.valueOf(CONFLICT, OWNED_BY_ARTEFACT.formatted(key));
			});

		return field -> (field == ErrandField.JSON_PARAMETERS) ? writableKey.apply(field).and(key -> !ownedKeys.contains(key)) : writableKey.apply(field);
	}

	private static boolean isOwnedByArtefact(final JsonParameterEntity parameter) {
		return Stream.<List<?>>of(parameter.getStatementLinks(), parameter.getInvestigationLinks(), parameter.getInvestigationSectionLinks(),
			parameter.getDecisionLinks(), parameter.getMeasureLinks())
			.anyMatch(links -> nonNull(links) && !links.isEmpty());
	}
}
