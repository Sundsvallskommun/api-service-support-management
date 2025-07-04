package se.sundsvall.supportmanagement.service.scheduler.messageexchange;

import generated.se.sundsvall.messageexchange.Conversation;
import java.util.Comparator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.supportmanagement.integration.db.model.MessageExchangeSyncEntity;

@Service
public class MessageExchangeScheduler {

	@Autowired
	@Lazy
	private MessageExchangeScheduler self;

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MessageExchangeScheduler.class);

	@Value("${scheduler.messageexchange.enabled:true}")
	private boolean isSchedulerEnabled;
	private final MessageExchangeWorker messageExchangeWorker;
	private final AsyncTaskExecutor asyncTaskExecutor;

	public MessageExchangeScheduler(final MessageExchangeWorker messageExchangeWorker,
		@Qualifier("taskScheduler") final AsyncTaskExecutor asyncTaskExecutor) {

		this.messageExchangeWorker = messageExchangeWorker;
		this.asyncTaskExecutor = asyncTaskExecutor;

	}

	@Dept44Scheduled(
		cron = "${scheduler.messageexchange.cron}",
		name = "${scheduler.messageexchange.name}",
		lockAtMostFor = "${scheduler.messageexchange.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.messageexchange.maximum-execution-time}")
	public void syncConversations() {
		if (!isSchedulerEnabled) {
			LOGGER.info("scheduler.messageexchange.enabled=false skipping scheduled execution");
			return;
		}

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

	public void triggerSyncConversationsAsync() {
		if (!isSchedulerEnabled) {
			LOGGER.info("scheduler.messageexchange.enable=false skipping triggered execution");
			return;
		}
		LOGGER.info("Initiating async trigger for syncConversations");
		asyncTaskExecutor.execute(self::syncConversations);
		LOGGER.info("Async trigger for syncConversations initiated successfully. Calling thread continues.");
	}

}
