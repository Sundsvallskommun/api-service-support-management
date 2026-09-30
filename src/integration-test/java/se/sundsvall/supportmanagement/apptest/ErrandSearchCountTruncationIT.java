package se.sundsvall.supportmanagement.apptest;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.search.mapper.orm.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

/**
 * A breakdown of a column holding more values than are answered with. The cap is a hundred in production, which no
 * fixture would reach, so it is lowered to one here: the point is that a partial breakdown says it is partial, and that
 * the count beside it still counts everything.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandSearchIT/", classes = Application.class)
@TestPropertySource(properties = "search.max-group-buckets=1")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-search.sql"
})
class ErrandSearchCountTruncationIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-3/errands/search/count";

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void reindex() throws InterruptedException {
		Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer().startAndWait();
	}

	@Test
	void test01_aBreakdownLeavingSomethingOutSaysSo() {
		final var body = setupCall()
			.withServicePath(PATH + "?groupBy=status")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.getResponseBody(new TypeReference<JsonNode>() {});

		// Three errands over two statuses: everything is counted, and the largest bucket alone is answered with
		assertThat(body.path("count").asLong()).isEqualTo(3);
		assertThat(body.path("group").path("truncated").asBoolean()).isTrue();
		assertThat(body.path("group").path("buckets")).hasSize(1);
		assertThat(body.path("group").path("buckets").get(0).path("value").asString()).isEqualTo("NEW");
		assertThat(body.path("group").path("buckets").get(0).path("count").asLong()).isEqualTo(2);
	}
}
