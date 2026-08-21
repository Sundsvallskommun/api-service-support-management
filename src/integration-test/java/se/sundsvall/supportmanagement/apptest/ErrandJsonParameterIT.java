package se.sundsvall.supportmanagement.apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * ErrandJsonParameter IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandJsonParameterIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandJsonParameterIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	// Namespace 2506 has access control on, with a role restricted to a single json parameter key.
	private static final String ACCESS_CONTROLLED_ERRAND = "/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8";

	@Test
	void test01_updateJsonParameterLeavesOtherKeysIntact() {
		// The endpoint writes one key at a time, so this guards against collateral damage rather than a wholesale
		// replace. smo02key holds FIRST_LINE, which reaches granted-json only. The endpoint answers 200 with the
		// stored parameter, for both create and update.
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND + "/json-parameters/granted-json")
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		// There is no list endpoint for json parameters, so the errand itself is read to see both keys at once: the one
		// that was updated, and the one the writer could not see and must not have lost. adm01adm holds no role and is
		// therefore unrestricted.
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND)
			.withHeader(SENT_BY_HEADER, "adm01adm; type=adAccount")
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response-privileged.json")
			.sendRequestAndVerifyResponse();
	}
}
