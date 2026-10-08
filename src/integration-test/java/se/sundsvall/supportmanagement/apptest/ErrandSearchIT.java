package se.sundsvall.supportmanagement.apptest;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Searches the OpenSearch index. The index is rebuilt from the database before every test, since the test data is
 * loaded by SQL, which Hibernate Search never sees.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandSearchIT/", classes = Application.class, sharedContext = true)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-search.sql"
})
class ErrandSearchIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-3/errands/search";
	private static final String ACCESS_CONTROLLED_PATH = "/2506/NAMESPACE-2506/errands/search";
	private static final String RESOURCE_CONTROLLED_PATH = "/2506/NAMESPACE-2507/errands/search";
	private static final String MIXED_PATH = "/2506/NAMESPACE-2508/errands/search";
	private static final String STATUS_ONLY_PATH = "/2506/NAMESPACE-2509/errands/search";

	private static final String LEAK = "NS3-25010001";
	private static final String INVOICE = "NS3-25020001";
	private static final String LIGHTING = "NS3-25030001";

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void reindex() throws InterruptedException {
		Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer().startAndWait();
	}

	@Test
	void test01_freeText() {
		// Stemmed: "vattenläckan" in the communication and "Vattenläcka" in the title share a stem
		assertThat(search(PATH, "vattenläcka")).containsExactly(LEAK);

		// The municipality and the namespace the errands are filtered by are no words of them: a search for one of them
		// looks where every other word looks, and finds the errands carrying it in something searchable, which is none
		assertThat(search(PATH, "2281")).isEmpty();
		assertThat(search(PATH, "NAMESPACE-3")).isEmpty();
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
	 * Labels reaching every label of the namespace at full read reach every errand of it.
	 */
	@Test
	void test10_accessThroughAllLabelsAtFullRead() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "all01red")).containsExactlyInAnyOrder("AP-23020001", "AP-23020002", "AP-23020003", "AP-26050001");
	}

	/**
	 * A label reaching one errand of three. The other two carry labels the user does not hold.
	 */
	/**
	 * A label reaching one errand of three, and beside them an errand carrying no access labels at all, which carries
	 * nothing that is not allowed and is therefore reached as well - the same answer the database gives.
	 */
	@Test
	void test11_accessThroughOneLabel() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "one01lbl")).containsExactlyInAnyOrder("AP-23020002", "AP-26050001");
	}

	/**
	 * No labels at all, but the reporter of one of the errands, which the namespace lets reporters read.
	 */
	@Test
	void test12_accessThroughReporting() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "rob01rep")).containsExactly("AP-23020003");
	}

	/**
	 * A user the access mapper grants no label reaches nothing, the errand carrying no access labels included: the
	 * specification behind the listing reaches no errand at all for them, and the search says the same.
	 */
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

		// The whole index belongs to no namespace, so it is rebuilt from outside of one - and it empties the search of
		// every namespace, so it asks that the caller may administer each one that enforces access control. This test data
		// holds several that do, and this caller administers none of them
		setupCall()
			.withServicePath("/search/reindex")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(FORBIDDEN)
			.sendRequest();
	}

	/**
	 * Labels reaching errands at limited read only search them by what a limited read exposes: the errands are found,
	 * and a query naming a field beyond those fields is refused rather than answered from them.
	 */
	@Test
	void test17_limitedReadIsSearchedByWhatALimitedReadExposes() {
		// The errand carrying no access labels is reached here too, since it carries nothing a limited read is kept from
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "lim01red")).containsExactlyInAnyOrder("AP-23020001", "AP-23020002", "AP-23020003", "AP-26050001");
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "title:e-service", "lim01red")).containsExactlyInAnyOrder("AP-23020001", "AP-23020002", "AP-23020003");

		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH, "description:x"))
			.withHeader(SENT_BY_HEADER, "lim01red; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-field.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A user whose resource grants reach the errand but not its communications. Errands are found, the words of a
	 * communication are not, and a query naming a communication field is refused rather than answered by hit or miss.
	 */
	@Test
	void test18_closedResourceIsNotSearchable() {
		assertThat(searchAs(RESOURCE_CONTROLLED_PATH, "", "fro01lin")).containsExactly("FL-23020001");
		assertThat(searchAs(RESOURCE_CONTROLLED_PATH, "hemligt", "fro01lin")).isEmpty();

		setupCall()
			.withServicePath(withQuery(RESOURCE_CONTROLLED_PATH, "communications.subject:hemligt"))
			.withHeader(SENT_BY_HEADER, "fro01lin; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-resource.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(withQuery(RESOURCE_CONTROLLED_PATH, "\\*.subject:hemligt"))
			.withHeader(SENT_BY_HEADER, "fro01lin; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-wildcard.json")
			.sendRequestAndVerifyResponse();

		// The parser binds a term to a field whatever whitespace stands before the colon, so a space in front of it
		// reaches the same field and is refused the same way. Held against the real parser here, since a query read
		// differently from how OpenSearch reads it is a field searched by hit and miss
		setupCall()
			.withServicePath(withQuery(RESOURCE_CONTROLLED_PATH, "communications.subject : hemligt"))
			.withHeader(SENT_BY_HEADER, "fro01lin; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-resource.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(withQuery(RESOURCE_CONTROLLED_PATH, "* : hemligt"))
			.withHeader(SENT_BY_HEADER, "fro01lin; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-wildcard.json")
			.sendRequestAndVerifyResponse();

		// The shapes that name the very same field to the parser while reading as something else, or as nothing at all:
		// a name opened by a minus, a name spelled with a unicode escape, a fielded term hidden behind an escaped quote,
		// and the object asked about by name. Each of them reached the communications before the query was read the way
		// the index reads it
		for (final var query : List.of(
			"-communications.subject:hemligt",
			"\\u0063ommunications.subject:hemligt",
			"x\\\" communications.subject:hemligt \"y\"",
			"_exists_:communications",
			"_exists_:(communications.subject)",
			"_exists_:\"communications.subject\"")) {

			setupCall()
				.withServicePath(withQuery(RESOURCE_CONTROLLED_PATH, query))
				.withHeader(SENT_BY_HEADER, "fro01lin; type=adAccount")
				.withHttpMethod(GET)
				.withExpectedResponseStatus(FORBIDDEN)
				.withExpectedResponse("response-closed-resource.json")
				.sendRequestAndVerifyResponse();
		}
	}

	/**
	 * The same namespace, for a user whose grants reach the communications as well.
	 */
	@Test
	void test19_openResourceIsSearchable() {
		assertThat(searchAs(RESOURCE_CONTROLLED_PATH, "hemligt", "com01red")).containsExactly("FL-23020001");
		assertThat(searchAs(RESOURCE_CONTROLLED_PATH, "communications.subject:hemligt", "com01red")).containsExactly("FL-23020001");
	}

	/**
	 * A case officer whose role sees the title, one parameter key and one JSON parameter key. The labels reach every
	 * errand, but the search keeps to what the role sees: the granted JSON key is searchable, the hidden one and the
	 * description are refused, free text does not find what the hidden key holds, and a sort on a field the role does
	 * not see is refused too.
	 */
	@Test
	void test20_roleKeepsFieldsAndKeysFromTheSearch() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "", "smo02key")).containsExactlyInAnyOrder("AP-23020001", "AP-23020002", "AP-23020003", "AP-26050001");
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "jsonParameters.granted-json.visible:true", "smo02key")).containsExactly("AP-23020003");
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "survive", "smo02key")).isEmpty();

		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH, "description:x"))
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-field.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH, "jsonParameters.hidden-json.secret:survive"))
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-key.json")
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH + "?sort=created,desc", ""))
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-sort.json")
			.sendRequestAndVerifyResponse();

		// A space before the colon names the same field to the parser, and so must name it here
		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH, "description : x"))
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-field.json")
			.sendRequestAndVerifyResponse();

		// The value of _exists_ is a field name, with or without the space
		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH, "_exists_ : description"))
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-field.json")
			.sendRequestAndVerifyResponse();

		// Access control's own bookkeeping is bound to no field of the errand, so nothing opens it
		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH, "accessLabels.metadataLabelId:aa000000-0000-0000-0000-000000000001"))
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-unknown-field.json")
			.sendRequestAndVerifyResponse();

		// The property ordered by belongs to the classification, which this role does not see, so the order says something
		// it may not be told - by paging through it, and by halving the interval, the values themselves
		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH + "?sort=category,asc", ""))
			.withHeader(SENT_BY_HEADER, "smo02key; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-sort-property.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The reporter, whom the access mapper grants nothing, is held to the reporter fields of the namespace on their own
	 * errand: found by title, refused on the description.
	 */
	@Test
	void test21_reporterIsHeldToTheReporterFields() {
		assertThat(searchAs(ACCESS_CONTROLLED_PATH, "title:e-service", "rob01rep")).containsExactly("AP-23020003");

		setupCall()
			.withServicePath(withQuery(ACCESS_CONTROLLED_PATH, "description:x"))
			.withHeader(SENT_BY_HEADER, "rob01rep; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.withExpectedResponse("response-closed-field.json")
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The mixed case: the labels reach one errand at read and another at limited read. A word is looked for in what each
	 * of them allows, a query on the body answers from the errand held at read alone, and one naming a resource the
	 * limited read does not extend to is refused, since no route of this user can answer it.
	 */
	@Test
	void test22_readAndLimitedReadSideBySide() {
		// Both are found by their title, which a limited read exposes
		assertThat(searchAs(MIXED_PATH, "title:vattenläcka", "mix01ed")).containsExactlyInAnyOrder("LR-26010001", "LR-26020001");
		assertThat(searchAs(MIXED_PATH, "", "mix01ed")).containsExactlyInAnyOrder("LR-26010001", "LR-26020001");

		// The body is readable on the errand held at read alone, so only it answers - and without a refusal
		assertThat(searchAs(MIXED_PATH, "description:källaren", "mix01ed")).containsExactly("LR-26010001");
		assertThat(searchAs(MIXED_PATH, "description:vinden", "mix01ed")).isEmpty();

		// A word without a field reaches the body of the one and the title of the other
		assertThat(searchAs(MIXED_PATH, "källaren", "mix01ed")).containsExactly("LR-26010001");
		assertThat(searchAs(MIXED_PATH, "vinden", "mix01ed")).isEmpty();

		// The communication hangs off the errand held at limited read, which a limited read does not extend to the
		// communications: the query is not refused, since the user may search the communications of the errands they hold
		// at read, but it reaches nothing
		assertThat(searchAs(MIXED_PATH, "communications.subject:uppföljning", "mix01ed")).isEmpty();
	}

	/**
	 * A role seeing one field that a search without a field never looks in: the status is searched by name, not by word.
	 * A word has nowhere to look and finds nothing, a query naming the status is answered, and a query naming a field
	 * the role does not see is refused - none of the three an error.
	 */
	@Test
	void test23_aRoleSeeingOneFieldThatFreeTextDoesNotLookIn() {
		// The errands come back with the status alone, the number among the fields the role does not see
		assertThat(statuses(pageAs(STATUS_ONLY_PATH, "status:new", "sta01usr"))).containsExactly("NEW");
		assertThat(statuses(pageAs(STATUS_ONLY_PATH, "status:ongoing", "sta01usr"))).containsExactly("ONGOING");
		assertThat(statuses(pageAs(STATUS_ONLY_PATH, "", "sta01usr"))).containsExactlyInAnyOrder("NEW", "ONGOING");

		// A word has nothing to look in here, so it finds nothing - and does not fail
		assertThat(statuses(pageAs(STATUS_ONLY_PATH, "vattenläcka", "sta01usr"))).isEmpty();

		// Nothing is a word here, the municipality id of the errands included: what stands in for the fields of this route
		// is a field no errand carries, not one they all do
		assertThat(statuses(pageAs(STATUS_ONLY_PATH, "2506", "sta01usr"))).isEmpty();

		// And a word beside a fielded term is still only a word, so the term answers on its own
		assertThat(statuses(pageAs(STATUS_ONLY_PATH, "status:new OR 2506", "sta01usr"))).containsExactly("NEW");

		setupCall()
			.withServicePath(withQuery(STATUS_ONLY_PATH, "title:vattenläcka"))
			.withHeader(SENT_BY_HEADER, "sta01usr; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(FORBIDDEN)
			.sendRequest();
	}

	/**
	 * The database compares the namespace under a case insensitive collation, so the index does too: the listing and the
	 * search of the same errands may not disagree on which namespace was asked for.
	 */
	@Test
	void test24_theNamespaceIsMatchedWhateverItsCasing() {
		assertThat(search("/2281/namespace-3/errands/search", "vattenläcka")).containsExactly(LEAK);
		assertThat(search("/2281/NaMeSpAcE-3/errands/search", "vattenläcka")).containsExactly(LEAK);
	}

	/**
	 * A date without a zone is a day in Sweden: half past midnight on the first of June in Swedish summer time is the
	 * evening before in UTC, and is found on the first. The point in time is written through JDBC rather than as a SQL
	 * literal, so that it is the same instant whatever zone the test runs in.
	 */
	@Test
	void test25_aDateIsADayInSweden() throws InterruptedException {
		jdbcTemplate.update("UPDATE errand SET created = ? WHERE errand_number = ?", Timestamp.from(Instant.parse("2025-05-31T22:30:00Z")), INVOICE);
		reindex();

		assertThat(search(PATH, "created:[2025-06-01 TO 2025-06-02}")).containsExactly(INVOICE);
		assertThat(search(PATH, "created:[2025-05-31 TO 2025-06-01}")).isEmpty();
	}

	/**
	 * A draft is left out of a search, as it is left out of the listing, unless the query names the life cycle.
	 */
	@Test
	void test26_aDraftIsFoundOnlyByAQueryNamingTheLifecycle() throws InterruptedException {
		jdbcTemplate.update("UPDATE errand SET lifecycle = 'DRAFT' WHERE errand_number = ?", INVOICE);
		reindex();

		assertThat(search(PATH, "")).containsExactly(LIGHTING, LEAK);
		assertThat(search(PATH, "lindqvist")).isEmpty();
		assertThat(search(PATH, "lifecycle:draft")).containsExactly(INVOICE);
		assertThat(search(PATH, "lindqvist AND lifecycle:DRAFT")).containsExactly(INVOICE);
		assertThat(search(PATH, "lifecycle:active")).containsExactlyInAnyOrder(LEAK, LIGHTING);
		assertThat(search(PATH, "_exists_:lifecycle")).containsExactlyInAnyOrder(LEAK, INVOICE, LIGHTING);
	}

	private List<String> search(final String path, final String query) {
		return errandNumbers(page(path, query));
	}

	private List<String> searchAs(final String path, final String query, final String adAccount) {
		return errandNumbers(pageAs(path, query, adAccount));
	}

	private JsonNode pageAs(final String path, final String query, final String adAccount) {
		return (setupCall()
			.withServicePath(withQuery(path, query))
			.withHeader(SENT_BY_HEADER, adAccount + "; type=adAccount")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.getResponseBody(new TypeReference<JsonNode>() {}));
	}

	private static List<String> statuses(final JsonNode page) {
		return page.path("content").valueStream()
			.map(errand -> errand.path("status").asString())
			.toList();
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
