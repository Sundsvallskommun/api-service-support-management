package se.sundsvall.supportmanagement.service;

/**
 * Says that a row has been written to the outbox for an errand, so that it can be delivered as soon as the transaction
 * is committed, without waiting for the next scheduled run.
 * <p>
 * Carries the errand and nothing of the row. The relay reads the rows again, so a signal may arrive late, twice or not
 * at all.
 *
 * @param errandId the errand the row is about.
 */
public record ProcessEventWritten(String errandId) {
}
