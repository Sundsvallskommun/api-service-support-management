package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

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
 * Errand notes IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandsIT extends AbstractAppTest {

	private static final String PATH = "/NAMESPACE.1/2281/errands"; // 2281 is the municipalityId of Sundsvalls kommun

	private static final String REQUEST_FILE = "request.json";

	private static final String RESPONSE_FILE = "response.json";

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
			.withHeader("sentbyuser", "joe01doe")
			.withServicePath(PATH.replace("NAMESPACE.1", "CONTACTCENTER"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/CONTACTCENTER/2281/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
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

		assertThat(revisionRepository.findAllByEntityIdOrderByVersion(id)).hasSize(1)
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

		assertThat(revisionRepository.findAllByEntityIdOrderByVersion(id)).hasSize(2)
			.extracting(RevisionEntity::getVersion)
			.containsExactlyInAnyOrder(0, 1);
	}

	@Test
	void test06_deleteErrand() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";

		assertThat(revisionRepository.findAllByEntityIdOrderByVersion(id)).hasSize(1);
		assertThat(errandsRepository.existsById(id)).isTrue();

		setupCall()
			.withHeader("sentbyuser", "smo02key")
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.existsById(id)).isFalse();
		assertThat(revisionRepository.findAllByEntityIdOrderByVersion(id)).hasSize(1);
	}

}
