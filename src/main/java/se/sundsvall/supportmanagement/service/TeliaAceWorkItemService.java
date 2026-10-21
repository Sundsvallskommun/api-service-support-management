package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.emailreader.Email;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * Queues a Telia ACE work item for an errand touched by the email reader. Queuing happens after the errand's own
 * transaction has committed, via {@link #onTeliaAceNotificationRequested}, so the queued row can never reference an
 * errand that ends up not existing.
 */
@Service
public class TeliaAceWorkItemService {

	private static final Logger LOG = LoggerFactory.getLogger(TeliaAceWorkItemService.class);

	private static final String SUBJECT = "Nytt ärende i Draken";

	// A global kill switch independent of the per-namespace toggle, so the feature can be turned on for every
	// namespace at once once it is ready for production, without editing namespace config rows before then. Off by
	// default: the feature must be deliberately enabled, not accidentally left on.
	@Value("${scheduler.telia-ace-work-item.enabled:false}")
	private boolean enabled = false;

	private final TeliaAceWorkItemRepository workItemRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;
	private final NamespaceConfigMapper namespaceConfigMapper;
	private final TransactionTemplate requiresNewTransaction;

	public TeliaAceWorkItemService(final TeliaAceWorkItemRepository workItemRepository, final NamespaceConfigRepository namespaceConfigRepository, final NamespaceConfigMapper namespaceConfigMapper,
		final PlatformTransactionManager transactionManager) {
		this.workItemRepository = workItemRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
		this.namespaceConfigMapper = namespaceConfigMapper;
		this.requiresNewTransaction = new TransactionTemplate(transactionManager);
		this.requiresNewTransaction.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
	}

	/**
	 * Runs {@link #notify} in its own {@code REQUIRES_NEW} transaction once the publishing transaction has committed,
	 * catching any failure - including one that only surfaces when that transaction itself commits - and signalling
	 * {@code setUnHealthyConsumer} rather than losing the notification silently. Runs immediately instead if no
	 * transaction happens to be active when the event is published ({@code fallbackExecution = true}).
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onTeliaAceNotificationRequested(final TeliaAceNotificationRequestedEvent event) {
		try {
			requiresNewTransaction.executeWithoutResult(status -> notify(event.errand(), event.email()));
		} catch (final Exception e) {
			LOG.warn("Failed to queue Telia ACE work item for errand {}: {}", event.errand().getId(), e.getMessage());
			event.setUnHealthyConsumer().accept("Failed to queue Telia ACE work item for errand " + event.errand().getId());
		}
	}

	void notify(final ErrandEntity errand, final Email email) {
		if (!enabled) {
			return;
		}

		final var namespaceConfig = namespaceConfigRepository.findByNamespaceAndMunicipalityId(errand.getNamespace(), errand.getMunicipalityId())
			.map(namespaceConfigMapper::toNamespaceConfig)
			.orElse(null);
		if (namespaceConfig == null || !namespaceConfig.isTeliaAceWorkItemEnabled()) {
			return;
		}

		final var baseUrl = Strings.CS.removeEnd(namespaceConfig.getTeliaAceWorkItemBaseUrl(), "/");

		workItemRepository.save(TeliaAceWorkItemEntity.create()
			.withErrandId(errand.getId())
			.withMunicipalityId(errand.getMunicipalityId())
			.withNamespace(errand.getNamespace())
			.withFromAddress(email.getSender())
			.withSubject(SUBJECT)
			.withContentUrl("%s/arende/%s".formatted(baseUrl, errand.getErrandNumber()))
			.withPredefinedAgentName(errand.getAssignedUserId()));
	}
}
