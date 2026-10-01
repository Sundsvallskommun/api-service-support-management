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
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

/**
 * A breakdown of a column holding more values than a breakdown answers with. The cap is a hundred in production, which
 * no fixture would reach, so it is lowered to one here: the point is that such a breakdown is refused rather than
 * answered in part, which is what lets the buckets of every answered breakdown add up to the count beside them.
 */
@WireMockAppTestSuite(files = "classpath:/ErrandSearchIT/", classes = Application.class)
@TestPropertySource(properties = "search.max-group-buckets=1")
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-it-search.sql"
})
class ErrandSearchCountTooManyGroupsIT extends AbstractAppTest {

	private static final String PATH = "/2281/NAMESPACE-3/errands/search/count";

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void reindex() throws InterruptedException {
		Search.mapping(entityManagerFactory).scope(ErrandEntity.class).massIndexer().startAndWait();
	}

	@Test
	void test01_aBreakdownThatWouldNotFitIsRefused() {
		// Three errands over two statuses, and only one bucket may be answered with
		setupCall()
			.withServicePath(PATH + "?groupBy=status")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequest();

		// The count itself is unaffected: it is the breakdown that would not fit
		final var body = setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.getResponseBody(new TypeReference<JsonNode>() {});

		assertThat(body.path("count").asLong()).isEqualTo(3);
		assertThat(body.path("group").isMissingNode()).isTrue();
	}
}
