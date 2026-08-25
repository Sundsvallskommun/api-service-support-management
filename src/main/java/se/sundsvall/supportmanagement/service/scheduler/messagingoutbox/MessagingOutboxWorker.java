package se.sundsvall.supportmanagement.service.scheduler.messagingoutbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.messaging.EmailBatchRequest;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.integration.db.MessagingOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.MessagingOutboxEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;

import static java.time.OffsetDateTime.now;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
public class MessagingOutboxWorker {

	private static final Logger LOG = LoggerFactory.getLogger(MessagingOutboxWorker.class);
	private static final int MAX_RETRIES = 5;
	static final String TYPE_EMAIL = "EMAIL";

	private final MessagingOutboxRepository repository;
	private final MessagingClient messagingClient;
	private final ObjectMapper objectMapper;

	public MessagingOutboxWorker(final MessagingOutboxRepository repository, final MessagingClient messagingClient, final ObjectMapper objectMapper) {
		this.repository = repository;
		this.messagingClient = messagingClient;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public List<MessagingOutboxEntity> fetchProcessable() {
		return repository.findProcessable(now(ZoneId.systemDefault()));
	}

	@Transactional
	public void enqueue(final String municipalityId, final EmailBatchRequest request) {
		try {
			final var payload = objectMapper.writeValueAsString(request);
			repository.save(MessagingOutboxEntity.create()
				.withMunicipalityId(municipalityId)
				.withMessageType(TYPE_EMAIL)
				.withPayload(payload));
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to serialize email batch request for outbox", e);
		}
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void process(final MessagingOutboxEntity entry) {
		try {
			if (TYPE_EMAIL.equals(entry.getMessageType())) {
				final var request = objectMapper.readValue(entry.getPayload(), EmailBatchRequest.class);
				messagingClient.sendEmailBatch(entry.getMunicipalityId(), request);
			} else {
				LOG.warn("Unknown message type {} in outbox entry {}, marking as dead letter", entry.getMessageType(), entry.getId());
				entry.setDeadLetter(true);
				repository.save(entry);
				return;
			}
			repository.delete(entry);
		} catch (final Exception e) {
			LOG.error("Failed to send outbox entry {}, retry count {}", entry.getId(), entry.getRetryCount(), e);
			final int retryCount = entry.getRetryCount() + 1;
			if (retryCount >= MAX_RETRIES) {
				LOG.error("Outbox entry {} exceeded max retries, marking as dead letter", entry.getId());
				entry.setDeadLetter(true);
			} else {
				entry.setRetryCount(retryCount);
				entry.setNextRetryAt(now(ZoneId.systemDefault()).plus(Duration.ofMinutes(5L * retryCount)));
			}
			repository.save(entry);
		}
	}
}
