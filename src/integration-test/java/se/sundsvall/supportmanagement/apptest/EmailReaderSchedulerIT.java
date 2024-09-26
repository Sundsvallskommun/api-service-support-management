package se.sundsvall.supportmanagement.apptest;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.service.scheduler.emailreader.EmailReaderScheduler;


@WireMockAppTestSuite(files = "classpath:/EmailReaderSchedulerIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class EmailReaderSchedulerIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE.1/errands";

	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private EmailReaderScheduler emailReaderScheduler;

	@Test
	void test01_getAndProcessEmails() {
		// Initialize
		setupCall();
		// Process mails
		emailReaderScheduler.getAndProcessEmails();
		// Verify created errands
		setupCall()
			.withServicePath(PATH + "?filter=category:'CATEGORY-1' and type:'TYPE-1' and stakeholders.contactChannels.type:'EMAIL'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();
		// Verify mocks
		verify(2, deleteRequestedFor(urlMatching("/api-emailreader/2281/email/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
		verifyStubs();

	}

}
