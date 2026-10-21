package se.sundsvall.supportmanagement.service.access;

import java.util.Set;
import java.util.stream.Collectors;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

/**
 * Which errands of a namespace the requesting user reaches, as data rather than as a query, so that the one decision
 * can be rendered both as the specification guarding every endpoint and as the predicate the search index is asked
 * with.
 * <p>
 * An errand is reached through the labels when every access label of it is among the allowed ones, and through
 * reporting when its reporter is the user. The two are unioned, so reporting can only ever add access, never reduce
 * what the labels already granted. A route that is null is closed: labels grant nothing at the required level, or the
 * namespace grants reporters nothing there. A route that is open but empty (no allowed labels, or no ad account to
 * match) reaches nothing, which is not the same thing as being closed only in what it says, since both leave the errand
 * out.
 *
 * @param enforced          false when the namespace does not enforce access control, in which case every errand is
 *                          reached
 * @param allowedLabels     labels through which the user reaches errands, null when labels grant nothing at the level
 * @param reporterAdAccount ad account whose reported errands are reached, null when the namespace grants reporters
 *                          nothing at the level
 */
public record AccessScope(boolean enforced, Set<MetadataLabelEntity> allowedLabels, String reporterAdAccount) {

	public static final AccessScope UNRESTRICTED = new AccessScope(false, null, null);

	/** Ids of the allowed labels, empty when labels grant nothing. */
	public Set<String> allowedLabelIds() {
		return ofNullable(allowedLabels).orElse(emptySet()).stream()
			.map(MetadataLabelEntity::getId)
			.collect(Collectors.toSet());
	}
}
