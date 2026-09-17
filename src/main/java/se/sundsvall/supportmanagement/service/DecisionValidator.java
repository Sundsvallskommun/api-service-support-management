package se.sundsvall.supportmanagement.service;

import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.DecisionOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SINGLE_DECISION_PER_ERRAND;
import static se.sundsvall.supportmanagement.service.ErrandProcessService.hasCompletedProcess;

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
 * answerable afterwards. A manual decision is written by an ad account, and an automatic one by the process consumer of
 * the namespace and nobody else.
 * <p>
 * <b>Which outcomes there are</b> is for the namespace to say, in its metadata. The recommendation of an investigation
 * is held to the same outcomes, since it proposes a decision.
 * <p>
 * <b>When a decision can no longer be changed</b> is a question only for errands that have a process. Once the process
 * has run to its end, no decision of the errand is written to any more - the same completed process that is never
 * started again. And once a decision is completed on such an errand it is locked as it stands, since the process has
 * gone on from it: a correction is a new errand, referred from this one. An errand without a process is never locked,
 * and its event log is what makes the changes traceable. The JSON parameters of a decision stand outside both locks,
 * since what they carry - legal force, service of the decision - is known only after it is made. What a locked decision
 * rests on - the attachments it links, the investigation it names - cannot be removed from under it either.
 */
@Component
public class DecisionValidator {

	private static final String SINGLE_DECISION_PER_ERRAND = "Errand with id '%s' already holds a decision, and namespace '%s' for municipality with id '%s' allows only one";
	private static final String MANUAL_REQUIRES_AD_ACCOUNT = "A decision with method MANUAL has to be written by an ad account";
	private static final String AUTOMATIC_WITHOUT_PROCESS_CONSUMER = "A decision with method AUTOMATIC is written by the process consumer of the namespace, and namespace '%s' for municipality with id '%s' has none";
	private static final String AUTOMATIC_REQUIRES_PROCESS_CONSUMER = "A decision with method AUTOMATIC can only be written by '%s', the process consumer of namespace '%s' for municipality with id '%s'";
	private static final String BAD_OUTCOME = "'%s' is not a valid decision outcome for namespace '%s' and municipality with id '%s'";
	private static final String PROCESS_LIFE_OVER = "The process of errand with id '%s' has run to its end, and its decisions can no longer be changed";
	private static final String DECISION_COMPLETED = "Decision with id '%s' is completed, and on an errand with a process a completed decision can no longer be changed";
	private static final String ATTACHMENT_OF_LOCKED_DECISION = "Attachment with id '%s' belongs to a decision that can no longer be changed, and cannot be removed from errand with id '%s'";
	private static final String INVESTIGATION_OF_LOCKED_DECISION = "Investigation with id '%s' is what a decision that can no longer be changed rests on, and cannot be removed from errand with id '%s'";

	private final DecisionRepository decisionRepository;
	private final DecisionOutcomeRepository decisionOutcomeRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final NamespaceConfigService namespaceConfigService;
	private final ErrandProcessRepository processRepository;

	DecisionValidator(final DecisionRepository decisionRepository, final DecisionOutcomeRepository decisionOutcomeRepository, final NamespaceConfigRepository namespaceConfigRepository,
		final NamespaceConfigService namespaceConfigService, final ErrandProcessRepository processRepository) {
		this.decisionRepository = decisionRepository;
		this.decisionOutcomeRepository = decisionOutcomeRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.namespaceConfigService = namespaceConfigService;
		this.processRepository = processRepository;
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
	 * <p>
	 * The process consumer is recognised by the value of {@code X-Sent-By}, whatever type the header gives it, as long as
	 * it is not an ad account. The header is set by the caller, so this holds the stamp to the intent of the caller rather
	 * than proving who it is - which is what the check is for: a decision cannot be given the method of the other kind of
	 * writer by mistake.
	 *
	 * @param namespace      namespace of the errand.
	 * @param municipalityId municipality of the errand.
	 * @param method         the method the decision ends up with. Null is left alone.
	 */
	public void validateMethod(final String namespace, final String municipalityId, final DecisionMethod method) {
		if (isNull(method)) {
			return;
		}

		final var identifier = Identifier.get();
		final var writtenByPerson = nonNull(identifier) && Identifier.Type.AD_ACCOUNT.equals(identifier.getType());

		if (method == MANUAL) {
			if (!writtenByPerson) {
				throw Problem.valueOf(FORBIDDEN, MANUAL_REQUIRES_AD_ACCOUNT);
			}
			return;
		}

		final var consumer = namespaceConfigService.getProcessConsumer(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(FORBIDDEN, AUTOMATIC_WITHOUT_PROCESS_CONSUMER.formatted(namespace, municipalityId)));

		if (writtenByPerson || isNull(identifier) || !consumer.equals(identifier.getValue())) {
			throw Problem.valueOf(FORBIDDEN, AUTOMATIC_REQUIRES_PROCESS_CONSUMER.formatted(consumer, namespace, municipalityId));
		}
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
	 * Rejects a write to a decision that can no longer be changed, or a new decision on an errand whose process has run to
	 * its end.
	 * <p>
	 * What is locked is the decision as stored. A patch concluding a decision is let through, and the next write after it
	 * is not - which is also what keeps a concluded decision from being taken back to a draft.
	 *
	 * @param errandId the errand the decision belongs to.
	 * @param decision the decision written to, or null when one is being created.
	 */
	public void validateChangeable(final String errandId, final DecisionEntity decision) {
		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);

		if (instances.isEmpty()) {
			return;
		}
		if (hasCompletedProcess(instances)) {
			throw Problem.valueOf(CONFLICT, PROCESS_LIFE_OVER.formatted(errandId));
		}
		if (nonNull(decision) && COMPLETED == decision.getStatus()) {
			throw Problem.valueOf(CONFLICT, DECISION_COMPLETED.formatted(decision.getId()));
		}
	}

	/**
	 * Rejects removing an attachment of the errand that a decision which can no longer be changed has linked.
	 * <p>
	 * The link would otherwise go with the attachment, through the cascade of the database and past every rule of the
	 * decision. Only removal through the API is held to this - the purge of an errand takes everything with it.
	 *
	 * @param namespace      namespace of the errand.
	 * @param municipalityId municipality of the errand.
	 * @param errandId       the errand the attachment belongs to.
	 * @param attachmentId   the attachment about to be removed.
	 */
	public void validateAttachmentRemovable(final String namespace, final String municipalityId, final String errandId, final String attachmentId) {
		validateNotRestedOnByLockedDecision(errandId,
			() -> decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(namespace, municipalityId, errandId, attachmentId),
			() -> decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(namespace, municipalityId, errandId, attachmentId, COMPLETED),
			ATTACHMENT_OF_LOCKED_DECISION.formatted(attachmentId, errandId));
	}

	/**
	 * Rejects removing an investigation that a decision which can no longer be changed rests on.
	 * <p>
	 * The database sets the reference of the decision to null when the investigation goes, which would change a locked
	 * decision past every rule of it - and the reference could not be put back, since the decision is locked.
	 *
	 * @param namespace       namespace of the errand.
	 * @param municipalityId  municipality of the errand.
	 * @param errandId        the errand the investigation belongs to.
	 * @param investigationId the investigation about to be removed.
	 */
	public void validateInvestigationRemovable(final String namespace, final String municipalityId, final String errandId, final String investigationId) {
		validateNotRestedOnByLockedDecision(errandId,
			() -> decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(namespace, municipalityId, errandId, investigationId),
			() -> decisionRepository.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus(namespace, municipalityId, errandId, investigationId, COMPLETED),
			INVESTIGATION_OF_LOCKED_DECISION.formatted(investigationId, errandId));
	}

	/**
	 * The lock of {@link #validateChangeable} for what a decision rests on, asked of the decisions rather than of one. Most
	 * of what is removed rests under no decision, and is let through without a look at the process.
	 */
	private void validateNotRestedOnByLockedDecision(final String errandId, final BooleanSupplier restedOn, final BooleanSupplier restedOnByCompleted, final String message) {
		if (!restedOn.getAsBoolean()) {
			return;
		}

		final var instances = processRepository.findByErrandIdOrderByCreatedDesc(errandId);

		if (instances.isEmpty()) {
			return;
		}
		if (hasCompletedProcess(instances) || restedOnByCompleted.getAsBoolean()) {
			throw Problem.valueOf(CONFLICT, message);
		}
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
