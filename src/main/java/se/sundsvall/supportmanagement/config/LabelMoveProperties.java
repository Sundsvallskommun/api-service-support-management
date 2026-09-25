package se.sundsvall.supportmanagement.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the label-move worker, gathered in one place so that the defaults cannot drift apart from the values
 * in application.yml the way scattered annotations do.
 *
 * @param batchSize         errands read per page. Each errand is still restowed in a transaction of its own.
 * @param maxConcurrentRuns highest number of moves carried out at the same time. One move at a time per namespace is
 *                          already the rule, but nothing stops several namespaces from being walked at once.
 * @param progressInterval  how long a run may go without writing to its job before it reports from inside the page it
 *                          is on - mirrors {@link ErrandPurgeProperties#progressInterval()} for the same reason: a
 *                          page that has gone quiet for longer than {@link JobProperties#staleAfter()} is taken to
 *                          have ended with the instance carrying it out, and a page is normally quick only while the
 *                          database is.
 */
@ConfigurationProperties(prefix = "label.move")
public record LabelMoveProperties(

	@DefaultValue("200") int batchSize,

	@DefaultValue("2") int maxConcurrentRuns,

	@DefaultValue("PT1M") Duration progressInterval) {
}
