package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.RevisionEntity;

/**
 * Errand IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandsIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String ACCESS_CONTROLLED_ERRAND = "/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private RevisionRepository revisionRepository;

	@Test
	void test01_getAllErrandsSortedByTouched() {
		setupCall()
			.withServicePath(PATH + "?sort=touched,desc")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getErrandsByFilter() {
		setupCall()
			.withServicePath(PATH + "?filter=category:'CATEGORY-1' and concat(stakeholders.firstName, ' ', stakeholders.lastName) ~ '%FIRST_NAME-1 LAST_NAME-1%'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getErrandsById() {
		setupCall()
			.withServicePath(PATH + "/1be673c0-6ba3-4fb0-af4a-43acf23389f6")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_postErrand() {
		final var headers = setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath(PATH.replace("NAMESPACE-1", "CONTACTCENTER"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/2281/CONTACTCENTER/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequest()
			.getResponseHeaders();

		setupCall()
			.withServicePath(headers.get(LOCATION).stream().findFirst().get())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_patchErrand() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, id)).hasSize(1)
			.extracting(RevisionEntity::getVersion)
			.containsExactly(0);

		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, id)).hasSize(2)
			.extracting(RevisionEntity::getVersion)
			.containsExactlyInAnyOrder(0, 1);
	}

	@Test
	void test06_deleteErrand() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, id)).hasSize(1);
		assertThat(errandsRepository.existsById(id)).isTrue();

		setupCall()
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.existsById(id)).isFalse();
		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, id)).hasSize(1);
	}

	@Test
	void test07_getErrandsByLabelFilter() {
		setupCall()
			.withServicePath(PATH + "?filter=labels.metadataLabel.resourcePath:'CATEGORY-1/TYPE-2/SUBTYPE-4/DEEPSUBTYPE-1'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_getErrandsByParametersFilter() {
		setupCall()
			.withServicePath(PATH + "?filter=parameters.values~'B1'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_countErrands() {
		setupCall()
			.withServicePath(PATH + "/count?filter=category:'CATEGORY-1' and concat(stakeholders.firstName, ' ', stakeholders.lastName) ~ '%FIRST_NAME-1 LAST_NAME-1%'")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test10_getErrandsWithAccessControlFull() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands?sort=created,desc")
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test11_getErrandsWithAccessControlPartial() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands?sort=created,desc")
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test12_getErrandsWithAccessControlNone() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands?sort=created,desc")
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test13_getErrandsWithAccessControlPartialWithSplitLimitedMapping() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands?sort=created,desc")
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test14_postErrandWithReferredFrom() {
		final var headers = setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath("/2281/CONTACTCENTER/errands?referredFrom=REFERRED_FROM|originalErrandId;someType;someService;someNamespace|")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/2281/CONTACTCENTER/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequest()
			.getResponseHeaders();

		setupCall()
			.withServicePath(headers.get(LOCATION).stream().findFirst().get())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test15_postErrandWithJsonParameters() {
		final var headers = setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath(PATH.replace("NAMESPACE-1", "CONTACTCENTER"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/2281/CONTACTCENTER/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequest()
			.getResponseHeaders();

		setupCall()
			.withServicePath(headers.get(LOCATION).stream().findFirst().get())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test16_patchErrandWithJsonParameters() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, id)).hasSize(1)
			.extracting(RevisionEntity::getVersion)
			.containsExactly(0);

		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(revisionRepository.findAllByNamespaceAndMunicipalityIdAndEntityIdOrderByVersion(NAMESPACE, MUNICIPALITY_ID, id)).hasSize(2)
			.extracting(RevisionEntity::getVersion)
			.containsExactlyInAnyOrder(0, 1);
	}

	@Test
	void test17_postErrandWithInvalidJsonParameters() {
		setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath(PATH.replace("NAMESPACE-1", "CONTACTCENTER"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test18_patchErrandWithInvalidJsonParameters() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test19_postErrandWithJsonSchemaServerError() {
		setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath(PATH.replace("NAMESPACE-1", "CONTACTCENTER"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(INTERNAL_SERVER_ERROR)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test20_patchErrandWithJsonSchemaServerError() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(INTERNAL_SERVER_ERROR)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test21_postErrandWithPhase() {
		final var headers = setupCall()
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withServicePath(PATH.replace("NAMESPACE-1", "CONTACTCENTER"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/2281/CONTACTCENTER/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequest()
			.getResponseHeaders();

		setupCall()
			.withServicePath(headers.get(LOCATION).stream().findFirst().get())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test22_patchErrandWithPhaseTransition() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		// Set initial phase on the errand
		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(PATCH)
			.withRequest("request-initial-phase.json")
			.withExpectedResponseStatus(OK)
			.sendRequest();

		// Transition to the next phase
		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		// Verify phase transition by GET
		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test23_getErrandAsReporterWithoutLabelAccess() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test24_getErrandNotReportedByRequestingUser() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/c9efe03d-deff-4828-a043-541fa78ffdeb")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test25_getErrandAsReporterWithPartyIdIdentifier() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=partyId")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test26_patchErrandAsReporterIsNotAllowed() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test27_getNotesAsReporterIsNotAllowed() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/notes")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test28_getGrantedParameterAsReporter() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/parameters/granted-key")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test29_getUngrantedParameterAsReporter() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/parameters/hidden-key")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test30_getParametersAsReporterListsGrantedKeysOnly() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/parameters")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test31_patchUngrantedParameterAsReporterIsNotAllowed() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/parameters/hidden-key")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test32_deleteUngrantedParameterAsReporterIsNotAllowed() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/parameters/hidden-key")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test33_patchParametersWithUngrantedKeyAsReporterIsNotAllowed() {
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/parameters")
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test34_getCommunicationOnLimitedReadErrand() {
		// joe01doe holds limited read for this errand, and the namespace extends limited read to the communication.
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/communication")
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test35_getNotesOnLimitedReadErrandIsNotAllowed() {
		// The namespace does not extend limited read to notes, so the same user is refused here.
		setupCall()
			.withServicePath("/2506/NAMESPACE-2506/errands/58c41b44-0b9f-413d-bd46-406d24bf5ca8/notes")
			.withHeader(SENT_BY_HEADER, "joe01doe; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(UNAUTHORIZED)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test36_patchErrandKeepsKeysTheUserMayNotSee() {
		// smo02key holds FIRST_LINE, restricted to one parameter key and one json parameter key, and writes through the
		// whole errand PATCH. Patching back only what they were served must not delete what they never saw.
		setupCall()
			.withServicePath(ACCESS_CONTROLLED_ERRAND)
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		// Read back as a user holding no role, who is therefore unrestricted and sees every key. Only the two keyed
		// collections are asserted, so the test does not couple to the rest of the payload.
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
