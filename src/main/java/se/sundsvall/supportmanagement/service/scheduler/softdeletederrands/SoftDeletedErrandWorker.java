package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.util.LogUtils;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;
import se.sundsvall.supportmanagement.service.ErrandService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SOFT_DELETED_RETENTION_IN_DAYS;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withDeletedBefore;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withMunicipalityId;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withNamespace;

/**
 * Deletes for good the errands that have been soft deleted for longer than the namespace they belong to keeps them, as
 * the delete of a single errand does.
 * <p>
 * Each namespace sets how many days its soft deleted errands are kept. A namespace that sets none keeps them until
 * they are restored or deleted, and is passed over.
 * <p>
 * Errands are read in batches of ids but deleted one at a time, each in a transaction of its own. An errand that
 * cannot be deleted is counted as failed, its id is logged, and the walk carries on. It is not tried again until
 * the next run.
 * <p>
 * A run deletes no more than {@code scheduler.soft-deleted-errands.max-errands-per-namespace} errands in each
 * namespace, and leaves the rest to the runs that follow.
 * <p>
 * A namespace whose walk is aborted is logged and passed, and is named in what the run returns.
 */
@Component
public class SoftDeletedErrandWorker {

	private static final Logger LOG = LoggerFactory.getLogger(SoftDeletedErrandWorker.class);

	static final int BATCH_SIZE = 100;

	private static final String ABORTED_ACCOUNT = "Deleting soft deleted errands was aborted in namespace %s";
	private static final String NAMESPACE_IN_ACCOUNT = "%s for municipality %s";

	// Only errands deleted count towards it, so that errands which cannot be deleted, and which the walk meets first every
	// night, cannot keep a namespace from getting any further
	@Value("${scheduler.soft-deleted-errands.max-errands-per-namespace:500}")
	private int maxErrandsPerNamespace = 500;

	private final ErrandsRepository errandsRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final ErrandService errandService;

	// Only errands deleted count towards it, so that errands which cannot be deleted, such as ones with a locked decision,
	// and which the walk meets first every night, cannot keep a namespace from getting any further
	private final int maxErrandsPerNamespace;

	public SoftDeletedErrandWorker(final ErrandsRepository errandsRepository, final NamespaceConfigRepository namespaceConfigRepository, final ErrandService errandService,
		@Value("${scheduler.soft-deleted-errands.max-errands-per-namespace:500}") final int maxErrandsPerNamespace) {
		this.errandsRepository = errandsRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.errandService = errandService;
		this.maxErrandsPerNamespace = maxErrandsPerNamespace;
	}

	/**
	 * Deletes, in every namespace that sets how long it keeps them, the errands soft deleted for longer than that.
	 *
	 * @return an account of the namespaces whose walk was aborted, or empty if none was
	 */
	public Optional<String> deleteSoftDeletedErrands() {
		final var aborted = new ArrayList<String>();

		// Read from the repository rather than through NamespaceConfigService, whose cache of every configuration is not
		// cleared when one of them changes, so that a run sees what is configured when it starts
		for (final var config : namespaceConfigRepository.findAll()) {
			final var retentionInDays = retentionInDaysOf(config);
			if (retentionInDays.isPresent() && !deleteSoftDeletedErrands(config, retentionInDays.get())) {
				aborted.add(NAMESPACE_IN_ACCOUNT.formatted(config.getNamespace(), config.getMunicipalityId()));
			}
		}

		return Optional.of(aborted)
			.filter(not(List::isEmpty))
			.map(namespaces -> ABORTED_ACCOUNT.formatted(String.join(", ", namespaces)));
	}

	private static Optional<Integer> retentionInDaysOf(final NamespaceConfigEntity config) {
		return ofNullable(ConfigPropertyExtractor.getNullableValue(config, PROPERTY_SOFT_DELETED_RETENTION_IN_DAYS));
	}

	/**
	 * Walks a single namespace, and says whether the walk got through without being aborted. A walk that cannot read its
	 * next batch, such as while the database is out of reach, is aborted and logged rather than thrown.
	 */
	private boolean deleteSoftDeletedErrands(final NamespaceConfigEntity config, final int retentionInDays) {
		try {
			deleteSoftDeletedErrands(config.getNamespace(), config.getMunicipalityId(), now(systemDefault()).minusDays(retentionInDays));
			return true;
		} catch (final Exception e) {
			LOG.error("Deleting soft deleted errands in namespace {} for municipality {} was aborted", sanitizeForLogging(config.getNamespace()), sanitizeForLogging(config.getMunicipalityId()), e);
			return false;
		}
	}

	private void deleteSoftDeletedErrands(final String namespace, final String municipalityId, final OffsetDateTime deletedBefore) {
		final var reached = withNamespace(namespace)
			.and(withMunicipalityId(municipalityId))
			.and(withDeletedBefore(deletedBefore));
		String cursor = null;
		var deleted = 0;
		var failed = 0;

		while (deleted < maxErrandsPerNamespace) {
			final var ids = errandsRepository.findIdsAfter(reached, cursor, BATCH_SIZE);

			if (ids.isEmpty()) {
				break;
			}

			for (final var id : ids) {
				if (deleted >= maxErrandsPerNamespace) {
					break;
				}

				// Advanced before the errand is handled, so that one which cannot be deleted is passed rather than met again
				// on the next batch.
				cursor = id;

				try {
					// An errand restored since the batch was read is left as it is, and not counted as deleted.
					if (errandService.deleteSoftDeletedErrand(id, deletedBefore)) {
						deleted++;
					}
				} catch (final Exception e) {
					failed++;
					LOG.warn("Could not delete soft deleted errand {} in namespace {} for municipality {}: {}", sanitizeForLogging(id), sanitizeForLogging(namespace), sanitizeForLogging(municipalityId),
						ofNullable(e.getMessage()).map(LogUtils::sanitizeForLogging).orElseGet(() -> e.getClass().getName()));
				}
			}
		}

		LOG.info("Deleted {} errands in namespace {} for municipality {} soft deleted before {}, {} could not be deleted", deleted, sanitizeForLogging(namespace), sanitizeForLogging(municipalityId), deletedBefore, failed);
		if (deleted >= maxErrandsPerNamespace) {
			LOG.info("Reached the {} errands a run deletes in namespace {} for municipality {}, any that remain are left to the next run", maxErrandsPerNamespace, sanitizeForLogging(namespace), sanitizeForLogging(municipalityId));
		}
	}
}
