package se.sundsvall.supportmanagement.apptest;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Searches the OpenSearch index. The index is rebuilt from the database before every test, since the test data is
 * loaded by SQL, which Hibernate Search never sees.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandSearchIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-search.sql"
})
class ErrandSearchIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-3/errands/search";
	private static final String ACCESS_CONTROLLED_PATH = "/2506/NAMESPACE-2506/errands/search";

	private static final String LEAK = "NS3-25010001";
	private static final String INVOICE = "NS3-25020001";
	private static final String LIGHTING = "NS3-25030001";

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void reindex() throws InterruptedException {
		Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer().startAndWait();
	}

	@Test
	void test01_freeText() {
		// Stemmed: "vattenläckan" in the communication and "Vattenläcka" in the title share a stem
		assertThat(search(PATH, "vattenläcka")).containsExactly(LEAK);
	}

	@Test
	void test02_blankQueryMatchesEverythingNewestFirst() {
		assertThat(search(PATH, "")).containsExactly(LIGHTING, INVOICE, LEAK);
	}

	@Test
	void test03_fieldedQuery() {
		assertThat(search(PATH, "status:new AND category:vatten")).containsExactly(LEAK);
		assertThat(search(PATH, "status:new")).containsExactlyInAnyOrder(LEAK, LIGHTING);
		assertThat(search(PATH, "created:[2025-02-01 TO 2025-12-31]")).containsExactlyInAnyOrder(INVOICE, LIGHTING);
	}

	@Test
	void test04_stakeholderAndContactChannel() {
		assertThat(search(PATH, "stakeholders.lastName:bergström")).containsExactly(LEAK);
		assertThat(search(PATH, "lindqvist")).containsExactly(INVOICE);
		assertThat(search(PATH, "stakeholders.contactChannels.value_raw:\"anna.bergstrom@example.com\"")).containsExactly(LEAK);
		assertThat(search(PATH, "stakeholders.city:timrå")).containsExactly(LIGHTING);
	}

	@Test
	void test05_parametersAndJsonParameters() {
		assertThat(search(PATH, "parameters.values:storgatan")).containsExactly(LEAK);
		assertThat(search(PATH, "INV-778899")).containsExactly(INVOICE);
		// A JSON parameter by path, exactly and by word, nested and in an array
		assertThat(search(PATH, "jsonParameters.vehicle.regNo.raw:abc123")).containsExactly(LEAK);
		assertThat(search(PATH, "jsonParameters.vehicle.owner.name:bergström")).containsExactly(LEAK);
		assertThat(search(PATH, "jsonParameters.vehicle.tags:diesel")).containsExactly(LEAK);
		assertThat(search(PATH, "jsonParameters.vehicle.regNo:xyz789")).containsExactly(LIGHTING);
		// And by nothing but its value
		assertThat(search(PATH, "tjänstebil")).containsExactly(LEAK);
	}

	@Test
	void test06_communicationsAndMeasures() {
		assertThat(search(PATH, "communications.subject:uppföljning")).containsExactly(LEAK);
		assertThat(search(PATH, "\"skicka någon\"")).containsExactly(LEAK);
		assertThat(search(PATH, "measures.title:armatur")).containsExactly(LIGHTING);
		assertThat(search(PATH, "measures.jsonParameters.order.supplier:ljusbolaget")).containsExactly(LIGHTING);
		assertThat(search(PATH, "ORD-4711")).containsExactly(LIGHTING);
	}

	@Test
	void test07_sorting() {
		assertThat(search(PATH + "?sort=created,asc", "")).containsExactly(LEAK, INVOICE, LIGHTING);
		assertThat(search(PATH + "?sort=priority,asc", "")).containsExactly(LEAK, INVOICE, LIGHTING);
		assertThat(search(PATH + "?sort=title,asc", "")).containsExactly(INVOICE, LIGHTING, LEAK);
	}

	@Test
	void test08_paging() {
		final var page = page(PATH + "?size=2&page=1&sort=created,asc", "");

		assertThat(page.get("totalElements").asInt()).isEqualTo(3);
		assertThat(errandNumbers(page)).containsExactly(LIGHTING);
	}

	@Test
	void test09_badQueriesAndSorts() {
		setupCall()
			.withServicePath(withQuery(PATH, "title:(unbalanced"))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-bad-query.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH + "?sort=description,asc")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-bad-sort.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(PATH + "?page=500&size=100")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response-beyond-window.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Labels reaching every label of the namespace reach every errand of it.
	 */
	@Test
	void test10_accessThroughAllLabels() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "lim01red")).containsExactlyInAnyOrder("AP-23020001", "AP-23020002", "AP-23020003");
	}

	/**
	 * A label reaching one errand of three. The other two carry labels the user does not hold.
	 */
	@Test
	void test11_accessThroughOneLabel() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "one01lbl")).containsExactly("AP-23020002");
	}

	/**
	 * No labels at all, but the reporter of one of the errands, which the namespace lets reporters read.
	 */
	@Test
	void test12_accessThroughReporting() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "rob01rep")).containsExactly("AP-23020003");
	}

	@Test
	void test13_noAccess() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "nob01ody")).isEmpty();
	}

	/**
	 * The other namespace holds an errand with the very same words, which a search in this one never sees.
	 */
	@Test
	void test14_tenantIsolation() {
		assertThat(search(PATH, "storgatan")).containsExactly(LEAK);
		assertThat(search("/2281/NAMESPACE-1/errands/search", "storgatan")).containsExactly("NS1-25010099");
	}

	/**
	 * A star, escaped, stands for any part of a field name and spans dots, so a path can be left out or a field looked for
	 * under every JSON parameter at once.
	 */
	@Test
	void test16_wildcardsInFieldNames() {
		assertThat(search(PATH, "jsonParameters.\\*.regNo:abc123")).containsExactly(LEAK);
		assertThat(search(PATH, "jsonParameters.vehicle.\\*.name:bergström")).containsExactly(LEAK);
		assertThat(search(PATH, "\\*.supplier:ljusbolaget")).containsExactly(LIGHTING);
		assertThat(search(PATH, "jsonParameters.\\*.regNo.raw:xyz789")).containsExactly(LIGHTING);
	}

	@Test
	void test15_reindex() {
		setupCall()
			.withServicePath(PATH + "/reindex")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	private List<String> search(final String path, final String query) {
		return errandNumbers(page(path, query));
	}

	private List<String> searchAs(final String path, final String query, final String adAccount) {
		return errandNumbers(setupCall()
			.withServicePath(withQuery(path, query))
			.withHeader(SENT_BY_HEADER, adAccount + "; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.getResponseBody(new TypeReference<JsonNode>() {}));
	}

	private JsonNode page(final String path, final String query) {
		return setupCall()
			.withServicePath(withQuery(path, query))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.getResponseBody(new TypeReference<JsonNode>() {});
	}

	/**
	 * The query goes in raw: the test client encodes the path once, and would encode an already encoded query twice.
	 */
	private static String withQuery(final String path, final String query) {
		return path + (path.contains("?") ? "&" : "?") + "query=" + query;
	}

	private static List<String> errandNumbers(final JsonNode page) {
		return page.path("content").valueStream()
			.map(errand -> errand.path("errandNumber").asString())
			.toList();
	}
}
