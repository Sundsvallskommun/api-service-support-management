package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * ErrandAccess IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandAccessIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandAccessIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";

	private static final String UNRESTRICTED_ERRAND = "/2281/NAMESPACE-1/errands/ec677eb3-604c-4935-bff7-f8f0b500c8f4/access";
	private static final String ACCESS_CONTROLLED_ERRAND = "/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/access";

	/**
	 * A namespace that has not switched access control on restricts nobody, which is reported as every field and every
	 * resource of the errand at read/write rather than as an absence of restrictions, so that a client reads one answer
	 * the same way whatever the namespace does.
	 */
	@Test
	void test01_unrestrictedNamespace() {
		setupCall()
			.withServicePath(UNRESTRICTED_ERRAND)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A case officer whose role exposes three fields and two individual keys. The keyed fields are held to read as a
	 * whole, since the role may change the keys it was granted but may not add keys of its own.
	 */
	@Test
	void test02_roleRestrictedUser() {
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND)
			.withHttpMethod(GET)
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The reporter of the errand, whom the access mapper grants nothing at all. They reach their own errand through the
	 * reporter exception alone, which the namespace grants at read, so nothing of it is reported writable.
	 */
	@Test
	void test03_reporter() {
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND)
			.withHttpMethod(GET)
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Labels reaching the errand at limited read only. The errand is reported at limited read, trimmed to the fields the
	 * namespace exposes for it, and the only resource beyond the errand itself is the one limited read is extended to.
	 */
	@Test
	void test04_limitedRead() {
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND)
			.withHttpMethod(GET)
			.withHeader(SENT_BY_HEADER, "lim01red; type=adAccount")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_unknownErrand() {
		setupCall()
			.withServicePath("/2281/NAMESPACE-1/errands/f8f0b500-c8f4-4935-bff7-ec677eb3604c/access")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A user the access mapper grants nothing, and who did not report the errand, is refused rather than answered with
	 * an empty report.
	 */
	@Test
	void test06_notAccessible() {
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND)
			.withHttpMethod(GET)
			.withHeader(SENT_BY_HEADER, "nob01ody; type=adAccount")
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}
}
