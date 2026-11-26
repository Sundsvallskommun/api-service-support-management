package se.sundsvall.supportmanagement.apptest;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.List;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.job.JobResponse;
import se.sundsvall.supportmanagement.integration.db.JobRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.JobEntity;
import se.sundsvall.supportmanagement.model.ErrandIndex;
import se.sundsvall.supportmanagement.service.search.index.ErrandReindexService;
import se.sundsvall.supportmanagement.service.search.index.OpenSearchClient;
import se.sundsvall.supportmanagement.service.search.index.SearchIndexHealth;
import se.sundsvall.supportmanagement.service.search.index.SearchSchemaInitializer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.COMPLETED;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.FAILED;
import static se.sundsvall.supportmanagement.model.enums.JobStatus.STOPPED;
import static se.sundsvall.supportmanagement.model.enums.JobType.SEARCH_REINDEX;

/**
 * Rebuilds of the search index against a real OpenSearch, and what the service finds out about the index.
 * <p>
 * The index is rebuilt whole before every test, since the test data is loaded by SQL, which Hibernate Search never
 * sees, and the cluster is shared with every other test class.
 */
@WireMockAppTestSuite(files = "classpath:/SearchReindexIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-search.sql"
})
class SearchReindexIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-3";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String SEARCH_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/search";
	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private OpenSearchClient openSearch;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private NamespaceConfigRepository namespaceConfigRepository;

	@Autowired
	private ErrandReindexService reindexService;

	@Autowired
	private SearchIndexHealth indexHealth;

	@Autowired
	private SearchSchemaInitializer schemaInitializer;

	@BeforeEach
	void reindexEverything() throws InterruptedException {
		jobRepository.deleteAll();
		Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer().startAndWait();
	}

	/**
	 * A document of an errand the database no longer holds - one deleted while the index was not listening, here one
	 * written straight into the index, without the moment it was written as documents from before that was kept - is
	 * removed by a rebuild of its namespace, and a document of another namespace is left be.
	 */
	@Test
	void test01_aNamespaceIsRebuiltAsAJobAndWhatIsNoLongerThereIsRemoved() throws Exception {
		final var errands = search("").size();
		writeDocument("ghost-of-ns3", "{\"municipalityId\": \"2281\", \"namespace\": \"namespace-3\", \"title\": \"ghost\"}");
		writeDocument("ghost-of-another", "{\"municipalityId\": \"2281\", \"namespace\": \"namespace-other\", \"title\": \"ghost\"}");

		final var started = setupCall()
			.withServicePath(SEARCH_PATH + "/reindex")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(JobResponse.class);

		assertThat(started.getType()).isEqualTo(SEARCH_REINDEX);
		assertThat(started.getTotal()).isEqualTo(errands);

		final var ended = awaitEndOf(started.getJobId());
		assertThat(ended.getStatus()).isEqualTo(COMPLETED);
		assertThat(ended.getMessage()).isEqualTo("%d errand(s) written, 1 document(s) of errands no longer there removed".formatted(errands));
		assertThat(ended.getProgress()).isEqualTo(100);

		assertThat(documentExists("ghost-of-ns3")).isFalse();
		assertThat(documentExists("ghost-of-another")).isTrue();
		assertThat(search("")).hasSize(errands);

		final var jobs = setupCall()
			.withServicePath("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/jobs?type=SEARCH_REINDEX")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(new TypeReference<List<JobResponse>>() {});
		assertThat(jobs).extracting(JobResponse::getJobId).containsExactly(started.getJobId());

		deleteDocument("ghost-of-another");
	}

	/**
	 * Every document a rebuild writes carries the moment it was written, which is what the next rebuild goes by.
	 */
	@Test
	void test02_aRebuildWritesWhenEveryDocumentWasWritten() throws Exception {
		final var started = setupCall()
			.withServicePath(SEARCH_PATH + "/reindex")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(JobResponse.class);
		assertThat(awaitEndOf(started.getJobId()).getStatus()).isEqualTo(COMPLETED);

		final var count = new Request("POST", "/" + openSearch.errandWriteIndex() + "/_count");
		count.setJsonEntity("{\"query\": {\"bool\": {\"filter\": [{\"term\": {\"namespace\": \"namespace-3\"}}], \"must_not\": [{\"exists\": {\"field\": \"" + ErrandIndex.INDEXED_AT
			+ "\"}}]}}}");
		assertThat(read(count).path("count").asLong()).isZero();
	}

	@Test
	void test03_theNightlyReindexRebuildsEveryNamespaceAsAJobOfItsOwn() {
		reindexService.reindexEveryNamespace();

		final var namespaces = namespaceConfigRepository.findAll();
		final var jobs = jobRepository.findAll();
		assertThat(jobs).hasSize(namespaces.size()).allSatisfy(job -> {
			assertThat(job.getType()).isEqualTo(SEARCH_REINDEX);
			assertThat(job.getStatus()).isEqualTo(COMPLETED);
		});
		assertThat(jobs).extracting(JobEntity::getNamespace)
			.containsExactlyInAnyOrderElementsOf(namespaces.stream().map(config -> config.getNamespace()).toList());
	}

	/**
	 * The layout is kept in the mapping, and Hibernate Search bringing the mapping up to date when the service starts
	 * must leave it there - were it not, every index would be reported as of the first layout after every start.
	 */
	@Test
	void test04_theLayoutIsKeptWhenTheMappingIsBroughtUpToDate() throws IOException {
		indexHealth.created();

		Search.mapping(entityManagerFactory).scope(ErrandEntity.class).schemaManager().createOrUpdate();

		assertThat(mapping().path("_meta").path("layoutVersion").asInt()).isEqualTo(ErrandIndex.LAYOUT_VERSION);
		indexHealth.inspect();
		assertThat(indexHealth.fullReindexWanted()).isEmpty();
		assertThat(indexHealth.fieldUsage()).hasValueSatisfying(usage -> {
			assertThat(usage.limit()).isEqualTo(5000);
			assertThat(usage.count()).isPositive();
		});
	}

	@Test
	void test05_anIndexOfAnotherLayoutWantsAFullReindexUntilItIsCreatedAnew() throws IOException {
		final var request = new Request("PUT", "/" + openSearch.errandWriteIndex() + "/_mapping");
		request.setJsonEntity("{\"_meta\": {\"layoutVersion\": 0}}");
		openSearch.restClient().performRequest(request);

		indexHealth.inspect();
		assertThat(indexHealth.fullReindexWanted()).hasValueSatisfying(reason -> assertThat(reason).contains("layout 0"));

		indexHealth.created();
		assertThat(indexHealth.fullReindexWanted()).isEmpty();
	}

	/**
	 * Every JSON parameter of every namespace shares the one mapping of the index: a key whose value is an object in one
	 * errand and a plain value in another, as two namespaces with schemas of their own may well have it, is refused for
	 * the errand that comes second, whole. Shows the cluster doing so, with a key of its own, as the mapping it leaves
	 * behind outlives the test.
	 */
	@Test
	void test06_aJsonParameterOfAnotherShapeUnderTheSameKeyIsRefused() throws IOException {
		writeDocument("shape-1", "{\"municipalityId\": \"2281\", \"namespace\": \"namespace-a\", \"jsonParameters\": {\"shapeProbe\": {\"owner\": \"Anna\"}}}");

		final var e = assertThrows(ResponseException.class,
			() -> writeDocument("shape-2", "{\"municipalityId\": \"2281\", \"namespace\": \"namespace-b\", \"jsonParameters\": {\"shapeProbe\": {\"owner\": {\"name\": \"Anna\"}}}}"));

		assertThat(e.getResponse().getStatusLine().getStatusCode()).isEqualTo(400);
		assertThat(EntityUtils.toString(e.getResponse().getEntity())).contains("mapper_parsing_exception");
		deleteDocument("shape-1");
	}

	/**
	 * A mapping the cluster refuses - here a field of another type than the service maps it as, as a change of its type
	 * in the service would leave it - is reported as wanting a full reindex, and the full reindex puts it right.
	 */
	@Test
	void test07_aMappingTheClusterRefusesWantsAFullReindex() throws Exception {
		final var schemaManager = Search.mapping(entityManagerFactory).scope(ErrandEntity.class).schemaManager();
		try {
			schemaManager.dropIfExisting();
			final var create = new Request("PUT", "/errand-000001");
			create.setJsonEntity("""
				{"aliases": {"errand-write": {"is_write_index": true}, "errand-read": {}},
				 "mappings": {"properties": {"title": {"type": "keyword"}}}}""");
			openSearch.restClient().performRequest(create);

			schemaInitializer.createOrUpdateSchema();

			assertThat(indexHealth.fullReindexWanted()).hasValueSatisfying(reason -> assertThat(reason)
				.contains("mapper [title] cannot be changed from type [keyword] to [text]")
				.contains("POST /search/reindex"));
		} finally {
			schemaManager.dropAndCreate();
			indexHealth.created();
		}

		assertThat(indexHealth.fullReindexWanted()).isEmpty();
	}

	private JobEntity awaitEndOf(final String jobId) {
		await()
			.atMost(60, SECONDS)
			.pollDelay(ofMillis(0))
			.pollInterval(ofMillis(250))
			.until(() -> jobRepository.findById(jobId)
				.map(JobEntity::getStatus)
				.filter(List.of(COMPLETED, STOPPED, FAILED)::contains)
				.isPresent());

		return jobRepository.findById(jobId).orElseThrow();
	}

	private List<String> search(final String query) {
		return setupCall()
			.withServicePath(SEARCH_PATH + "?size=100&query=" + query)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.getResponseBody(new TypeReference<JsonNode>() {})
			.path("content").valueStream()
			.map(errand -> errand.path("errandNumber").asString())
			.toList();
	}

	private void writeDocument(final String id, final String json) throws IOException {
		final var request = new Request("PUT", "/" + openSearch.errandWriteIndex() + "/_doc/" + id);
		request.addParameter("refresh", "true");
		request.setJsonEntity(json);
		openSearch.restClient().performRequest(request);
	}

	private void deleteDocument(final String id) throws IOException {
		final var request = new Request("DELETE", "/" + openSearch.errandWriteIndex() + "/_doc/" + id);
		request.addParameter("refresh", "true");
		openSearch.restClient().performRequest(request);
	}

	private boolean documentExists(final String id) throws IOException {
		try {
			openSearch.restClient().performRequest(new Request("GET", "/" + openSearch.errandWriteIndex() + "/_doc/" + id));
			return true;
		} catch (final ResponseException e) {
			if (e.getResponse().getStatusLine().getStatusCode() == 404) {
				return false;
			}
			throw e;
		}
	}

	private JsonNode mapping() throws IOException {
		return read(new Request("GET", "/" + openSearch.errandWriteIndex() + "/_mapping")).iterator().next().path("mappings");
	}

	private JsonNode read(final Request request) throws IOException {
		try (var content = openSearch.restClient().performRequest(request).getEntity().getContent()) {
			return MAPPER.readTree(content);
		}
	}
}
