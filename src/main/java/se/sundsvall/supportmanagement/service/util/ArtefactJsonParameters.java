package se.sundsvall.supportmanagement.service.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterLink;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.supportmanagement.service.mapper.ErrandMapper.changedJsonParameterKeys;

/**
 * The JSON parameters a handling artefact owns, as the errand holding them sees them: removed with the artefact, left
 * alone by a patch of the errand, and not written through the errand at all. In one place, so that the services cannot
 * drift apart on any of it.
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
	 * The links naming them are left to the database, which removes a link together with its parameter. Hibernate is never
	 * asked to remove one: doing so in the same flush as the parameter would null the reference to it first, which the
	 * column refuses.
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
	 * @param  errandEntity      the errand as it stands, before the patch is applied.
	 * @param  jsonParameters    the parameters of the patch, or null when it leaves them alone.
	 * @param  writableKey       the keys the caller may change, per field.
	 * @param  ownedParameterIds the ids of the parameters the artefacts of the errand own, asked for only when the patch
	 *                           touches the parameters.
	 * @return                   the keys the patch may change, per field: the same, less the ones artefacts own.
	 */
	public static Function<ErrandField, Predicate<String>> withoutArtefactParameters(final ErrandEntity errandEntity, final List<JsonParameter> jsonParameters,
		final Function<ErrandField, Predicate<String>> writableKey, final Supplier<Set<String>> ownedParameterIds) {

		if (isNull(jsonParameters)) {
			return writableKey;
		}

		final var ownedIds = ownedParameterIds.get();
		final var ownedKeys = ofNullable(errandEntity.getJsonParameters()).orElse(emptyList()).stream()
			.filter(parameter -> ownedIds.contains(parameter.getId()))
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

	/**
	 * Refuses writing or removing a parameter through the JSON parameter endpoints of the errand when a handling artefact
	 * owns it.
	 * <p>
	 * Stricter than {@link #withoutArtefactParameters}, on purpose. A patch carries every parameter of the errand, so it
	 * carries the ones the artefacts own as a matter of course, and is let through as long as it leaves them as they stand.
	 * An endpoint naming a single key is asked for that key alone, so it is refused whatever it carries, and the caller is
	 * pointed at the artefact. Without it, the content of an artefact could be rewritten past the version the artefact
	 * answers with, or removed from under the artefact - the database takes the link with the parameter.
	 *
	 * @param parameter       the parameter of the errand about to be written or removed.
	 * @param ownedByArtefact whether a handling artefact owns the parameter with the given id.
	 */
	public static void verifyNotOwnedByArtefact(final JsonParameterEntity parameter, final Predicate<String> ownedByArtefact) {
		if (ownedByArtefact.test(parameter.getId())) {
			throw Problem.valueOf(CONFLICT, OWNED_BY_ARTEFACT.formatted(parameter.getKey()));
		}
	}
}
