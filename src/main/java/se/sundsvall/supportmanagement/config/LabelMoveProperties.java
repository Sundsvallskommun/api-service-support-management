package se.sundsvall.supportmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the label-move worker, gathered in one place so that the defaults cannot drift apart from the values
 * in application.yml the way scattered annotations do.
 *
 * @param batchSize         errands read per page. Each page is restowed in a transaction of its own.
 * @param maxConcurrentRuns highest number of moves carried out at the same time. One move at a time per namespace is
 *                          already the rule, but nothing stops several namespaces from being walked at once.
 */
@ConfigurationProperties(prefix = "label.move")
public record LabelMoveProperties(

	@DefaultValue("200") int batchSize,

	@DefaultValue("2") int maxConcurrentRuns) {
}
