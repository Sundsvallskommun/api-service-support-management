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
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_2281 = "2281";
	private static final String MUNICIPALITY_2309 = "2309";
	private static final String PATH = "/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/roles";

	@Autowired
	private RoleRepository roleRepository;

	@Test
	void test01_createRole() {
		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_BRAND_NEW_ROLE")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/roles/A_BRAND_NEW_ROLE"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "A_BRAND_NEW_ROLE")).isTrue();
	}

	@Test
	void test02_getRole() {
		final var roleId = "cc000000-0000-0000-0000-000000000101";
		setupCall()
			.withServicePath(PATH + "/" + roleId)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getRoles() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getRolesWhenEmpty() {
		final var path = "/" + MUNICIPALITY_2309 + "/" + NAMESPACE + "/metadata/roles";

		setupCall()
			.withServicePath(path)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteRole() {
		final var roleId = "cc000000-0000-0000-0000-000000000101";

		assertThat(roleRepository.existsByIdAndNamespaceAndMunicipalityId(roleId, NAMESPACE, MUNICIPALITY_2281)).isTrue();
		assertThat(roleRepository.count()).isEqualTo(6);

		setupCall()
			.withServicePath(PATH + "/" + roleId)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(roleRepository.existsByIdAndNamespaceAndMunicipalityId(roleId, NAMESPACE, MUNICIPALITY_2281)).isFalse();
		assertThat(roleRepository.count()).isEqualTo(5);
	}

	@Test
	void test06_createRoleWithDisplayName() {
		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "ROLE_WITH_DISPLAY_NAME")).isFalse();

		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/" + MUNICIPALITY_2281 + "/" + NAMESPACE + "/metadata/roles/ROLE_WITH_DISPLAY_NAME"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(roleRepository.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_2281, "ROLE_WITH_DISPLAY_NAME")).isTrue();
		assertThat(roleRepository.findAllByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_2281))
			.filteredOn(role -> "ROLE_WITH_DISPLAY_NAME".equals(role.getName()))
			.singleElement()
			.satisfies(role -> assertThat(role.getDisplayName()).isEqualTo("Display name of role"));
	}

	@Test
	void test07_patchRole() {
		final var roleId = "cc000000-0000-0000-0000-000000000102";
		setupCall()
			.withServicePath(PATH + "/" + roleId)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
