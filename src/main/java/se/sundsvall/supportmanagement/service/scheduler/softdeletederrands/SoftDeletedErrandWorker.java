package se.sundsvall.supportmanagement.service.scheduler.softdeletederrands;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
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
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SOFT_DELETED_RETENTION_IN_DAYS;

/**
 * Deletes for good the errands that have been soft deleted for longer than the namespace they belong to keeps them, as
 * the delete of a single errand does.
 * <p>
 * Each namespace sets how many days its soft deleted errands are kept. A namespace that sets none keeps them until
 * they are restored or deleted, and is passed over. The configurations are read straight from the repository rather
 * than through the cache of the namespace configuration, so that a run sees what is configured when it starts.
 * <p>
 * Errands are read in batches of ids but deleted one at a time, each in a transaction of its own. An errand that cannot
 * be deleted is counted as failed, its id is logged, and the walk carries on. The batch query is keyed on the id of the
 * last errand reached, so the walk passes an errand that failed instead of meeting it again on the next batch.
 */
@Component
public class SoftDeletedErrandWorker {

	private static final Logger LOG = LoggerFactory.getLogger(SoftDeletedErrandWorker.class);

	static final int BATCH_SIZE = 100;

	private final ErrandsRepository errandsRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final ErrandService errandService;

	public SoftDeletedErrandWorker(final ErrandsRepository errandsRepository, final NamespaceConfigRepository namespaceConfigRepository, final ErrandService errandService) {
		this.errandsRepository = errandsRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.errandService = errandService;
	}

	/**
	 * Deletes, in every namespace that sets how long it keeps them, the errands soft deleted for longer than that.
	 */
	public void deleteSoftDeletedErrands() {
		namespaceConfigRepository.findAll()
			.forEach(config -> retentionInDaysOf(config)
				.ifPresent(days -> deleteSoftDeletedErrands(config.getNamespace(), config.getMunicipalityId(), now(systemDefault()).minusDays(days))));
	}

	private static Optional<Integer> retentionInDaysOf(final NamespaceConfigEntity config) {
		return ofNullable(ConfigPropertyExtractor.getNullableValue(config, PROPERTY_SOFT_DELETED_RETENTION_IN_DAYS));
	}

	private void deleteSoftDeletedErrands(final String namespace, final String municipalityId, final OffsetDateTime deletedBefore) {
		var cursor = "";
		var deleted = 0;
		var failed = 0;

		while (true) {
			final var ids = errandsRepository.findIdsDeletedBefore(namespace, municipalityId, deletedBefore, cursor, PageRequest.of(0, BATCH_SIZE));

			if (ids.isEmpty()) {
				break;
			}

			for (final var id : ids) {
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
	}
}
