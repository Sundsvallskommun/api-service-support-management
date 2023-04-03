package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
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
import se.sundsvall.supportmanagement.integration.db.RoleRepository;

/**
 * Role Metadata IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/MetadataRoleIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MetadataRoleIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String NAMESPACE = "NAMESPACE.1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String PATH = "/" + NAMESPACE + "/" + MUNICIPALITY_2281 + "/metadata/roles";

	@Autowired
	private RoleRepository roleRepository;

	@Test
	void test01_createRole() throws Exception {
		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_BRAND_NEW_ROLE")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^http://(.*)/metadata/roles/A_BRAND_NEW_ROLE$"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_BRAND_NEW_ROLE")).isTrue();
	}

	@Test
	void test02_getRole() throws Exception {
		setupCall()
			.withServicePath(PATH + "/ROLE-2")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getRoles() throws Exception {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getRolesWhenEmpty() throws Exception {
		final var path = "/" + NAMESPACE + "/" + MUNICIPALITY_2309 + "/metadata/roles";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteRole() throws Exception {
		final var roleName = "ROLE-2";

		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, roleName)).isTrue();
		assertThat(roleRepository.count()).isEqualTo(6);

		setupCall()
			.withServicePath(PATH + "/" + roleName)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, roleName)).isFalse();
		assertThat(roleRepository.count()).isEqualTo(5);
	}
}
