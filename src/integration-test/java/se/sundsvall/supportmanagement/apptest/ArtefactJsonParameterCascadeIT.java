package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * The JSON parameters of a handling artefact, kept beside the artefact rather than among those of the errand.
 * <p>
 * The errand and each of its artefacts hold parameters of their own: neither shows the other's, and a key one of them
 * uses is free for the others. The parameters of an artefact go with it, where an attachment linked to it stays on the
 * errand. These cases are what say so, and each of them checks both halves: what went, and what stayed.
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
	private static final String DECISION_ID = "f4000000-0000-0000-0000-000000000001";

	private static final String STATEMENT_KEY = "responseForm";
	private static final String STATEMENT_PARAMETER = """
		{"key":"responseForm","schemaId":"test-schema-1.0","value":{"answer":"pending"},"version":0}""";

	/** The key of the parameter the errand holds itself. */
	private static final String ERRAND_KEY = "formData";

	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String STATEMENT_PATH = ERRAND_PATH + "/statements/" + STATEMENT_ID;
	private static final String DECISION_PATH = ERRAND_PATH + "/decisions/" + DECISION_ID;
	private static final String REQUEST_FILE = "request.json";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * The parameter is read through the statement holding it, and is not one of the errand - reading the errand says
	 * nothing of the content of its artefacts.
	 */
	@Test
	void test01_theParameterIsTheStatementsAndNotTheErrands() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + STATEMENT_KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(STATEMENT_PARAMETER)
			.sendRequestAndVerifyResponse();

		setupCall()
			.withServicePath(ERRAND_PATH + "/json-parameters/" + STATEMENT_KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * Removing the parameter leaves the statement standing.
	 */
	@Test
	void test02_deletingParameterKeepsStatement() {

		assertThat(statementParameters(STATEMENT_KEY)).isOne();

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + STATEMENT_KEY)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(statementParameters(STATEMENT_KEY)).as("the parameter went").isZero();
		assertThat(statements()).as("the statement stayed").isOne();
	}

	/**
	 * Removing the statement takes its content with it - which is the whole difference from an attachment - and leaves
	 * what the errand holds itself.
	 */
	@Test
	void test03_deletingStatementTakesItsParameters() {

		setupCall()
			.withServicePath(STATEMENT_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(statements()).as("the statement went").isZero();
		assertThat(statementParameters(STATEMENT_KEY)).as("the content of the statement went with it").isZero();
		assertThat(errandParameters(ERRAND_KEY)).as("what the errand holds itself stayed").isOne();
	}

	/**
	 * The errand takes everything, the parameters of every one of its artefacts included.
	 */
	@Test
	void test04_deletingErrandRemovesEverything() {

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(statements()).isZero();
		assertThat(errandParameters(ERRAND_KEY)).isZero();
		assertThat(artefactParameterCounts()).allSatisfy((table, count) -> assertThat(count).as(table).isZero());
	}

	/**
	 * A key the errand uses is free for the statement. The two are separate parameters, and writing the one leaves the
	 * other as it was.
	 */
	@Test
	void test05_theErrandAndTheStatementEachHoldTheSameKey() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + ERRAND_KEY)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(statementParameters(ERRAND_KEY)).as("the statement holds the key").isOne();
		assertThat(errandValue(ERRAND_KEY)).as("and the errand kept its own under it").contains("John");
	}

	/**
	 * Writing a key the statement does not hold yet creates the parameter, on the statement alone.
	 */
	@Test
	void test06_writingANewKeyCreatesTheParameter() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/dispatchLog")
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(statementParameters("dispatchLog")).as("the statement holds the new parameter").isOne();
		assertThat(errandParameters("dispatchLog")).as("and the errand does not").isZero();
	}

	/**
	 * A key the errand holds is not the statement's to read.
	 */
	@Test
	void test07_readingAKeyTheStatementDoesNotHoldGives404() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/json-parameters/" + ERRAND_KEY)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * A patch replacing the parameters of the errand leaves those of its artefacts as they stand.
	 */
	@Test
	void test08_patchingTheErrandLeavesArtefactParametersAlone() {

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(errandValue(ERRAND_KEY)).as("the errand wrote its own parameter").contains("Jane");
		assertThat(artefactParameterCounts()).as("every artefact kept its parameter").allSatisfy((table, count) -> assertThat(count).as(table).isOne());
	}

	/**
	 * A patch of the errand naming the key of the statement writes a parameter of the errand, and leaves the one of the
	 * statement as it was.
	 */
	@Test
	void test09_patchingTheErrandWithTheKeyOfTheStatementWritesTheErrandsOwn() {

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(errandValue(STATEMENT_KEY)).as("the errand holds the key now").contains("changed");
		assertThat(statementValue(STATEMENT_KEY)).as("the content of the statement stayed as it was").contains("pending");
	}

	/**
	 * The same through the endpoint of the errand naming the key.
	 */
	@Test
	void test10_writingTheKeyOfTheStatementThroughTheErrandCreatesTheErrandsOwn() {

		setupCall()
			.withServicePath(ERRAND_PATH + "/json-parameters/" + STATEMENT_KEY)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(errandParameters(STATEMENT_KEY)).as("the errand holds the key now").isOne();
		assertThat(statementValue(STATEMENT_KEY)).as("the content of the statement stayed as it was").contains("pending");
	}

	/**
	 * A key only the statement holds is not there for the errand to remove.
	 */
	@Test
	void test11_deletingTheKeyOfTheStatementThroughTheErrandGives404() {

		setupCall()
			.withServicePath(ERRAND_PATH + "/json-parameters/" + STATEMENT_KEY)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		assertThat(statementParameters(STATEMENT_KEY)).as("the parameter of the statement stayed").isOne();
	}

	/**
	 * Keys are unique per artefact rather than per errand, so two artefacts of one errand may each hold the same one.
	 */
	@Test
	void test12_twoArtefactsOfOneErrandEachHoldTheSameKey() {

		setupCall()
			.withServicePath(DECISION_PATH + "/json-parameters/" + STATEMENT_KEY)
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.sendRequestAndVerifyResponse();

		assertThat(jdbcTemplate.queryForObject("select count(*) from decision_json_parameter where decision_id = ? and parameter_key = ?", Integer.class, DECISION_ID, STATEMENT_KEY))
			.as("the decision holds the key").isOne();
		assertThat(statementValue(STATEMENT_KEY)).as("and the statement kept its own under it").contains("pending");
	}

	private int statements() {
		return jdbcTemplate.queryForObject("select count(*) from statement where id = ?", Integer.class, STATEMENT_ID);
	}

	private int statementParameters(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from statement_json_parameter where statement_id = ? and parameter_key = ?", Integer.class, STATEMENT_ID, key);
	}

	private String statementValue(final String key) {
		return jdbcTemplate.queryForObject("select value from statement_json_parameter where statement_id = ? and parameter_key = ?", String.class, STATEMENT_ID, key);
	}

	private int errandParameters(final String key) {
		return jdbcTemplate.queryForObject("select count(*) from json_parameter where errand_id = ? and parameter_key = ?", Integer.class, ERRAND_ID, key);
	}

	private String errandValue(final String key) {
		return jdbcTemplate.queryForObject("select value from json_parameter where errand_id = ? and parameter_key = ?", String.class, ERRAND_ID, key);
	}

	/** The parameters of the artefacts of the errand - one of each kind is seeded. */
	private Map<String, Integer> artefactParameterCounts() {
		return Map.of(
			"statement_json_parameter", jdbcTemplate.queryForObject("select count(*) from statement_json_parameter", Integer.class),
			"investigation_json_parameter", jdbcTemplate.queryForObject("select count(*) from investigation_json_parameter", Integer.class),
			"investigation_section_json_parameter", jdbcTemplate.queryForObject("select count(*) from investigation_section_json_parameter", Integer.class),
			"decision_json_parameter", jdbcTemplate.queryForObject("select count(*) from decision_json_parameter", Integer.class),
			"measure_json_parameter", jdbcTemplate.queryForObject("select count(*) from measure_json_parameter", Integer.class));
	}
}
