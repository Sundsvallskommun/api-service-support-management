package se.sundsvall.supportmanagement.service;

/**
 * Says that a row has been written to the outbox for an errand, so that it can be delivered as soon as the transaction
 * is committed rather than at the next scheduled run.
 * <p>
 * Carries the errand and nothing of the row. The relay reads the rows again, which is what makes a signal that arrives
 * late, twice or not at all harmless.
 *
 * @param errandId the errand the row is about.
 */
public record ProcessEventWritten(String errandId) {
}
