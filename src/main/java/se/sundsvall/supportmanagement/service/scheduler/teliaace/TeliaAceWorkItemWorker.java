package se.sundsvall.supportmanagement.service.scheduler.teliaace;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.TeliaAceWorkItemRepository;
import se.sundsvall.supportmanagement.integration.db.model.TeliaAceWorkItemEntity;
import se.sundsvall.supportmanagement.integration.teliaace.AddWorkItemRequest;
import se.sundsvall.supportmanagement.integration.teliaace.TeliaAceClient;

import static java.time.OffsetDateTime.now;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
public class TeliaAceWorkItemWorker {

	private static final Logger LOG = LoggerFactory.getLogger(TeliaAceWorkItemWorker.class);

	private static final String ENTRANCE = "Draken";
	private static final String ERRAND = "w01";
	private static final String WORKITEM_INSTRUCTION = "Öppna ärendet i Draken";
	private static final String CUSTOM_KEY_PREDEFINED_AGENT_NAME = "predefinedAgentName";

	/**
	 * How old a queued work item may get before it is dropped undelivered instead of retried again. Without this, a
	 * work item that can never succeed - a stale namespace config, an errand that no longer exists - would otherwise be
	 * retried forever, and by the time it is this old its operational value (alerting a case worker to an active
	 * contact) has passed anyway.
	 */
	@Value("${scheduler.telia-ace-work-item.max-age:P3D}")
	private Duration maxAge = Duration.ofDays(3);

	private final TeliaAceWorkItemRepository workItemRepository;
	private final TeliaAceClient teliaAceClient;

	public TeliaAceWorkItemWorker(final TeliaAceWorkItemRepository workItemRepository, final TeliaAceClient teliaAceClient) {
		this.workItemRepository = workItemRepository;
		this.teliaAceClient = teliaAceClient;
	}

	@Transactional(readOnly = true)
	public List<TeliaAceWorkItemEntity> fetchProcessable() {
		return workItemRepository.findAllByOrderByCreatedAsc();
	}

	/**
	 * Delivers one queued work item to Telia ACE and removes it. The row is only deleted once delivery succeeds; a
	 * failure here is left to propagate so the row is rolled back and retried on the next scheduler run. A work item
	 * past {@code maxAge} is dropped without an attempt, so it does not count as a failure.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	public void process(final TeliaAceWorkItemEntity workItem) {
		if (isStale(workItem)) {
			LOG.warn("Dropping Telia ACE work item for errand {} undelivered: queued since {}, past the {} max age", workItem.getErrandId(), workItem.getCreated(), maxAge);
			workItemRepository.delete(workItem);
			return;
		}

		teliaAceClient.addWorkItem(toRequest(workItem));
		workItemRepository.delete(workItem);
	}

	private boolean isStale(final TeliaAceWorkItemEntity workItem) {
		return workItem.getCreated() != null && workItem.getCreated().isBefore(now(ZoneId.systemDefault()).minus(maxAge));
	}

	private AddWorkItemRequest toRequest(final TeliaAceWorkItemEntity workItem) {
		final var customKeys = Optional.ofNullable(workItem.getPredefinedAgentName())
			.map(agentName -> Map.of(CUSTOM_KEY_PREDEFINED_AGENT_NAME, agentName))
			.orElse(Map.of());

		return new AddWorkItemRequest(
			workItem.getFromAddress(),
			workItem.getSubject(),
			ENTRANCE,
			ERRAND,
			WORKITEM_INSTRUCTION,
			workItem.getContentUrl(),
			customKeys);
	}
}
