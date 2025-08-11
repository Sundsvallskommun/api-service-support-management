package se.sundsvall.supportmanagement.apptest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeSyncRepository;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@WireMockAppTestSuite(files = "classpath:/MessageExchangeSyncConfigIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class MessageExchangeSyncConfigIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";

	private static final String PATH = "/" + MUNICIPALITY_ID + "/message-exchange-sync-config";
	private static final UnaryOperator<String> PATH_WITH_ID = id -> PATH + "/" + id;

	@Autowired
	private MessageExchangeSyncRepository repository;

	@Test
	void test01_getAllConfig() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_createConfig() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH_WITH_ID.apply("3")))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_deleteConfig() {
		assertThat(repository.findByIdAndMunicipalityId(1L, MUNICIPALITY_ID)).isNotEmpty();

		setupCall()
			.withServicePath(PATH_WITH_ID.apply("1"))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByIdAndMunicipalityId(1L, MUNICIPALITY_ID)).isEmpty();
	}

	@Test
	void test04_updateConfig() {

		assertThat(repository.findByIdAndMunicipalityId(2L, MUNICIPALITY_ID)).hasValueSatisfying(value -> {
			assertThat(value.isActive()).isFalse();
			assertThat(value.getNamespace()).isEqualTo("external-namespace-2");
		});

		setupCall()
			.withServicePath(PATH_WITH_ID.apply("2"))
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		assertThat(repository.findByIdAndMunicipalityId(2L, MUNICIPALITY_ID)).hasValueSatisfying(value -> {
			assertThat(value.isActive()).isTrue();
			assertThat(value.getNamespace()).isEqualTo("new-namespace");
		});
	}
}
