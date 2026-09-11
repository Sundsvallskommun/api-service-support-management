package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.ACTIVE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.StatementOutcome.NO_RESPONSE;

/**
 * Upholds what the life cycle of a statement means.
 * <p>
 * The rules are checked against the state the statement would end up in rather than against what the request carries,
 * since a patch that only sets the status has to be judged together with the values already stored. That is why the
 * entity is validated after the patch has been applied to it and before it is saved.
 * <p>
 * Which values exist at all is not checked here - the model declares them, and bean validation has rejected anything
 * else before the request reaches the service.
 */
@Component
public class StatementValidator {

	private static final String ACTIVE_REQUIRES_SENT_AT = "A statement cannot be ACTIVE without sentAt being set";
	private static final String COMPLETED_REQUIRES_OUTCOME = "A statement cannot be COMPLETED without an outcome";
	private static final String COMPLETED_REQUIRES_RESPONDED_AT = "A statement with outcome '%s' cannot be COMPLETED without respondedAt being set";
	private static final String OUTCOME_REQUIRES_COMPLETED = "A statement cannot have an outcome while it is %s";

	/**
	 * Rejects a statement whose life cycle does not add up.
	 * <p>
	 * An outcome belongs to a completed statement only. A patch cannot clear a value, so a statement that has been given
	 * one stays COMPLETED - which is what the life cycle says: a statement is withdrawn before it is answered, not after.
	 *
	 * @param entity the statement as it would be stored.
	 */
	public void validate(final StatementEntity entity) {
		if ((entity.getStatus() == ACTIVE) && (entity.getSentAt() == null)) {
			throw Problem.valueOf(BAD_REQUEST, ACTIVE_REQUIRES_SENT_AT);
		}
		if ((entity.getStatus() != COMPLETED) && (entity.getOutcome() != null)) {
			throw Problem.valueOf(BAD_REQUEST, OUTCOME_REQUIRES_COMPLETED.formatted(entity.getStatus()));
		}
		if (entity.getStatus() == COMPLETED) {
			validateCompleted(entity);
		}
	}

	private void validateCompleted(final StatementEntity entity) {
		final var outcome = ofNullable(entity.getOutcome())
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, COMPLETED_REQUIRES_OUTCOME));

		// A statement closed because the deadline passed has no response, and demanding a timestamp for one that never
		// arrived would leave no way to close it.
		if ((outcome != NO_RESPONSE) && (entity.getRespondedAt() == null)) {
			throw Problem.valueOf(BAD_REQUEST, COMPLETED_REQUIRES_RESPONDED_AT.formatted(outcome));
		}
	}
}
