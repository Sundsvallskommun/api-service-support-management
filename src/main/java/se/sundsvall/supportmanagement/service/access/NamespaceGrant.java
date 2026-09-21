package se.sundsvall.supportmanagement.service.access;

import java.util.Map;
import java.util.Set;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static java.util.Objects.isNull;

/**
 * What one user holds in one namespace at one access level, resolved without an errand in hand.
 * <p>
 * Errands are reached by two routes. Through the labels of the user, which cover an errand or not, and through
 * reporting, which reaches the errands the user reported whatever the labels say. Each route comes with what the user
 * may read of an errand reached by it: on the label route the roles of the user restrict a covered errand, on the
 * reporter route the reporter fields of the namespace do. A route that is null is closed at the level; a route that is
 * open but empty (no labels, no ad account) reaches nothing.
 * <p>
 * Built from one configuration and one snapshot, so that every question about the user is answered from the same
 * moment. The specification guarding the database, the check made in memory, the mapping of an errand and the search
 * of the index are all renderings of this, and none of them decides anything of its own.
 *
 * @param enforced         false when the namespace does not enforce access control, in which case every errand is
 *                         reached in full and both routes are null
 * @param labels           the label route, null when the labels grant nothing at the level
 * @param reporter         the reporter route, null when the namespace grants reporters nothing at the level
 * @param resourcesReached the errand scoped resources the labels of the user reach at the level, on the errands they
 *                         cover
 */
public record NamespaceGrant(boolean enforced, LabelRoute labels, ReporterRoute reporter, Set<ProtectedResource> resourcesReached) {

	public static final NamespaceGrant UNRESTRICTED = new NamespaceGrant(false, null, null, Set.of());

	/**
	 * @param labels   the labels through which the user reaches errands, an errand being reached when every access label
	 *                 of it is among them
	 * @param readable what the user may read of an errand the labels cover, null when nothing restricts them
	 */
	public record LabelRoute(Set<MetadataLabelEntity> labels, Map<ErrandField, Set<String>> readable) {

		public boolean reachesAnything() {
			return !labels.isEmpty();
		}
	}

	/**
	 * @param adAccount the ad account whose reported errands are reached
	 * @param readable  what the user may read of an errand they reported and the labels do not cover
	 */
	public record ReporterRoute(String adAccount, Map<ErrandField, Set<String>> readable) {}

	/**
	 * The errand projection: which errands are reached, without what may be read of them.
	 */
	public AccessScope scope() {
		return enforced
			? new AccessScope(true, isNull(labels) ? null : labels.labels(), isNull(reporter) ? null : reporter.adAccount())
			: AccessScope.UNRESTRICTED;
	}

	public boolean reaches(final ProtectedResource resource) {
		return !enforced || resourcesReached.contains(resource);
	}
}
