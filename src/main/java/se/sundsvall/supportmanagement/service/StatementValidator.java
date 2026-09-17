package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.StatementOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementOutcomeEntity;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.ACTIVE;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.COMPLETED;

/**
 * Upholds what the life cycle of a statement means.
 * <p>
 * The rules are checked against the state the statement would end up in rather than against what the request carries,
 * since a patch that only sets the status has to be judged together with the values already stored. That is why the
 * entity is validated after the patch has been applied to it and before it is saved.
 * <p>
 * Which statuses there are is declared by the model, and bean validation has rejected anything else before the request
 * reaches the service. Which outcomes there are is for the namespace to say, in its metadata - and so is what the life
 * cycle needs to know about an outcome: whether it means that the counterparty responded.
 */
@Component
public class StatementValidator {

	private static final String ACTIVE_REQUIRES_SENT_AT = "A statement cannot be ACTIVE without sentAt being set";
	private static final String COMPLETED_REQUIRES_OUTCOME = "A statement cannot be COMPLETED without an outcome";
	private static final String COMPLETED_REQUIRES_RESPONDED_AT = "A statement with outcome '%s' cannot be COMPLETED without respondedAt being set";
	private static final String OUTCOME_REQUIRES_COMPLETED = "A statement cannot have an outcome while it is %s";
	private static final String BAD_OUTCOME = "'%s' is not a valid statement outcome for namespace '%s' and municipality with id '%s'";

	private final StatementOutcomeRepository statementOutcomeRepository;

	StatementValidator(final StatementOutcomeRepository statementOutcomeRepository) {
		this.statementOutcomeRepository = statementOutcomeRepository;
	}

	/**
	 * Rejects an outcome the namespace has not registered.
	 * <p>
	 * Checked against what the request carries rather than against what is stored, so that an outcome the namespace has
	 * since removed does not stand in the way of every later change to a statement that was given it.
	 *
	 * @param namespace      namespace of the errand.
	 * @param municipalityId municipality of the errand.
	 * @param outcome        the outcome the request carries. Null is left alone, since a patch says nothing about the
	 *                       fields it omits.
	 */
	public void validateOutcome(final String namespace, final String municipalityId, final String outcome) {
		ofNullable(outcome).ifPresent(value -> {
			if (!statementOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, value)) {
				throw Problem.valueOf(BAD_REQUEST, BAD_OUTCOME.formatted(value, namespace, municipalityId));
			}
		});
	}

	/**
	 * Rejects a statement whose life cycle does not add up.
	 * <p>
	 * An outcome belongs to a completed statement only. A patch cannot clear a value, so a statement that has been given
	 * one stays COMPLETED - which is what the life cycle says: a statement is withdrawn before it is answered, not after.
	 * <p>
	 * Whether a completed statement needs the time of the response is read from how the namespace has registered its
	 * outcome, and that registration can change after the statement was completed. It is judged only when the request sets
	 * the status or the outcome, so that a statement completed under one registration is not made unchangeable by a later
	 * one.
	 *
	 * @param entity              the statement as it would be stored.
	 * @param setsStatusOrOutcome whether the request sets the status or the outcome, which a creation always does.
	 */
	public void validate(final StatementEntity entity, final boolean setsStatusOrOutcome) {
		if ((entity.getStatus() == ACTIVE) && (entity.getSentAt() == null)) {
			throw Problem.valueOf(BAD_REQUEST, ACTIVE_REQUIRES_SENT_AT);
		}
		if ((entity.getStatus() != COMPLETED) && (entity.getOutcome() != null)) {
			throw Problem.valueOf(BAD_REQUEST, OUTCOME_REQUIRES_COMPLETED.formatted(entity.getStatus()));
		}
		if (entity.getStatus() == COMPLETED) {
			validateCompleted(entity, setsStatusOrOutcome);
		}
	}

	private void validateCompleted(final StatementEntity entity, final boolean setsStatusOrOutcome) {
		final var outcome = ofNullable(entity.getOutcome())
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, COMPLETED_REQUIRES_OUTCOME));

		if (setsStatusOrOutcome && (entity.getRespondedAt() == null) && meansAResponse(entity, outcome)) {
			throw Problem.valueOf(BAD_REQUEST, COMPLETED_REQUIRES_RESPONDED_AT.formatted(outcome));
		}
	}

	/**
	 * Whether the outcome, as the namespace registered it, means that the counterparty responded. A statement closed
	 * because the deadline passed has no response, and demanding a timestamp for one that never arrived would leave no
	 * way to close it.
	 * <p>
	 * An outcome the namespace no longer holds cannot say, and does not hold the statement to a response.
	 */
	private boolean meansAResponse(final StatementEntity entity, final String outcome) {
		return statementOutcomeRepository.findByNamespaceAndMunicipalityIdAndName(entity.getNamespace(), entity.getMunicipalityId(), outcome)
			.map(StatementOutcomeEntity::isResponded)
			.orElse(false);
	}
}
