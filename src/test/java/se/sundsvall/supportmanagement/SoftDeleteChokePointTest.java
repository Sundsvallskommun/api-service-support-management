package se.sundsvall.supportmanagement;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

import static java.util.Map.entry;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reminds developers that a soft deleted errand is to be held as gone.
 * <p>
 * Every finder {@link ErrandsRepository} declares has to leave soft deleted errands out, by naming DeletedIsNull, or be
 * listed as exempt with the reason it reaches them. The finders it inherits, such as findById and existsById, cannot be
 * told apart this way: whoever calls them decides on soft deleted errands themselves.
 */
class SoftDeleteChokePointTest {

	private static final String LEAVES_OUT_SOFT_DELETED = "DeletedIsNull";

	/**
	 * Finders that reach soft deleted errands too, with the reason they do.
	 */
	private static final Map<String, String> EXEMPT = Map.ofEntries(
		// What may be done to a soft deleted errand: restoring it and deleting it for good.
		entry("existsByIdAndNamespaceAndMunicipalityId", "AccessControlService.getErrandIncludingDeleted, for restore and hard delete"),
		entry("existsWithLockingByIdAndNamespaceAndMunicipalityId", "AccessControlService.getErrandIncludingDeleted, and the row locks of the attachment sequence and the process relay"),
		// The retention purge removes soft deleted errands as it removes any other, and a handover reads an errand already
		// held to access, or the one it has just created.
		entry("findByIdAndNamespaceAndMunicipalityId", "retention purge, and handover of an errand already verified"),
		// The action worker locks the errand and leaves a soft deleted one waiting itself.
		entry("findWithLockingById", "ActionWorker, which checks the deletion after locking"),
		// Incoming email is kept on the errand it answers, soft deleted or not, so that nothing is lost.
		entry("findByErrandNumberAndNamespaceAndMunicipalityId", "EmailReaderWorker, which keeps incoming email on a soft deleted errand"),
		// A soft deleted errand still wears its labels and phases, so metadata it uses may not be removed, and moving or
		// merging labels rebuilds its labels as well, for it to come back with the labels of the tree.
		entry("existsByLabelsMetadataLabelIdIn", "label removal guard"),
		entry("existsByAccessLabelsMetadataLabelIdIn", "label removal guard"),
		entry("countByLabelsMetadataLabelId", "label usage count"),
		entry("countDistinctByLabelsMetadataLabelIdIn", "label usage count"),
		entry("findAllByLabelsMetadataLabelId", "label usage lookup"),
		entry("findByLabelsMetadataLabelIdAndIdGreaterThanOrderByIdAsc", "label move restow"),
		entry("findByLabelsMetadataLabelIdInAndIdGreaterThanOrderByIdAsc", "label merge restow"),
		entry("findIdsWithNonEmptyLabels", "label move and merge restow"),
		entry("existsByPhasesPhaseEntityId", "phase removal guard"));

	@Test
	void everyFinderLeavesOutSoftDeletedErrands() {
		final var offenders = finders()
			.filter(name -> !name.contains(LEAVES_OUT_SOFT_DELETED))
			.filter(name -> !EXEMPT.containsKey(name))
			.sorted()
			.toList();

		assertThat(offenders)
			.withFailMessage(() -> """
				The following finders of ErrandsRepository reach soft deleted errands:

				%s

				A soft deleted errand is held as gone everywhere but where it is restored or deleted for good. Name
				DeletedIsNull in the finder, or, if it genuinely has to reach soft deleted errands, add it to EXEMPT in
				%s together with the reason.""".formatted(
				offenders.stream().map(name -> "  - " + name).collect(joining("\n")),
				getClass().getName()))
			.isEmpty();
	}

	@Test
	void exemptionsAreStillDeclared() {
		// Keeps EXEMPT from accumulating stale entries as finders are renamed or removed.
		final var declared = finders().collect(toSet());

		assertThat(EXEMPT.keySet().stream().filter(name -> !declared.contains(name)).sorted().toList())
			.withFailMessage("These EXEMPT entries are no longer declared by ErrandsRepository and should be removed from %s"
				.formatted(getClass().getName()))
			.isEmpty();
	}

	private static Stream<String> finders() {
		return Stream.of(ErrandsRepository.class.getDeclaredMethods())
			.map(Method::getName)
			.filter(name -> name.startsWith("find") || name.startsWith("exists") || name.startsWith("count"))
			.distinct();
	}
}
