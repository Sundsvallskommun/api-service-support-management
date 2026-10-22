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
 * The rules are checked against the state the statement would end up in, so the entity is validated after the patch
 * has been applied to it and before it is saved. The outcomes, and whether each of them means that the counterparty
 * responded, are read from the metadata of the namespace.
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
	 * Rejects an outcome the namespace has not registered. Only the outcome the request carries is checked, not the one
	 * already stored.
	 *
	 * @param namespace      namespace of the errand.
	 * @param municipalityId municipality of the errand.
	 * @param outcome        the outcome the request carries. Null is left alone.
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
	 * An active statement needs sentAt, and an outcome belongs to a completed statement only. A patch cannot clear a
	 * value, so a statement that has been given an outcome stays COMPLETED.
	 * <p>
	 * Whether a completed statement needs the time of the response is read from how the namespace has registered its
	 * outcome. It is judged only when the request sets the status or the outcome.
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
	 * Whether the outcome, as the namespace registered it, means that the counterparty responded. An outcome the
	 * namespace no longer holds does not hold the statement to a response.
	 */
	private boolean meansAResponse(final StatementEntity entity, final String outcome) {
		return statementOutcomeRepository.findByNamespaceAndMunicipalityIdAndName(entity.getNamespace(), entity.getMunicipalityId(), outcome)
			.map(StatementOutcomeEntity::isResponded)
			.orElse(false);
	}
}
