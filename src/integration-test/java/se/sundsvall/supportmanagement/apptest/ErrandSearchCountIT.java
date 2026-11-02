package se.sundsvall.supportmanagement.apptest;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.config.SearchProperties;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.service.search.ErrandSearchService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;

/**
 * Counts the OpenSearch index, and divides the count over one column. The index is rebuilt from the database before
 * every test, since the test data is loaded by SQL, which Hibernate Search never sees.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandSearchCountIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-search.sql"
})
class ErrandSearchCountIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-3/errands/search";
	private static final String ACCESS_CONTROLLED_PATH = "/2506/NAMESPACE-2506/errands/search";
	private static final String MIXED_PATH = "/2506/NAMESPACE-2508/errands/search";
	private static final String STATUS_ONLY_PATH = "/2506/NAMESPACE-2509/errands/search";

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ErrandSearchService errandSearchService;

	@Autowired
	private SearchProperties searchProperties;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void reindex() throws InterruptedException {
		Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer().startAndWait();
	}

	/**
	 * The count answers the number the search answers with, without the errands: same query, same grant, same routes.
	 */
	@Test
	void test01_theCountIsTheSearchWithoutTheErrands() {
		assertThat(count(PATH, "")).isEqualTo(3);
		assertThat(count(PATH, "vattenläcka")).isEqualTo(1);
		assertThat(count(PATH, "status:new")).isEqualTo(2);
		assertThat(count(PATH, "status:new AND priority:high")).isEqualTo(1);
		assertThat(count(PATH, "ingentingalls")).isZero();

		// The same query, answered by the search, reports the same total
		assertThat(page(PATH, "status:new").path("totalElements").asInt()).isEqualTo(2);

		// And a namespace counts its own errands only, whatever the casing it is asked for by
		assertThat(count("/2281/namespace-3/errands/search", "vattenläcka")).isEqualTo(1);
	}

	/**
	 * The breakdown: the largest bucket first, and the values in the casing the metadata of the namespace gives them
	 * rather than the lowercased form the index holds.
	 */
	@Test
	void test02_theCountDividesOverOneColumn() {
		final var byStatus = group(PATH, "", "status");

		assertThat(byStatus.path("property").asString()).isEqualTo("status");
		assertThat(bucketsOf(byStatus)).containsExactly("NEW=2", "ONGOING=1");

		// The breakdown accounts for every errand the count counted, which is the whole point of refusing a partial one
		assertThat(accountedFor(byStatus)).isEqualTo(count(PATH, ""));
		assertThat(byStatus.path("withoutValue").asLong()).isZero();
		assertThat(byStatus.path("withheld").asLong()).isZero();

		// A column none of the errands carries gets no buckets at all, and is counted as carrying nothing
		final var byResolution = group(PATH, "", "resolution");
		assertThat(byResolution.path("buckets")).isEmpty();
		assertThat(byResolution.path("withoutValue").asLong()).isEqualTo(3);
		assertThat(accountedFor(byResolution)).isEqualTo(3);

		// The query narrows the breakdown as it narrows the count
		assertThat(bucketsOf(group(PATH, "status:new", "status"))).containsExactly("NEW=2");

		// A column with no catalogue behind it answers with what the index holds
		assertThat(bucketsOf(group(PATH, "", "assignedUserId"))).containsExactly("han01dle=2", "han02dle=1");
		assertThat(bucketsOf(group(PATH, "", "priority"))).hasSize(3);
	}

	/** A property nobody groups by is a bad request, and says what may be grouped by. */
	@Test
	void test03_aColumnNobodyGroupsByIsRefused() {
		setupCall()
			.withServicePath(PATH + "/count?groupBy=description")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();

		setupCall()
			.withServicePath(PATH + "/count?groupBy=labels")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();
	}

	/**
	 * A count is held to the grant as a search is, and the column it groups by to what the route may read: the role seeing
	 * the status alone counts by status, and asking it for the category divides nothing up rather than refusing - the count
	 * stays the count of the search, which is what a client filtering with one and counting with the other depends on.
	 */
	@Test
	void test04_aCountIsGroupedWithinWhatTheUserMayRead() {
		// The errands this role reaches, counted and divided by the one column it may read
		assertThat(countAs(STATUS_ONLY_PATH, "", "sta01usr")).isEqualTo(2);
		assertThat(bucketsOf(groupAs(STATUS_ONLY_PATH, "", "status", "sta01usr"))).containsExactlyInAnyOrder("NEW=1", "ONGOING=1");

		// A column the role may not read divides nothing up, and refuses nothing either: the count is what the search of the
		// same query answers with, and every errand of it lands in the bucket the breakdown cannot account for
		final var byCategory = groupAs(STATUS_ONLY_PATH, "", "category", "sta01usr");
		assertThat(byCategory.path("buckets")).isEmpty();
		// Withheld rather than said to carry nothing: that an errand holds no category is a fact about the category
		assertThat(byCategory.path("withheld").asLong()).isEqualTo(2);
		assertThat(byCategory.path("withoutValue").asLong()).isZero();
		assertThat(accountedFor(byCategory)).isEqualTo(countAs(STATUS_ONLY_PATH, "", "sta01usr"));

		// The labels of the access controlled namespace reach three errands and the unlabelled one
		assertThat(countAs(ACCESS_CONTROLLED_PATH, "", "lim01red")).isEqualTo(4);
	}

	/**
	 * The shape where one route of the grant may read the column and another may not: the labels of this namespace reach
	 * one errand at read and the other at limited read, and a limited read exposes the status but not the category. So the
	 * breakdown divides up the errand held at read and withholds the other, while the count counts both - which is what a
	 * client filtering with a search and counting with the same query depends on.
	 */
	@Test
	void test05_aColumnOneRouteMayReadAndAnotherMayNot() {
		// Both errands are counted, as the search of the same query answers with both
		assertThat(countAs(MIXED_PATH, "", "mix01ed")).isEqualTo(2);
		assertThat(searchAs(MIXED_PATH, "", "mix01ed")).hasSize(2);

		// The status, which both routes may read, divides both of them up
		final var byStatus = groupAs(MIXED_PATH, "", "status", "mix01ed");
		assertThat(bucketsOf(byStatus)).containsExactly("NEW=2");
		assertThat(byStatus.path("withheld").asLong()).isZero();
		assertThat(accountedFor(byStatus)).isEqualTo(2);

		// The category, which a limited read does not expose, divides up the errand held at read and withholds the other
		final var byCategory = groupAs(MIXED_PATH, "", "category", "mix01ed");
		assertThat(bucketsOf(byCategory)).containsExactly("VATTEN=1");
		assertThat(byCategory.path("withheld").asLong()).isEqualTo(1);
		assertThat(byCategory.path("withoutValue").asLong()).isZero();
		assertThat(accountedFor(byCategory)).isEqualTo(countAs(MIXED_PATH, "", "mix01ed"));
	}

	/**
	 * A breakdown of a column holding more values than a breakdown answers with is refused rather than answered in part,
	 * which is what lets the buckets of every answered breakdown add up to the count beside them. The cap is a hundred,
	 * which no fixture reaches, so the service is given a cap of one for this test alone.
	 */
	@Test
	void test06_aBreakdownThatWouldNotFitIsRefused() {
		final var service = AopTestUtils.<ErrandSearchService>getUltimateTargetObject(errandSearchService);
		ReflectionTestUtils.setField(service, "properties",
			new SearchProperties(searchProperties.maxResultWindow(), searchProperties.timeout(), 1, searchProperties.reindex()));

		try {
			// Three errands over two statuses, and only one bucket may be answered with
			setupCall()
				.withServicePath(PATH + "/count?groupBy=status")
				.withHttpMethod(GET)
				.withExpectedResponseStatus(BAD_REQUEST)
				.sendRequest();

			// The count itself is unaffected: it is the breakdown that would not fit
			final var body = countBody(PATH, "", null, null);
			assertThat(body.path("count").asLong()).isEqualTo(3);
			assertThat(body.path("group").isMissingNode()).isTrue();
		} finally {
			ReflectionTestUtils.setField(service, "properties", searchProperties);
		}
	}

	/**
	 * A draft is counted, and divided up, only by a query naming the life cycle, as the search finds it.
	 */
	@Test
	void test07_aDraftIsCountedOnlyByAQueryNamingTheLifecycle() throws InterruptedException {
		jdbcTemplate.update("UPDATE errand SET lifecycle = 'DRAFT' WHERE errand_number = ?", "NS3-25020001");
		reindex();

		assertThat(count(PATH, "")).isEqualTo(2);
		assertThat(count(PATH, "status:ongoing")).isZero();
		assertThat(count(PATH, "lifecycle:draft")).isEqualTo(1);
		assertThat(bucketsOf(group(PATH, "", "status"))).containsExactly("NEW=2");
		assertThat(bucketsOf(group(PATH, "lifecycle:*", "status"))).containsExactly("NEW=2", "ONGOING=1");
	}

	private long count(final String path, final String query) {
		return countBody(path, query, null, null).path("count").asLong();
	}

	private long countAs(final String path, final String query, final String adAccount) {
		return countBody(path, query, null, adAccount).path("count").asLong();
	}

	private JsonNode group(final String path, final String query, final String groupBy) {
		return countBody(path, query, groupBy, null).path("group");
	}

	private JsonNode groupAs(final String path, final String query, final String groupBy, final String adAccount) {
		return countBody(path, query, groupBy, adAccount).path("group");
	}

	private JsonNode countBody(final String path, final String query, final String groupBy, final String adAccount) {
		final var servicePath = withQuery(path + "/count", query) + (groupBy == null ? "" : "&groupBy=" + groupBy);
		final var call = setupCall()
			.withServicePath(servicePath)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK);

		if (adAccount != null) {
			call.withHeader(SENT_BY_HEADER, adAccount + "; type=adAccount");
		}
		return call.sendRequest().getResponseBody(new TypeReference<JsonNode>() {});
	}

	/** What a breakdown accounts for, which must always be the count it was answered beside. */
	private static long accountedFor(final JsonNode group) {
		return group.path("buckets").valueStream().mapToLong(bucket -> bucket.path("count").asLong()).sum()
			+ group.path("withoutValue").asLong() + group.path("withheld").asLong();
	}

	/** The buckets as 'value=count', in the order they were answered with. */
	private static List<String> bucketsOf(final JsonNode group) {
		return group.path("buckets").valueStream()
			.map(bucket -> bucket.path("value").asString() + "=" + bucket.path("count").asLong())
			.toList();
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
