package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.DecisionOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SINGLE_DECISION_PER_ERRAND;

/**
 * Upholds the rules a decision carries that the database does not.
 * <p>
 * <b>How many decisions an errand may hold</b> is a question the lines of business answer differently. Interim
 * decisions, partial decisions and reconsideration are ordinary where one line of business expects exactly one decision
 * per errand, so the restriction is a setting of the namespace rather than a unique key. A namespace that has not asked
 * for it is not restricted.
 * <p>
 * <b>Who may claim which method</b> follows from administrative law. Without the check a caseworker could stamp their
 * own decision as automatic, or a process stamp its own as manual - and that is precisely the difference that has to be
 * answerable afterwards.
 * <p>
 * <b>Which outcomes there are</b> is for the namespace to say, in its metadata. The recommendation of an investigation
 * is held to the same outcomes, since it proposes a decision.
 */
@Component
public class DecisionValidator {

	private static final String SINGLE_DECISION_PER_ERRAND = "Errand with id '%s' already holds a decision, and namespace '%s' for municipality with id '%s' allows only one";
	private static final String MANUAL_REQUIRES_AD_ACCOUNT = "A decision with method MANUAL has to be written by an ad account";
	private static final String AUTOMATIC_REQUIRES_CONSUMER = "A decision with method AUTOMATIC cannot be written by an ad account";
	private static final String BAD_OUTCOME = "'%s' is not a valid decision outcome for namespace '%s' and municipality with id '%s'";

	private final DecisionRepository decisionRepository;
	private final DecisionOutcomeRepository decisionOutcomeRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;

	DecisionValidator(final DecisionRepository decisionRepository, final DecisionOutcomeRepository decisionOutcomeRepository, final NamespaceConfigRepository namespaceConfigRepository) {
		this.decisionRepository = decisionRepository;
		this.decisionOutcomeRepository = decisionOutcomeRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
	}

	/**
	 * Rejects a second decision where the namespace allows only one.
	 *
	 * @param namespace      namespace of the errand.
	 * @param municipalityId municipality of the errand.
	 * @param errandId       id of the errand the decision is being created on.
	 */
	public void validateCardinality(final String namespace, final String municipalityId, final String errandId) {
		if (singleDecisionPerErrand(namespace, municipalityId) && decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityId(namespace, municipalityId, errandId)) {
			throw Problem.valueOf(CONFLICT, SINGLE_DECISION_PER_ERRAND.formatted(errandId, namespace, municipalityId));
		}
	}

	/**
	 * Rejects a method the caller is not the kind of caller for.
	 * <p>
	 * The method to pass is the one the decision ends up with - on a patch the stored one when the request names none.
	 * Checking only what a patch carries would let a caseworker change an automatic decision by leaving the method out,
	 * and the decision would still claim to be automatic.
	 *
	 * @param method the method the decision ends up with. Null is left alone.
	 */
	public void validateMethod(final DecisionMethod method) {
		ofNullable(method).ifPresent(value -> {
			final var writtenByPerson = Identifier.Type.AD_ACCOUNT.equals(ofNullable(Identifier.get()).map(Identifier::getType).orElse(null));

			if ((value == MANUAL) && !writtenByPerson) {
				throw Problem.valueOf(FORBIDDEN, MANUAL_REQUIRES_AD_ACCOUNT);
			}
			if ((value != MANUAL) && writtenByPerson) {
				throw Problem.valueOf(FORBIDDEN, AUTOMATIC_REQUIRES_CONSUMER);
			}
		});
	}

	/**
	 * Rejects an outcome the namespace has not registered.
	 * <p>
	 * Checked against what the request carries rather than against what is stored, so that an outcome the namespace has
	 * since removed does not stand in the way of every later change to a decision that was given it.
	 *
	 * @param namespace      namespace of the errand.
	 * @param municipalityId municipality of the errand.
	 * @param outcome        the outcome, or the recommendation, the request carries. Null is left alone, since a patch
	 *                       says nothing about the fields it omits.
	 */
	public void validateOutcome(final String namespace, final String municipalityId, final String outcome) {
		ofNullable(outcome).ifPresent(value -> {
			if (!decisionOutcomeRepository.existsByNamespaceAndMunicipalityIdAndName(namespace, municipalityId, value)) {
				throw Problem.valueOf(BAD_REQUEST, BAD_OUTCOME.formatted(value, namespace, municipalityId));
			}
		});
	}

	/**
	 * A namespace configured before the setting existed, or configured without it, reads as unrestricted rather than
	 * failing the request.
	 */
	private boolean singleDecisionPerErrand(final String namespace, final String municipalityId) {
		return namespaceConfigRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(config -> ConfigPropertyExtractor.<Boolean>getNullableValue(config, PROPERTY_SINGLE_DECISION_PER_ERRAND))
			.orElse(false);
	}
}
