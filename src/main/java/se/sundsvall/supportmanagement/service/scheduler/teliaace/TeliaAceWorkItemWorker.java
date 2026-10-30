package se.sundsvall.supportmanagement.service.scheduler.teliaace;

import java.time.Duration;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.exception.ClientProblem;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceContactRepository;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceContactEntity;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.integration.teliaace.AddWorkItemRequest;
import se.sundsvall.supportmanagement.integration.teliaace.TeliaAceClient;
import se.sundsvall.supportmanagement.integration.teliaace.UpdateContactDataRequest;

import static java.time.OffsetDateTime.now;

/**
 * Delivers a queued work item to Telia ACE: creates a work item the first time an errand is notified, and updates
 * it after that. Drops the item if it is stale, its errand no longer exists, or Telia ACE rejects the request
 * outright. An auth or rate-limit response ({@link #PROPAGATED_STATUSES}) instead propagates, so the item stays
 * queued for retry and the scheduler's health indicator turns red. Not transactional itself - the HTTP call can
 * take up to the configured read timeout, and each database operation below is transactional in its own right.
 */
@Component
public class TeliaAceWorkItemWorker {

	private static final Logger LOG = LoggerFactory.getLogger(TeliaAceWorkItemWorker.class);

	private static final String ENTRANCE = "wiDraken";
	private static final String ERRAND = "w01";
	private static final String WORKITEM_SOURCE = "draken";
	private static final String WORKITEM_INSTRUCTION = "Öppna ärendet i Draken";
	private static final String CUSTOM_KEY_PREDEFINED_AGENT_NAME = "predefTargetAgentName";

	// Per InterfaceSpecification_WorkItemInterfaceProxy_REST_rev8.0.pdf §3.1, AddWorkItem's "workitem" field reads
	// back through UpdateContactData as the custom data key named "workItem".
	private static final String CONTACT_DATA_KEY_WORKITEM = "workItem";
	private static final String UNKNOWN_SENDER = "okänd avsändare";

	// Must be told apart from a work item Telia ACE rejects outright (TeliaAceConfiguration puts these statuses in
	// ProblemErrorDecoder's bypass list so ClientProblem.getStatus() carries them instead of a normalized BAD_GATEWAY).
	private static final Set<HttpStatus> PROPAGATED_STATUSES = EnumSet.of(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.TOO_MANY_REQUESTS);

	@Value("${scheduler.telia-ace-work-item.max-age:P3D}")
	private Duration maxAge = Duration.ofDays(3);

	private final TeliaAceWorkItemRepository workItemRepository;
	private final TeliaAceContactRepository contactRepository;
	private final ErrandsRepository errandsRepository;
	private final TeliaAceClient teliaAceClient;

	public TeliaAceWorkItemWorker(final TeliaAceWorkItemRepository workItemRepository, final TeliaAceContactRepository contactRepository, final ErrandsRepository errandsRepository, final TeliaAceClient teliaAceClient) {
		this.workItemRepository = workItemRepository;
		this.contactRepository = contactRepository;
		this.errandsRepository = errandsRepository;
		this.teliaAceClient = teliaAceClient;
	}

	@Transactional(readOnly = true)
	public List<TeliaAceWorkItemEntity> fetchProcessable() {
		return workItemRepository.findTop6ByOrderByCreatedAsc();
	}

	public void process(final TeliaAceWorkItemEntity workItem) {
		if (isStale(workItem)) {
			LOG.warn("Dropping Telia ACE work item for errand {} undelivered: queued since {}, past the {} max age", workItem.getErrandId(), workItem.getCreated(), maxAge);
			workItemRepository.delete(workItem);
			return;
		}

		if (!errandsRepository.existsByIdAndDeletedIsNull(workItem.getErrandId())) {
			LOG.warn("Dropping Telia ACE work item for errand {}: the errand no longer exists", workItem.getErrandId());
			workItemRepository.delete(workItem);
			return;
		}

		final var contact = contactRepository.findById(workItem.getErrandId()).orElse(null);
		if (contact == null) {
			add(workItem);
		} else {
			update(workItem, contact);
		}
	}

	/**
	 * Creates a work item in Telia ACE and stores the returned {@code emailUid} for the errand. Drops the queued item
	 * if Telia ACE rejects the request or returns no {@code emailUid}. If storing the {@code emailUid} fails, the
	 * item stays queued and the retry creates a second work item.
	 */
	private void add(final TeliaAceWorkItemEntity workItem) {
		final AddWorkItemRequest request = toAddRequest(workItem);
		final String emailUid;
		try {
			final var response = teliaAceClient.addWorkItem(request);
			emailUid = response == null ? null : response.emailUid();
		} catch (final ClientProblem e) {
			if (PROPAGATED_STATUSES.contains(e.getStatus())) {
				throw e;
			}
			LOG.error("Telia ACE rejected work item for errand {} as a bad request, dropping it: {}", workItem.getErrandId(), e.getMessage());
			workItemRepository.delete(workItem);
			return;
		}

		if (StringUtils.isBlank(emailUid)) {
			LOG.error("Telia ACE returned no emailUid for errand {}, dropping the work item rather than retrying a non-idempotent call", workItem.getErrandId());
			workItemRepository.delete(workItem);
			return;
		}

		contactRepository.save(TeliaAceContactEntity.create().withErrandId(workItem.getErrandId()).withEmailUid(emailUid));
		workItemRepository.delete(workItem);
	}

	/**
	 * Updates the errand's existing work item. If Telia ACE rejects the update, clears the stored contact and
	 * creates a new work item instead.
	 */
	private void update(final TeliaAceWorkItemEntity workItem, final TeliaAceContactEntity contact) {
		try {
			teliaAceClient.updateContactData(contact.getEmailUid(), toUpdateRequest(workItem));
		} catch (final ClientProblem e) {
			if (PROPAGATED_STATUSES.contains(e.getStatus())) {
				throw e;
			}
			LOG.error("Telia ACE rejected the update for errand {} and emailUid {}, clearing the stale contact and adding a fresh work item instead: {}", workItem.getErrandId(), contact.getEmailUid(), e.getMessage());
			contactRepository.delete(contact);
			add(workItem);
			return;
		}

		workItemRepository.delete(workItem);
	}

	private boolean isStale(final TeliaAceWorkItemEntity workItem) {
		return workItem.getCreated() != null && workItem.getCreated().isBefore(now(ZoneId.systemDefault()).minus(maxAge));
	}

	private AddWorkItemRequest toAddRequest(final TeliaAceWorkItemEntity workItem) {
		return new AddWorkItemRequest(
			workItem.getFromAddress(),
			workItem.getSubject(),
			ENTRANCE,
			ERRAND,
			WORKITEM_INSTRUCTION,
			WORKITEM_SOURCE,
			workItem.getContentUrl(),
			toCustomKeys(workItem));
	}

	private UpdateContactDataRequest toUpdateRequest(final TeliaAceWorkItemEntity workItem) {
		final var sender = Optional.ofNullable(workItem.getFromAddress()).orElse(UNKNOWN_SENDER);
		final var customKeys = new HashMap<>(Optional.ofNullable(toCustomKeys(workItem)).orElse(Map.of()));
		customKeys.put(CONTACT_DATA_KEY_WORKITEM, "Nytt meddelande mottaget från %s. %s".formatted(sender, WORKITEM_INSTRUCTION));
		return new UpdateContactDataRequest(customKeys);
	}

	private Map<String, String> toCustomKeys(final TeliaAceWorkItemEntity workItem) {
		return Optional.ofNullable(workItem.getPredefinedAgentName())
			.map(agentName -> Map.of(CUSTOM_KEY_PREDEFINED_AGENT_NAME, agentName))
			.orElse(null);
	}
}
