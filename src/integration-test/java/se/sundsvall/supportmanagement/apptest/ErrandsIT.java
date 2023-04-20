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
import org.springframework.data.domain.Example;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.RevisionRepository;
import se.sundsvall.supportmanagement.integration.db.model.DbExternalTag;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
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
		assertThat(revisionRepository.findAll()).hasSize(1);

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^http://(.*)/errands/(.*)$"))
			.sendRequestAndVerifyResponse();

		final var entityId = errandsRepository.findAll(Example.of(ErrandEntity.create().withExternalTags(List.of(DbExternalTag.create().withKey("caseid").withValue("8849-2848"))))).stream()
			.findAny()
			.map(ErrandEntity::getId)
			.orElse(null);

		assertThat(revisionRepository.findAll()).hasSize(2);
		assertThat(revisionRepository.findByEntityId(entityId))
			.hasSize(1)
			.extracting(RevisionEntity::getVersion)
			.containsExactly(0);
	}

	@Test
	void test05_patchErrand() {
		assertThat(revisionRepository.findAll()).hasSize(1);
		assertThat(revisionRepository.findByEntityId("1be673c0-6ba3-4fb0-af4a-43acf23389f6")).hasSize(1)
			.extracting(RevisionEntity::getVersion)
			.containsExactly(0);

		setupCall()
			.withServicePath(PATH + "/1be673c0-6ba3-4fb0-af4a-43acf23389f6")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(revisionRepository.findAll()).hasSize(2);
		assertThat(revisionRepository.findByEntityId("1be673c0-6ba3-4fb0-af4a-43acf23389f6")).hasSize(2)
			.extracting(RevisionEntity::getVersion)
			.containsExactlyInAnyOrder(0, 1);
	}

	@Test
	void test06_deleteErrand() {
		final var id = "1be673c0-6ba3-4fb0-af4a-43acf23389f6";
		assertThat(revisionRepository.findAll()).hasSize(1);
		assertThat(errandsRepository.existsById(id)).isTrue();

		setupCall()
			.withServicePath(PATH + "/" + id)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(errandsRepository.existsById(id)).isFalse();
		assertThat(revisionRepository.findAll()).hasSize(1);
	}
}
