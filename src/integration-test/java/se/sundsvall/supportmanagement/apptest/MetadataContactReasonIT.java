package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ContactReasonRepository;

/**
 * Contact Reason Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataContactReasonIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataContactReasonIT extends AbstractAppTest {

	private static final String NAMESPACE = "CONTACTCENTER";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/contactreasons";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String CONTACT_REASON_ID = "123";

	@Autowired
	private ContactReasonRepository contactReasonRepository;

	@Test
	void test01_createContactReason() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^" + PATH + "/(\\d+)$"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getContactReason() {
		setupCall()
			.withServicePath(PATH + "/" + CONTACT_REASON_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getContactReasons() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateContactReason() {
		setupCall()
			.withServicePath(PATH + "/" + CONTACT_REASON_ID)
			.withHttpMethod(PATCH)
			.withExpectedResponseStatus(OK)
			.withRequest(REQUEST_FILE)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteContactReason() {
		assertThat(contactReasonRepository.findByReasonIgnoreCaseAndNamespaceAndMunicipalityId("reason3", NAMESPACE, MUNICIPALITY_2281)).isPresent();

		setupCall()
			.withServicePath(PATH + "/" + CONTACT_REASON_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(contactReasonRepository.findByIdAndNamespaceAndMunicipalityId(Long.valueOf(CONTACT_REASON_ID), NAMESPACE, MUNICIPALITY_2281)).isNotPresent();
	}
}
