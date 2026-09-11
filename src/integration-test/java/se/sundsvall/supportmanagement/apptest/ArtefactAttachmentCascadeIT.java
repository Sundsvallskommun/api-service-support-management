package se.sundsvall.supportmanagement.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.util.List;
import net.javacrumbs.jsonunit.core.Option;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;

/**
 * The five removal cases the attachment link has to survive.
 * <p>
 * Every one of them verifies <b>both</b> halves of its requirement - that the right row went, and that the right row
 * stayed. A test that only checked the first would say yes to a model that takes the attachment with it, which is exactly
 * the failure the link exists to prevent.
 * <p>
 * The rows are counted with SQL rather than read back through JPA on purpose: a collection in memory can be stale where
 * the database is right, and it is the database these cases are about.
 */
@WireMockAppTestSuite(files = "classpath:/ArtefactAttachmentCascadeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ArtefactAttachmentCascadeIT extends AbstractAppTest {

	private static final String NAMESPACE = "NAMESPACE-ARTEFACT";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "a0000000-0000-0000-0000-000000000001";
	private static final String OTHER_ERRAND_ID = "cc236cf1-c00f-4479-8341-ecf5dd90b5b9";

	private static final String STATEMENT_ID = "f1000000-0000-0000-0000-000000000001";
	private static final String ATTACHMENT_ID = "a5000000-0000-0000-0000-000000000001";
	private static final String SUPPORTING_PURPOSE_ID = "f6000000-0000-0000-0000-000000000001";
	private static final String RESPONSE_PURPOSE_ID = "f6000000-0000-0000-0000-000000000002";

	/** An attachment of the other errand, used to show that a link across errands cannot be made. */
	private static final String OTHER_ERRAND_ATTACHMENT_ID = "c697642d-4d8d-4b07-8816-025a2734b09a";

	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID;
	private static final String STATEMENT_PATH = ERRAND_PATH + "/statements/" + STATEMENT_ID;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Case 1 - the attachment is removed. The link goes with it; the statement lives on.
	 */
	@Test
	void test01_deletingAttachmentRemovesLinkButKeepsStatement() {

		assertThat(links()).isOne();

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments/" + ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(links()).as("the link went with the attachment").isZero();
		assertThat(statements()).as("the statement stayed").isOne();
	}

	/**
	 * Case 2 - the attachment is unlinked from the statement. The link goes; the attachment stays on the errand.
	 */
	@Test
	void test02_unlinkingKeepsAttachmentOnErrand() {

		assertThat(links()).isOne();

		setupCall()
			.withServicePath(STATEMENT_PATH + "/attachments/" + ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(links()).as("the link went").isZero();
		assertThat(attachments()).as("the attachment stayed on the errand").isOne();
		assertThat(statements()).as("the statement stayed").isOne();
	}

	/**
	 * Case 3 - the statement is removed. Its links go; the attachments they pointed at stay on the errand.
	 */
	@Test
	void test03_deletingStatementKeepsAttachments() {

		assertThat(links()).isOne();

		setupCall()
			.withServicePath(STATEMENT_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(links()).as("the link went with the statement").isZero();
		assertThat(statements()).as("the statement went").isZero();
		assertThat(attachments()).as("the attachment stayed on the errand").isOne();
	}

	/**
	 * Case 4 - the attachment is removed through the errand rather than named directly. Same outcome as case 1, and the
	 * point of testing it separately is that it takes a different route through the service.
	 */
	@Test
	void test04_deletingAttachmentViaErrandRemovesLink() {

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments/" + ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(attachments()).as("the attachment went").isZero();
		assertThat(links()).as("the link went with it").isZero();
		assertThat(statements()).as("the statement stayed").isOne();
	}

	/**
	 * Case 5 - the errand is removed. Everything goes: the statement, its links and the attachments.
	 */
	@Test
	void test05_deletingErrandRemovesEverything() {

		assertThat(statements()).isOne();
		assertThat(links()).isOne();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(statements()).as("the statement went with the errand").isZero();
		assertThat(links()).as("the links went").isZero();
		assertThat(attachments()).as("the attachments went").isZero();
	}

	/**
	 * The invariant JPA cannot express: the two foreign keys of a link know nothing about each other, so nothing but the
	 * lookup stops an attachment of one errand from being linked to an artefact of another. Fetching the attachment through
	 * the errand is what makes it a 404 rather than a link nobody meant to allow.
	 */
	@Test
	void test06_linkingAttachmentFromAnotherErrandGives404() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/attachments/" + OTHER_ERRAND_ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withRequest("{\"sortOrder\":1}")
			.withExpectedResponseStatus(NOT_FOUND)
			.sendRequestAndVerifyResponse();

		assertThat(linksTo(OTHER_ERRAND_ATTACHMENT_ID)).as("no link across errands was written").isZero();
		assertThat(attachmentsOf(OTHER_ERRAND_ID)).as("the other errand kept its attachment").isPositive();
	}

	/**
	 * Linking an attachment that is already linked says so rather than writing a second row - the unique key on the pair
	 * would refuse it anyway, and a constraint violation deep in the flush is not an answer a caller can act on.
	 */
	@Test
	void test07_linkingTwiceIsAConflict() {

		setupCall()
			.withServicePath(STATEMENT_PATH + "/attachments/" + ATTACHMENT_ID)
			.withHttpMethod(POST)
			.withRequest("{\"sortOrder\":2}")
			.withExpectedResponseStatus(CONFLICT)
			.sendRequestAndVerifyResponse();

		assertThat(links()).as("still exactly one link").isOne();
	}

	/**
	 * Uploading through the statement puts the attachment on the errand and links it in one call, which is what makes the
	 * errand attachment resource the place it is read from.
	 */
	@Test
	void test08_uploadingThroughStatementPutsAttachmentOnErrand() throws Exception {

		final var attachmentsBefore = attachmentsOf(ERRAND_ID);

		setupCall()
			.withServicePath(STATEMENT_PATH + "/attachments?sortOrder=2")
			.withHttpMethod(POST)
			.withContentType(MULTIPART_FORM_DATA)
			.withRequestFile("attachment", "test.txt")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION,
				List.of(ERRAND_PATH + "/attachments/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
			.sendRequestAndVerifyResponse();

		assertThat(attachmentsOf(ERRAND_ID)).as("the attachment is an attachment of the errand").isEqualTo(attachmentsBefore + 1);
		assertThat(links()).as("and it is linked to the statement").isEqualTo(2);
	}

	/**
	 * The purpose belongs to the attachment, not to any link to it. That is what lets the errand show it in its own
	 * attachment list, and what lets a file linked to nothing carry one at all.
	 */
	@Test
	void test09_purposeIsWrittenOnTheAttachmentAndShownByTheErrand() {

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments/" + ATTACHMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest("{\"purpose\":{\"id\":\"" + RESPONSE_PURPOSE_ID + "\"}}")
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		assertThat(purposeOf(ATTACHMENT_ID)).as("the purpose landed on the attachment").isEqualTo(RESPONSE_PURPOSE_ID);

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withJsonAssertOptions(List.of(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER))
			.withExpectedResponse("response.json")
			.sendRequest();
	}

	/**
	 * A purpose the namespace has not registered is refused, so an id from somewhere else cannot become a purpose here.
	 */
	@Test
	void test10_anUnregisteredPurposeIsRejected() {

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments/" + ATTACHMENT_ID)
			.withHttpMethod(PATCH)
			.withRequest("{\"purpose\":{\"id\":\"f6000000-0000-0000-0000-0000000000ff\"}}")
			.withExpectedResponseStatus(BAD_REQUEST)
			.sendRequestAndVerifyResponse();

		assertThat(purposeOf(ATTACHMENT_ID)).as("what was there stayed").isEqualTo(SUPPORTING_PURPOSE_ID);
	}

	/**
	 * Clearing the purpose takes the reference away and nothing else: the purpose stays in the metadata of the namespace.
	 */
	@Test
	void test11_clearingThePurposeLeavesTheMetadataAlone() {

		setupCall()
			.withServicePath(ERRAND_PATH + "/attachments/" + ATTACHMENT_ID + "/purpose")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();

		assertThat(purposeOf(ATTACHMENT_ID)).as("the reference went").isNull();
		assertThat(jdbcTemplate.queryForObject("select count(*) from attachment_purpose where id = ?", Integer.class, SUPPORTING_PURPOSE_ID))
			.as("the purpose stayed").isOne();
	}

	private String purposeOf(final String attachmentId) {
		return jdbcTemplate.queryForObject("select attachment_purpose_id from attachment where id = ?", String.class, attachmentId);
	}

	private int links() {
		return jdbcTemplate.queryForObject("select count(*) from statement_attachment where statement_id = ?", Integer.class, STATEMENT_ID);
	}

	private int linksTo(final String attachmentId) {
		return jdbcTemplate.queryForObject("select count(*) from statement_attachment where attachment_id = ?", Integer.class, attachmentId);
	}

	private int statements() {
		return jdbcTemplate.queryForObject("select count(*) from statement where id = ?", Integer.class, STATEMENT_ID);
	}

	/** Only the attachment these cases own - the errand carries others, seeded for tests about something else. */
	private int attachments() {
		return jdbcTemplate.queryForObject("select count(*) from attachment where id = ?", Integer.class, ATTACHMENT_ID);
	}

	private int attachmentsOf(final String errandId) {
		return jdbcTemplate.queryForObject("select count(*) from attachment where errand_id = ?", Integer.class, errandId);
	}
}
