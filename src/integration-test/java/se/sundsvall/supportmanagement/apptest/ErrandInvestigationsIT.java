package se.sundsvall.supportmanagement.apptest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.IF_MATCH;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Errand Investigations IT tests, including the sections an investigation is assessed in, the attachments linked to it,
 * the JSON parameters it and its sections own and the parameters they carry.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandInvestigationsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
@SqlMergeMode(MERGE)
class ErrandInvestigationsIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";
	private static final String INVESTIGATION_ID = "f2000000-0000-0000-0000-000000000001";
	private static final String DECISION_ID = "f4000000-0000-0000-0000-000000000001";
	private static final String SECTION_ID = "f3000000-0000-0000-0000-000000000002";
	private static final String LINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000001";
	private static final String UNLINKED_ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000002";

	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String PATH = ERRAND_PATH + "/investigations";
	private static final String INVESTIGATION_PATH = PATH + "/" + INVESTIGATION_ID;
	private static final String SECTION_PATH = INVESTIGATION_PATH + "/sections/" + SECTION_ID;
	private static final String DECISION_PATH = ERRAND_PATH + "/decisions/" + DECISION_ID;
	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String GET_RESPONSE_FILE = "get-response.json";
	private static final String AD_ACCOUNT = "joe01doe; type=adAccount";
	private static final String MODIFIED_BY_THE_CALLER = "update investigation set modified_by = 'joe01doe' where id = '" + INVESTIGATION_ID + "'";

	/**
	 * The location names the investigation that was created. The parameters sent for the same key are held under one,
	 * with their values in the order sent.
	 */
	@Test
	void test01_createErrandInvestigation() {
		final var headers = setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + UUID_PATTERN))
			.sendRequest()
			.getResponseHeaders();

		setupCall()
			.withServicePath(headers.getLocation().getPath())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(null)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrandInvestigation() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_findErrandInvestigations() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrandInvestigation() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The location names the section that was created, by the id of the section. The parameters sent for the same trimmed
	 * key are held under one, with their values in the order sent.
	 */
	@Test
	void test05_createInvestigationSection() {
		final var headers = setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(INVESTIGATION_PATH + "/sections/" + UUID_PATTERN))
			.sendRequest()
			.getResponseHeaders();

		setupCall()
			.withServicePath(headers.getLocation().getPath())
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(null)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_findInvestigationSections() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_readInvestigationSection() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The section is part of the investigation, so a change to it moves the version of the investigation.
	 */
	@Test
	void test08_updateInvestigationSection() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(GET_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("1"))
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Removing a section leaves the other section with its parameters, and the JSON parameters of the investigation.
	 */
	@Test
	void test09_deleteInvestigationSection() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/investigationForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The section key is unique per investigation, and a section reusing one is answered with 409.
	 */
	@Test
	void test10_reusingASectionKeyIsAConflict() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The investigation is removed, and leaves the attachments linked to it on the errand and the decision resting on it
	 * without the reference.
	 */
	@Test
	void test11_deleteErrandInvestigation() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(DECISION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test12_readingUnknownSectionGives404() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections/f3000000-0000-0000-0000-0000000000ff")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test13_uploadInvestigationAttachment() throws Exception {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/attachments")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("attachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UUID_PATTERN))
			.sendRequest();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test14_linkInvestigationAttachment() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(ERRAND_PATH + "/attachments/" + UNLINKED_ATTACHMENT_ID))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(GET_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The link goes, and the attachment stays on the errand.
	 */
	@Test
	void test16_unlinkInvestigationAttachment() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments/" + LINKED_ATTACHMENT_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test17_readInvestigationJsonParameters() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test18_readInvestigationJsonParameter() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/investigationForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test19_createInvestigationJsonParameter() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(INVESTIGATION_PATH + "/json-parameters/dispatchLog"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The JSON parameter goes, and the investigation stays.
	 */
	@Test
	void test20_deleteInvestigationJsonParameter() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/investigationForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/json-parameters/investigationForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test21_readSectionJsonParameters() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test22_readSectionJsonParameter() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/sectionForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test23_createSectionJsonParameter() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(SECTION_PATH + "/json-parameters/dispatchLog"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The JSON parameter goes, and the section stays.
	 */
	@Test
	void test24_deleteSectionJsonParameter() {
		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/sectionForm")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(SECTION_PATH + "/json-parameters/sectionForm")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The recommendation proposes a decision, and is held to the decision outcomes the namespace has registered.
	 */
	@Test
	void test25_aRecommendationTheNamespaceHasNotRegisteredIsRejected() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(GET_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A change to the parameters alone moves the version of the investigation once, and the ETag it answers with is the
	 * version it was stored with. The caller already stands as the last to modify it, so nothing but the parameters
	 * changes.
	 */
	@Test
	@Sql(statements = MODIFIED_BY_THE_CALLER)
	void test26_updatingOnlyTheParametersMovesTheVersion() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withHeader(IF_MATCH, "\"0\"")
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("1"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Parameters that come out the same as the stored ones, in another order and with the values of a key split, leave the
	 * investigation as it stands.
	 */
	@Test
	@Sql(statements = MODIFIED_BY_THE_CALLER)
	void test27_sameParametersLeaveTheInvestigationAsItStands() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test28_anEmptyListRemovesTheParameters() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test29_aParameterWithoutKeyIsRejected() {
		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(GET_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Keys the database orders differently from the service, one with a letter outside a-z and two that differ only in
	 * case, are sent again in another order and with a padded key. The investigation is left as it stands, and answers with
	 * its parameters in the order of their keys.
	 */
	@Test
	@Sql(statements = {
		MODIFIED_BY_THE_CALLER,
		"delete from investigation_parameter where investigation_id = '" + INVESTIGATION_ID + "'",
		"""
			insert into investigation_parameter(id, investigation_id, parameters_key)
			values ('fa000000-0000-0000-0000-000000000011', 'f2000000-0000-0000-0000-000000000001', 'zon'),
			       ('fa000000-0000-0000-0000-000000000012', 'f2000000-0000-0000-0000-000000000001', 'ärende'),
			       ('fa000000-0000-0000-0000-000000000013', 'f2000000-0000-0000-0000-000000000001', 'Zon');""",
		"""
			insert into investigation_parameter_values(investigation_parameter_id, value_order, value)
			values ('fa000000-0000-0000-0000-000000000011', 0, 'a'),
			       ('fa000000-0000-0000-0000-000000000012', 0, 'b'),
			       ('fa000000-0000-0000-0000-000000000013', 0, 'c');"""
	})
	void test30_theSameParametersInAnotherOrderLeaveTheInvestigationAsItStands() {
		setupCall()
			.withHeader(SENT_BY_HEADER, AD_ACCOUNT)
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * An empty list removes the parameters of the section, and the other section keeps its own.
	 */
	@Test
	void test31_anEmptyListRemovesTheSectionParameters() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(INVESTIGATION_PATH + "/sections")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(GET_RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Parameters sent out of the order of their keys, with a padded key and with the values of a key split, are held
	 * trimmed and merged, and answered in the order of their keys.
	 */
	@Test
	void test32_sectionParametersAreTrimmedMergedAndOrderedOnUpdate() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(null)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(null)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * An update of a section that leaves out the parameters keeps the ones it has.
	 */
	@Test
	void test33_anOmittedListLeavesTheSectionParametersAsTheyAre() {
		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(SECTION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
