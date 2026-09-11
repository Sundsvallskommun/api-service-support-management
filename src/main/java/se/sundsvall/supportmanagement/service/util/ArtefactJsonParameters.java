package se.sundsvall.supportmanagement.service.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterEntity;
import se.sundsvall.supportmanagement.integration.db.model.JsonParameterLink;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

/**
 * Removal of the JSON parameters a handling artefact owns, in one place so that four services cannot drift apart on it.
 */
public final class ArtefactJsonParameters {

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
}
