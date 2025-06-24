package se.sundsvall.supportmanagement.service.scheduler.messageexchange;

import generated.se.sundsvall.messageexchange.Conversation;
import java.util.Comparator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

@Service
@ConditionalOnProperty(prefix = "scheduler.messageexchange", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageExchangeScheduler {

	private final MessageExchangeWorker messageExchangeWorker;

	public MessageExchangeScheduler(final MessageExchangeWorker messageExchangeWorker) {
		this.messageExchangeWorker = messageExchangeWorker;
	}

	@Dept44Scheduled(
		cron = "${scheduler.messageexchange.cron}",
		name = "${scheduler.messageexchange.name}",
		lockAtMostFor = "${scheduler.messageexchange.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.messageexchange.maximum-execution-time}")
	void syncConversations() {
		messageExchangeWorker.getActiveSyncEntities()
			.forEach(syncEntity -> {
				var page = processConversationPage(messageExchangeWorker.getConversations(syncEntity, Pageable.ofSize(100)), syncEntity);
				while (page.hasNext()) {
					page = processConversationPage(messageExchangeWorker.getConversations(syncEntity, page.nextPageable()), syncEntity);
				}
				messageExchangeWorker.saveSyncEntity(syncEntity);
			});
	}

	/**
	 * Process all conversations in page. Sets the highest sequence number on syncEntity
	 *
	 * @param  conversationPage page to process
	 * @param  syncEntity       Updated with the highest sequence number from conversation page
	 * @return                  same as param conversationPage
	 */
	private Page<Conversation> processConversationPage(Page<Conversation> conversationPage, MessageExchangeSyncEntity syncEntity) {
		conversationPage.stream()
			.map(messageExchangeWorker::processConversation)
			.max(Comparator.comparingLong(Conversation::getLatestSequenceNumber))
			.map(Conversation::getLatestSequenceNumber)
			.filter(latestSeqNrInConversationPage -> syncEntity.getLatestSyncedSequenceNumber().compareTo(latestSeqNrInConversationPage) < 0)
			.ifPresent(syncEntity::setLatestSyncedSequenceNumber);
		return conversationPage;
	}

}
