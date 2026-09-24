package se.sundsvall.supportmanagement.service.access;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static java.util.Objects.isNull;

/**
 * What one user holds in one namespace at one access level, resolved without an errand in hand.
 * <p>
 * Errands are reached by three routes. Through the labels of the user, which cover an errand at the level asked for or
 * only at limited read, and through reporting, which reaches the errands the user reported whatever the labels say.
 * Each route comes with what the user may read of an errand reached by it: on the label route the roles of the user
 * restrict a covered errand, on the limited route what the namespace exposes for a limited read, on the reporter route
 * its reporter fields. A route that is null is closed at the level; a route that is open but empty (no labels, no ad
 * account) reaches nothing.
 * <p>
 * The labels of a level are a subset of those of every level below it, so the limited route covers the errands of the
 * label route as well. Two routes reaching the same errand is no contradiction: each says what may be read there, and
 * the narrower of them is always safe.
 * <p>
 * Built from one configuration and one snapshot, so that every question about the user is answered from the same
 * moment. The specification guarding the database, the check made in memory, the mapping of an errand and the search
 * of the index are all renderings of this, and none of them decides anything of its own.
 *
 * @param enforced      false when the namespace does not enforce access control, in which case every errand is
 *                      reached in full and every route is null
 * @param labels        the label route, null when the labels grant nothing at the level
 * @param limitedLabels the labels reaching errands at limited read, with what a limited read exposes. Null when the
 *                      labels grant nothing at limited read, or when the level asked for is above read, since
 *                      nothing is written on the strength of a limited read
 * @param reporter      the reporter route, null when the namespace grants reporters nothing at the level
 */
public record NamespaceGrant(boolean enforced, LabelRoute labels, LabelRoute limitedLabels, ReporterRoute reporter) {

	public static final NamespaceGrant UNRESTRICTED = new NamespaceGrant(false, null, null, null);

	/**
	 * @param labels    the labels through which the user reaches errands, an errand being reached when every access label
	 *                  of it is among them
	 * @param readable  what the user may read of an errand the labels cover, null when nothing restricts them
	 * @param resources the errand scoped resources the user reaches on every errand this route reaches. A resource is
	 *                  guarded on its own, so reaching an errand is not reaching what hangs off it: a namespace extends
	 *                  a limited read to the resources it names and no others
	 */
	public record LabelRoute(Set<MetadataLabelEntity> labels, Map<ErrandField, Set<String>> readable, Set<ProtectedResource> resources) {

		public boolean reachesAnything() {
			return !labels.isEmpty();
		}
	}

	/**
	 * @param adAccount the ad account whose reported errands are reached
	 * @param readable  what the user may read of an errand they reported and the labels do not cover
	 * @param resources the errand scoped resources the namespace grants its reporters at the level
	 */
	public record ReporterRoute(String adAccount, Map<ErrandField, Set<String>> readable, Set<ProtectedResource> resources) {}

	/**
	 * The errand projection: every errand the grant reaches, without what may be read of them. The limited route is part
	 * of it, since an errand reached at limited read is reached.
	 */
	public AccessScope scope() {
		if (!enforced) {
			return AccessScope.UNRESTRICTED;
		}
		final var reaching = isNull(limitedLabels) ? labels : limitedLabels;
		return new AccessScope(true, isNull(reaching) ? null : reaching.labels(), isNull(reporter) ? null : reporter.adAccount());
	}

	/**
	 * The errands one route of the grant reaches, which is what a search filters each of its clauses on.
	 */
	public static AccessScope scopeOf(final LabelRoute route) {
		return new AccessScope(true, isNull(route) ? null : route.labels(), null);
	}

	/**
	 * The errands the user reported, when the namespace grants its reporters anything at the level.
	 */
	public AccessScope reporterScope() {
		return new AccessScope(true, null, isNull(reporter) ? null : reporter.adAccount());
	}

	/**
	 * Whether any route of the grant reaches sent in resource. What a search may search of it is settled per route, see
	 * {@link LabelRoute#resources()}.
	 */
	public boolean reaches(final ProtectedResource resource) {
		return !enforced || Stream.of(isNull(labels) ? null : labels.resources(), isNull(limitedLabels) ? null : limitedLabels.resources(), isNull(reporter) ? null : reporter.resources())
			.filter(java.util.Objects::nonNull)
			.anyMatch(resources -> resources.contains(resource));
	}
}
