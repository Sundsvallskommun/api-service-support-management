package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;


/**
 * Errand time measurements IT tests.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandTimeMeasurementsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandTimeMeasurementsIT extends AbstractAppTest {

	private final static String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";

	private static final String BASE_PATH = "/NAMESPACE.1/2281/errands";

	private static final String TIME_MEASUREMENT_PATH = BASE_PATH + "/" + ERRAND_ID + "/timeMeasure";

	private static final String REQUEST_FILE = "request.json";

	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private ErrandsRepository errandsRepository;


	@Test
	void test01_getTimeMeasurements() {
		setupCall()
			.withServicePath(TIME_MEASUREMENT_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_createErrandAndGetTimeMeasurements() {

		assertThat(errandsRepository.findAll()).hasSize(6);

		// Create errand
		setupCall()
			.withHeader("sentbyuser", "joe01doe")
			.withServicePath(BASE_PATH.replace("NAMESPACE.1", "CONTACTCENTER"))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("/CONTACTCENTER/2281/errands/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequestAndVerifyResponse();

		final var result = errandsRepository.findAll(Example.of(ErrandEntity.create().withTitle("test04_postErrand"))).stream()
			.findAny()
			.orElseThrow();

		assertThat(result).isNotNull();
		assertThat(result.getTimeMeasures()).hasSize(1);
		assertThat(result.getTimeMeasures().getFirst().getStartTime()).isCloseTo(OffsetDateTime.now(), within(10, ChronoUnit.SECONDS));
		assertThat(result.getTimeMeasures().getFirst().getStatus()).isEqualTo("STATUS-2");
		assertThat(result.getTimeMeasures().getFirst().getStopTime()).isNull();

	}

	@Test
	void test03_updateErrandAndGetTimeMeasurements() {

		setupCall()
			.withServicePath(BASE_PATH + "/" + ERRAND_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponseHeader(CONTENT_TYPE, List.of(APPLICATION_JSON_VALUE))
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		errandsRepository.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, "NAMESPACE.1", "2281")
			.ifPresentOrElse(errand -> {
				assertThat(errand.getTimeMeasures()).hasSize(3);
				assertThat(errand.getTimeMeasures().getLast().getStartTime()).isCloseTo(OffsetDateTime.now(), within(10, ChronoUnit.SECONDS));
				assertThat(errand.getTimeMeasures().getLast().getStatus()).isEqualTo("STATUS-3");
				assertThat(errand.getTimeMeasures().getLast().getStopTime()).isNull();
			}, () -> {
				fail("Errand not found");
			});

	}

}
