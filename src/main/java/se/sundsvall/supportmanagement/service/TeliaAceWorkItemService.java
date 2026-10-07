package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.emailreader.Email;
import org.springframework.stereotype.Service;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;

import static java.lang.Boolean.TRUE;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_TELIA_ACE_WORK_ITEM_BASE_URL;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_TELIA_ACE_WORK_ITEM_ENABLED;

/**
 * Queues Telia ACE work items for errands created or updated via EmailReader, so that a case worker can be alerted
 * in Telia ACE and jump straight to the errand in Draken. Queued rather than sent directly, so that EmailReader
 * processing is unaffected by Telia ACE being temporarily unavailable - delivery happens on a best-effort basis via
 * {@code TeliaAceWorkItemScheduler}.
 * <p>
 * Restricted per namespace via {@link ConfigPropertyExtractor#PROPERTY_TELIA_ACE_WORK_ITEM_ENABLED}, since this is
 * currently only wanted for Kontakt Sundsvall.
 */
@Service
public class TeliaAceWorkItemService {

	private static final String SUBJECT_NEW_ERRAND = "Nytt ärende i Draken";
	private static final String SUBJECT_UPDATED_ERRAND = "Uppdaterat ärende i Draken";
	private static final String ERRAND_LINK_FORMAT = "%s/arende/%s";

	private final TeliaAceWorkItemRepository workItemRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;

	public TeliaAceWorkItemService(final TeliaAceWorkItemRepository workItemRepository, final NamespaceConfigRepository namespaceConfigRepository) {
		this.workItemRepository = workItemRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
	}

	/**
	 * Queues a work item for a newly created errand. Goes to Telia ACE's general work item queue, with no agent
	 * pre-assigned.
	 */
	public void enqueueForNewErrand(final ErrandEntity errand, final Email email) {
		enqueue(errand, email, SUBJECT_NEW_ERRAND, null);
	}

	/**
	 * Queues a work item for an errand updated via email, provided it already has an assigned case worker. Their
	 * AD account is sent along so Telia ACE can route the work item to their personal queue.
	 */
	public void enqueueForUpdatedErrand(final ErrandEntity errand, final Email email) {
		if (errand.getAssignedUserId() == null) {
			return;
		}
		enqueue(errand, email, SUBJECT_UPDATED_ERRAND, errand.getAssignedUserId());
	}

	private void enqueue(final ErrandEntity errand, final Email email, final String subject, final String assignedUserId) {
		final var namespaceConfig = namespaceConfigRepository.findByNamespaceAndMunicipalityId(errand.getNamespace(), errand.getMunicipalityId()).orElse(null);
		if (!TRUE.equals(ConfigPropertyExtractor.<Boolean>getNullableValue(namespaceConfig, PROPERTY_TELIA_ACE_WORK_ITEM_ENABLED))) {
			return;
		}

		final var baseUrl = ConfigPropertyExtractor.<String>getValue(namespaceConfig, PROPERTY_TELIA_ACE_WORK_ITEM_BASE_URL);

		workItemRepository.save(TeliaAceWorkItemEntity.create()
			.withErrandId(errand.getId())
			.withMunicipalityId(errand.getMunicipalityId())
			.withNamespace(errand.getNamespace())
			.withFromAddress(email.getSender())
			.withSubject(subject)
			.withContentUrl(ERRAND_LINK_FORMAT.formatted(baseUrl, errand.getErrandNumber()))
			.withPredefinedAgentName(assignedUserId));
	}
}
