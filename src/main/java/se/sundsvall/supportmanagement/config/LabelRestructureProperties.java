package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the label-restructure runner, gathered in one place so that the defaults cannot drift apart from the
 * values in application.yml the way scattered annotations do.
 *
 * @param batchSize          errands locked and restowed per chunk, inside the restructure's single transaction.
 * @param maxConcurrentRuns  highest number of restructures carried out at the same time. One restructure at a time
 *                           per namespace is already the rule, but nothing stops several namespaces from being
 *                           walked at once.
 * @param progressInterval   how long a run may go without writing to its job before it reports from inside the chunk
 *                           it is on - mirrors {@link ErrandPurgeProperties#progressInterval()} for the same reason:
 *                           a chunk that has gone quiet for longer than {@link JobProperties#staleAfter()} is taken to
 *                           have ended with the instance carrying it out, and a chunk is normally quick only while the
 *                           database is.
 * @param maxAffectedErrands highest number of errands one restructure may affect, across every MOVE/MERGE step
 *                           combined. Refused up front, before any lock is taken, rather than left unbounded - the
 *                           whole affected set is locked for the life of one transaction, so an unexpectedly large
 *                           subtree would otherwise hold those rows for however long restowing all of them takes,
 *                           blocking every other writer that touches one of them for the duration.
 */
@ConfigurationProperties(prefix = "label.restructure")
public record LabelRestructureProperties(

	@DefaultValue("200") int batchSize,

	@DefaultValue("2") int maxConcurrentRuns,

	@DefaultValue("PT1M") Duration progressInterval,

	@DefaultValue("10000") int maxAffectedErrands) {
}
