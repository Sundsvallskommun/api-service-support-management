package se.sundsvall.supportmanagement.service.model;

import generated.se.sundsvall.accessmapper.Access;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Everything the access mapper says about one user within one namespace, resolved from a single answer of theirs.
 * <p>
 * Access is granted from the three together, so they are held together: read one at a time and cached one at a time,
 * they could be answered from different moments and disagree with each other.
 *
 * @param labelsByLevel the labels the user reaches, per level granted for them
 * @param roles         the roles the user holds, upper cased
 * @param resources     the level the user is granted for each resource they reach
 */
public record AccessSnapshot(
	Map<Access.AccessLevelEnum, Set<MetadataLabelEntity>> labelsByLevel,
	Set<String> roles,
	Map<ProtectedResource, Access.AccessLevelEnum> resources) {

	/**
	 * Copies what it is given, since one snapshot is shared by every request resolving the same user for as long as it
	 * is cached: a caller mutating what an accessor hands back would otherwise rewrite that user's grants for all of
	 * them.
	 */
	public AccessSnapshot {
		labelsByLevel = labelsByLevel.entrySet().stream().collect(toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
		roles = Set.copyOf(roles);
		resources = Map.copyOf(resources);
	}

	/**
	 * What the access mapper says about someone it grants nothing at all, and about anyone it is never asked about.
	 */
	public static AccessSnapshot empty() {
		return new AccessSnapshot(emptyMap(), emptySet(), emptyMap());
	}

	/**
	 * The labels the user reaches at any of sent in levels.
	 */
	public Set<MetadataLabelEntity> labels(Collection<Access.AccessLevelEnum> levels) {
		return levels.stream()
			.map(level -> labelsByLevel.getOrDefault(level, emptySet()))
			.flatMap(Set::stream)
			.collect(toSet());
	}
}
