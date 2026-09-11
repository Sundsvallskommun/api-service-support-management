package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * The lifecycle of a JSON parameter owned by a handling artefact - the mirror image of the attachment link.
 * <p>
 * A attachment survives the artefact that pointed at it; the business content of the artefact does not. These cases are
 * what say so, and each of them checks both halves: what went, and what stayed.
 * <p>
 * Counted with SQL rather than read back through JPA, since a collection in memory can be stale where the database is
 * right - and it is the database these cases are about.
 */
@WireMockAppTestSuite(files = "classpath:/ArtefactJsonParameterCascadeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ArtefactJsonParameterCascadeIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";

	private static final String STATEMENT_ID = "f1000000-0000-0000-0000-000000000001";
	private static final String PARAMETER_ID = "f8000000-0000-0000-0000-000000000001";
	private static final String PARAMETER_KEY = "responseForm";
	private static final String PARAMETER = """
		{"key":"responseForm","schemaId":"test-schema-1.0","value":{"answer":"pending"},"version":0}""";

	/** A key the errand already holds, owned by nothing in particular. */
	private static final String ERRAND_OWNED_KEY = "formData";

	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String STATEMENT_PATH = ERRAND_PATH + "/statements/" + STATEMENT_ID;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * The parameter is read through the statement that owns it, and it is also an ordinary parameter of the errand -
	 * that is what the link arrangement buys.
	 */
	@Test
	void test01_theParameterIsBothTheStatementsAndTheErrands() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + PARAMETER_KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PARAMETER)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(ERRAND_PATH + "/json-parameters/" + PARAMETER_KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(PARAMETER)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Removing the parameter leaves the statement standing. The link goes with the parameter, since it exists only to
	 * say whose content it is.
	 */
	@Test
	void test02_deletingParameterKeepsStatement() {

		assertThat(parameters()).isOne();
		assertThat(links()).isOne();

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + PARAMETER_KEY)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(parameters()).as("the parameter went").isZero();
		assertThat(links()).as("the link went with it").isZero();
		assertThat(statements()).as("the statement stayed").isOne();
	}

	/**
	 * Removing the statement takes its content with it - which is the whole difference from an attachment, and the
	 * reason the parameter is linked rather than merely referenced.
	 */
	@Test
	void test03_deletingStatementTakesItsParameter() {

		assertThat(parameters()).isOne();

		setupCall()
			.withServicePath(STATEMENT_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(statements()).as("the statement went").isZero();
		assertThat(links()).as("the link went").isZero();
		assertThat(parameters()).as("the content of the statement went with it").isZero();
		assertThat(errandParameters(ERRAND_OWNED_KEY)).as("what the errand owned itself stayed").isOne();
	}

	/**
	 * The errand takes everything.
	 */
	@Test
	void test04_deletingErrandRemovesEverything() {

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(statements()).isZero();
		assertThat(links()).isZero();
		assertThat(parameters()).isZero();
		assertThat(errandParameters(ERRAND_OWNED_KEY)).isZero();
	}

	/**
	 * Keys are unique per errand, and the database is what says so. An artefact asking for a key something else on the
	 * errand already holds is told, rather than quietly taking it over.
	 */
	@Test
	void test05_claimingAKeyTheErrandAlreadyHoldsIsAConflict() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + ERRAND_OWNED_KEY)
			.withHttpMethod(PUT)
			.withRequest("request.json")
			.withExpectedResponseStatus(CONFLICT)
			.sendRequestAndVerifyResponse();

		assertThat(links()).as("no second link was written").isOne();
	}

	/**
	 * Writing a key the artefact does not hold yet creates both the parameter and the link, and answers 201.
	 */
	@Test
	void test06_writingANewKeyCreatesParameterAndLink() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest("request.json")
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(links()).as("the statement now owns two parameters").isEqualTo(2);
		assertThat(errandParameters("dispatchLog")).as("and the errand carries the new one").isOne();
	}

	/**
	 * A key belonging to another artefact, or to nothing at all, is not this artefact's to read.
	 */
	@Test
	void test07_readingAKeyTheStatementDoesNotOwnGives404() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + ERRAND_OWNED_KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The errand holds the parameters of its artefacts, but a patch replacing its own leaves theirs as they stand -
	 * otherwise it would take the content of every artefact with it.
	 */
	@Test
	void test08_patchingTheErrandLeavesArtefactParametersAlone() {

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select value from json_parameter where errand_id = ? and parameter_key = ?", String.class, ERRAND_ID, ERRAND_OWNED_KEY))
			.as("the errand wrote its own parameter").contains("Jane");
		assertThat(List.of(PARAMETER_KEY, "investigationForm", "sectionForm", "decisionForm", "measureForm"))
			.as("every artefact kept its parameter").allSatisfy(key -> assertThat(errandParameters(key)).as(key).isOne());
		assertThat(linkCounts()).as("and its link").allSatisfy((table, count) -> assertThat(count).as(table).isOne());
	}

	/**
	 * Changing the content of an artefact through the errand is refused rather than ignored, so the caller learns where
	 * it is written. Nothing of the patch is applied - including the removal of the parameter of the errand it left out.
	 */
	@Test
	void test09_changingAnArtefactParameterThroughTheErrandIsAConflict() {

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withRequest("request.json")
			.withExpectedResponseStatus(CONFLICT)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select value from json_parameter where id = ?", String.class, PARAMETER_ID))
			.as("the content of the statement stayed as it was").contains("pending");
		assertThat(errandParameters(ERRAND_OWNED_KEY)).as("and the errand kept its own parameter").isOne();
	}

	private int links() {
		return jdbcTemplate.queryForObject("select count(*) from statement_json_parameter where statement_id = ?", Integer.class, STATEMENT_ID);
	}

	/** The links of the parameters the artefacts of the errand own - one of each kind is seeded. */
	private Map<String, Integer> linkCounts() {
		return Map.of(
			"statement_json_parameter", jdbcTemplate.queryForObject("select count(*) from statement_json_parameter", Integer.class),
			"investigation_json_parameter", jdbcTemplate.queryForObject("select count(*) from investigation_json_parameter", Integer.class),
			"investigation_section_json_parameter", jdbcTemplate.queryForObject("select count(*) from investigation_section_json_parameter", Integer.class),
			"decision_json_parameter", jdbcTemplate.queryForObject("select count(*) from decision_json_parameter", Integer.class),
			"measure_json_parameter", jdbcTemplate.queryForObject("select count(*) from measure_json_parameter", Integer.class));
	}

	private int parameters() {
		return jdbcTemplate.queryForObject("select count(*) from json_parameter where id = ?", Integer.class, PARAMETER_ID);
	}

	private int errandParameters(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from json_parameter where errand_id = ? and parameter_key = ?", Integer.class, ERRAND_ID, key);
	}

	private int statements() {
		return jdbcTemplate.queryForObject("select count(*) from statement where id = ?", Integer.class, STATEMENT_ID);
	}
}
