package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.stream.Collectors.toSet;

/**
 * A path-indexed view of a namespace's label tree, shared by {@link MetadataService#restructureLabelTree} (dry-run)
 * and {@code LabelTreeRestructureWorker} (real execution) so that both walk the exact same ordered list of
 * {@code LabelRestructureStep}s the exact same way, resolving each step's paths against whatever the *previous* steps
 * in that same request already did to the tree - including a step that references a label an earlier {@code ADD}
 * step in the same request just created, which has no persisted id yet during a dry run.
 * <p>
 * Built once from a flat list of every {@link MetadataLabelEntity} in the namespace (each entity's own
 * {@code resourcePath}/{@code resourceName} fields are enough - this deliberately never touches a lazy
 * {@code parent}/{@code metadataLabels} association, since the entities backing it are read once, outside any
 * transaction that would still be open by the time {@code recordAdd}/{@code recordMove} etc. run on them later).
 * <p>
 * Every path is addressed by its joined {@code "A/B/C"} form, identical to
 * {@link MetadataLabelEntity#getResourcePath()}
 * - root is the empty string.
 */
public final class LabelTreeSnapshot {

	static final String SEPARATOR = "/";
	static final String ROOT = "";

	// path -> id. A value of null means a label exists at this path only because an earlier step in the same request
	// (necessarily an ADD, during a dry run) put it there, and it has no persisted id yet.
	private final Map<String, String> idByPath = new HashMap<>();

	// path -> immediate child resourceNames. Always has an entry for every path idByPath does (including ROOT), so a
	// lookup never has to distinguish "no children" from "path does not exist" via a missing map entry.
	private final Map<String, Set<String>> childrenByPath = new HashMap<>();

	// path -> every real, persisted id currently folded into it - starts as a singleton of the label's own id, but
	// recordMerge unions a source path's accumulated ids into its target's rather than discarding them, so a later
	// step asking "which real ids does this path represent" still finds a source a prior step already merged away.
	// Without this, a step after an in-request MERGE would ask the database about only the target's own id and miss
	// every errand still carrying a source id the real merge has not actually restowed yet (see
	// MetadataService#simulateDelete/#simulateMove/#simulateMerge for where that matters).
	private final Map<String, Set<String>> realIdsByPath = new HashMap<>();

	private LabelTreeSnapshot() {
		childrenByPath.put(ROOT, new HashSet<>());
	}

	static LabelTreeSnapshot of(final List<MetadataLabelEntity> allLabels) {
		final var snapshot = new LabelTreeSnapshot();
		allLabels.forEach(label -> {
			final var path = label.getResourcePath();
			snapshot.idByPath.put(path, label.getId());
			snapshot.childrenByPath.putIfAbsent(path, new HashSet<>());
			snapshot.childrenByPath.computeIfAbsent(parentOf(path), k -> new HashSet<>()).add(label.getResourceName());
			snapshot.realIdsByPath.computeIfAbsent(path, k -> new HashSet<>()).add(label.getId());
		});
		return snapshot;
	}

	public static String join(final List<String> path) {
		return String.join(SEPARATOR, path);
	}

	/**
	 * Every segment of {@code path} but the last - a step's own parent path, which both {@link MetadataService}'s
	 * simulation and {@code LabelTreeRestructureWorker}'s real execution need from the exact same
	 * {@code LabelRestructureStep#getPath()} shape, hence held here rather than as a private copy in each.
	 */
	public static List<String> allButLast(final List<String> path) {
		return path.subList(0, path.size() - 1);
	}

	/**
	 * The last segment of {@code path} - a step's own resourceName, absent an explicit override. See
	 * {@link #allButLast} for why this lives here rather than as a private copy in each of its two callers.
	 */
	public static String lastSegment(final List<String> path) {
		return path.get(path.size() - 1);
	}

	private static String parentOf(final String path) {
		final var lastSeparator = path.lastIndexOf(SEPARATOR);
		return lastSeparator < 0 ? ROOT : path.substring(0, lastSeparator);
	}

	private static String lastSegmentOf(final String path) {
		final var lastSeparator = path.lastIndexOf(SEPARATOR);
		return lastSeparator < 0 ? path : path.substring(lastSeparator + 1);
	}

	boolean exists(final String path) {
		return ROOT.equals(path) || idByPath.containsKey(path);
	}

	/**
	 * @return the id at this path, or {@code null} if the path exists only because an earlier step in this same request
	 *         added it (not yet persisted).
	 */
	String idAt(final String path) {
		return idByPath.get(path);
	}

	boolean isLeaf(final String path) {
		return childrenByPath.getOrDefault(path, Set.of()).isEmpty();
	}

	/**
	 * Every real, persisted id currently folded into a path at or under {@code path} (itself included) - used to
	 * collect the full set of ids a move or merge must ask the database about, mirroring {@link MetadataService}'s
	 * existing descendant-collection for the standalone move endpoint, but additionally carrying along any id an
	 * earlier {@code MERGE} step in the same request already folded into a surviving path - see
	 * {@link #realIdsByPath}'s own doc for why a plain {@code idByPath} lookup would lose track of those.
	 */
	Set<String> realIdsAtOrUnder(final String path) {
		final var ids = new HashSet<String>();
		realIdsByPath.forEach((candidatePath, pathIds) -> {
			if (candidatePath.equals(path) || candidatePath.startsWith(path + SEPARATOR)) {
				ids.addAll(pathIds);
			}
		});
		return ids;
	}

	/**
	 * Every path strictly under {@code path} - used by a move to check each descendant's rebased path for collision
	 * and length the same way the moved label's own new path already is, mirroring {@link MetadataService}'s
	 * DB-backed {@code validateNoDescendantPathCollision}/{@code validateResourcePathLength} for the standalone move
	 * endpoint.
	 */
	Set<String> descendantPathsUnder(final String path) {
		final var prefix = path + SEPARATOR;
		return idByPath.keySet().stream()
			.filter(candidatePath -> candidatePath.startsWith(prefix))
			.collect(toSet());
	}

	void recordAdd(final String path, final String id) {
		idByPath.put(path, id);
		childrenByPath.putIfAbsent(path, new HashSet<>());
		childrenByPath.computeIfAbsent(parentOf(path), k -> new HashSet<>()).add(lastSegmentOf(path));
	}

	void recordDelete(final String path) {
		idByPath.remove(path);
		childrenByPath.remove(path);
		childrenByPath.getOrDefault(parentOf(path), Set.of()).remove(lastSegmentOf(path));
		realIdsByPath.remove(path);
	}

	/**
	 * Reparents everything at or under {@code sourcePath} to sit under {@code newPath} instead, preserving each
	 * descendant's relative path beneath the moved node - mirrors how {@code MetadataLabelEntity}'s own
	 * {@code updateChildrenPathsRecursively} recomputes descendant paths on a real move.
	 */
	void recordMove(final String sourcePath, final String newPath) {
		final var toReparent = new ArrayList<Map.Entry<String, String>>();
		idByPath.forEach((candidatePath, id) -> {
			if (candidatePath.equals(sourcePath) || candidatePath.startsWith(sourcePath + SEPARATOR)) {
				toReparent.add(Map.entry(candidatePath, id));
			}
		});

		childrenByPath.getOrDefault(parentOf(sourcePath), Set.of()).remove(lastSegmentOf(sourcePath));

		toReparent.forEach(entry -> {
			final var oldPath = entry.getKey();
			final var id = entry.getValue();
			final var rebasedPath = newPath + oldPath.substring(sourcePath.length());

			idByPath.remove(oldPath);
			final var children = childrenByPath.remove(oldPath);
			idByPath.put(rebasedPath, id);
			childrenByPath.put(rebasedPath, children != null ? children : new HashSet<>());

			final var realIds = realIdsByPath.remove(oldPath);
			if (realIds != null) {
				realIdsByPath.put(rebasedPath, realIds);
			}
		});

		childrenByPath.computeIfAbsent(parentOf(newPath), k -> new HashSet<>()).add(lastSegmentOf(newPath));
	}

	/**
	 * Folds each source path's accumulated real ids into the target's before dropping the source paths from the tree,
	 * rather than simply deleting them - see {@link #realIdsByPath}'s own doc for why a later step must still be able
	 * to find them there.
	 */
	void recordMerge(final String targetPath, final List<String> sourcePaths) {
		final var targetRealIds = realIdsByPath.computeIfAbsent(targetPath, k -> new HashSet<>());
		sourcePaths.forEach(sourcePath -> {
			targetRealIds.addAll(realIdsByPath.getOrDefault(sourcePath, Set.of()));
			recordDelete(sourcePath);
		});
	}
}
