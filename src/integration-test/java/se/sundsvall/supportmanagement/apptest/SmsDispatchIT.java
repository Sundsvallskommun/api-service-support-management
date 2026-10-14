package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.supportmanagement.integration.db.model.enums.NotificationChannelType.SMS;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.NotificationDispatchRepository;
import se.sundsvall.supportmanagement.service.scheduler.notificationdispatch.NotificationDispatchScheduler;
import se.sundsvall.supportmanagement.service.scheduler.notificationdispatch.NotificationDispatchWorker;

@WireMockAppTestSuite(files = "classpath:/SmsDispatchIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-sms-dispatch-it.sql"
})
class SmsDispatchIT extends AbstractAppTest {

	private static final String NAMESPACE_1_UPDATE_DISPATCH_ID = "33333333-0000-0000-0000-000000000001";
	private static final String NAMESPACE_2_UPDATE_DISPATCH_ID = "33333333-0000-0000-0000-000000000002";
	private static final String NAMESPACE_1_CREATE_DISPATCH_ID = "33333333-0000-0000-0000-000000000003";
	private static final String SMS_SUBSCRIBER_ID = "11111111-0000-0000-0000-000000000001";

	@Autowired
	private NotificationDispatchWorker notificationDispatchWorker;

	@Autowired
	private NotificationDispatchScheduler notificationDispatchScheduler;

	@Autowired
	private NotificationDispatchOutboxRepository notificationDispatchOutboxRepository;

	@Autowired
	private NotificationDispatchRepository notificationDispatchRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	/**
	 * A subscriber with an SMS channel in a namespace that has enabled sms is texted about the errand, with a link to it,
	 * by the same run that dispatched the event, and the outbox is emptied.
	 */
	@Test
	void test01_smsSubscriberIsTextedOnUpdate() {
		setupCall();
		notificationDispatchRepository.deleteAllById(List.of(NAMESPACE_2_UPDATE_DISPATCH_ID, NAMESPACE_1_CREATE_DISPATCH_ID));

		final var dispatchEntries = notificationDispatchWorker.fetchProcessable();
		notificationDispatchWorker.processGroup(dispatchEntries);

		transactionTemplate.executeWithoutResult(_ -> assertThat(notificationDispatchOutboxRepository.findAll())
			.singleElement()
			.satisfies(outbox -> {
				assertThat(outbox.getSubscriber().getId()).isEqualTo(SMS_SUBSCRIBER_ID);
				assertThat(outbox.getChannel()).isEqualTo(SMS);
				assertThat(outbox.getErrandNumber()).isEqualTo("NS1-2024-000001");
			}));

		notificationDispatchScheduler.processDispatch();

		assertThat(notificationDispatchRepository.findAll()).isEmpty();
		assertThat(notificationDispatchOutboxRepository.findAll()).isEmpty();
		verifyStubs();
	}

	/**
	 * An event routed to sms in a namespace that has not enabled sms is not put in the outbox, so no sms is sent.
	 */
	@Test
	void test02_namespaceWithoutSmsIsNotTexted() {
		setupCall();
		notificationDispatchRepository.deleteAllById(List.of(NAMESPACE_1_UPDATE_DISPATCH_ID, NAMESPACE_1_CREATE_DISPATCH_ID));

		final var dispatchEntries = notificationDispatchWorker.fetchProcessable();
		assertThat(dispatchEntries).singleElement().satisfies(entry -> assertThat(entry.getNamespace()).isEqualTo("NAMESPACE-2"));

		notificationDispatchWorker.processGroup(dispatchEntries);

		assertThat(notificationDispatchRepository.findAll()).isEmpty();
		assertThat(notificationDispatchOutboxRepository.findAll()).isEmpty();
	}

	/**
	 * A profile routing new errands to sms reaches a member who only has the internal channel, and texts them on the
	 * mobile number of the employee directory, made into an MSISDN.
	 */
	@Test
	void test03_profileTextsMemberOnTheirEmployeeMobileNumber() {
		setupCall();
		notificationDispatchRepository.deleteAllById(List.of(NAMESPACE_1_UPDATE_DISPATCH_ID, NAMESPACE_2_UPDATE_DISPATCH_ID));

		notificationDispatchScheduler.processDispatch();

		assertThat(notificationDispatchRepository.findAll()).isEmpty();
		assertThat(notificationDispatchOutboxRepository.findAll()).isEmpty();
		verifyStubs();
	}
}
