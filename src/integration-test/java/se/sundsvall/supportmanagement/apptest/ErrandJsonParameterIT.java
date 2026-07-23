package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.ETAG;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.JsonParameterRepository;

@WireMockAppTestSuite(files = "classpath:/ErrandJsonParameterIT/", classes = Application.class)
class ErrandJsonParameterIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String SEEDED_PARAMETER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	private static final String KEY = "formData";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/json-parameters";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private JsonParameterRepository jsonParameterRepository;

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql"
	})
	void test01_getAllJsonParameters_empty() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql"
	})
	void test02_putJsonParameter_create() {
		assertThat(jsonParameterRepository.findByErrandEntityIdAndKey(ERRAND_ID, KEY)).isEmpty();

		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponseHeader(LOCATION, List.of(PATH + "/" + KEY))
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(jsonParameterRepository.findByErrandEntityIdAndKey(ERRAND_ID, KEY)).isPresent();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql",
		"/db/scripts/testdata-it-json-parameter.sql"
	})
	void test03_getAllJsonParameters_filled() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql",
		"/db/scripts/testdata-it-json-parameter.sql"
	})
	void test04_getJsonParameter_found() {
		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponseHeader(ETAG, List.of("0"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql"
	})
	void test05_getJsonParameter_notFound() {
		setupCall()
			.withServicePath(PATH + "/missing-key")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql",
		"/db/scripts/testdata-it-json-parameter.sql"
	})
	void test06_putJsonParameter_updateInPlace() {
		final var idBefore = jsonParameterRepository.findById(SEEDED_PARAMETER_ID).orElseThrow().getId();

		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponseHeader(ETAG, List.of("1"))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		// In-place-kravet: rad-id ska vara oförändrat efter uppdatering
		final var idAfter = jsonParameterRepository.findByErrandEntityIdAndKey(ERRAND_ID, KEY).orElseThrow().getId();
		assertThat(idAfter).isEqualTo(idBefore);
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql"
	})
	void test07_putJsonParameter_schemaValidationFailure() {
		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql"
	})
	void test08_putJsonParameter_keyMismatch() {
		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql",
		"/db/scripts/testdata-it-json-parameter.sql"
	})
	void test09_deleteJsonParameter() {
		assertThat(jsonParameterRepository.findByErrandEntityIdAndKey(ERRAND_ID, KEY)).isPresent();

		setupCall()
			.withServicePath(PATH + "/" + KEY)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(jsonParameterRepository.findByErrandEntityIdAndKey(ERRAND_ID, KEY)).isEmpty();
	}

	@Test
	@Sql({
		"/db/scripts/truncate.sql",
		"/db/scripts/testdata-it.sql"
	})
	void test10_deleteJsonParameter_notFound() {
		setupCall()
			.withServicePath(PATH + "/missing-key")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_PROBLEM_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
