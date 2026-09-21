package se.sundsvall.supportmanagement.apptest;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ProcessEventOutboxRepository;
import se.sundsvall.supportmanagement.integration.db.model.ProcessEventOutboxEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.scheduler.processevent.ProcessEventRelay;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.supportmanagement.Constants.SENT_BY_HEADER;
import static se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType.ERRAND;

/**
 * The start mode of the labels over the wire: the permission an ordinary errand event carries to pw-alkt, and the checks
 * a label write is held to.
 * <p>
 * A changed errand wakes the process of PROCESS-NAMESPACE (testdata-process-loop-guard.sql), and the direct run is off,
 * so every event written stays undelivered until a test delivers it.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessStartModeIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-loop-guard.sql",
	"/db/scripts/testdata-process-start-mode.sql"
})
class ProcessStartModeIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-NAMESPACE";
	private static final String ERRANDS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";
	private static final String LABELS_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/metadata/labels";
	private static final String PW_ALKT_PATH = "/api-pw-alkt/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/process/errand-events";

	private static final String LABEL_WITHOUT_START_MODE_ID = "dd000000-0000-0000-0000-0000000000d2";
	private static final String MANUAL_APPLICATION_LABEL_ID = "ef000000-0000-0000-0000-0000000000f1";
	private static final String AUTOMATIC_APPLICATION_LABEL_ID = "ef000000-0000-0000-0000-0000000000f3";

	private static final String FAILED_ERRAND_WEARING_TWO_LABELS_ID = "ac000000-0000-0000-0000-0000000000c1";
	private static final String COMPLETED_ERRAND_ID = "ac000000-0000-0000-0000-0000000000c2";
	private static final String FAILED_ERRAND_ID = "ac000000-0000-0000-0000-0000000000c3";

	private static final String APPLICATION = "alkt-ansokan";
	private static final String HANDLER_IDENTITY = "joe01doe; type=adAccount";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private ProcessEventOutboxRepository outboxRepository;

	@Autowired
	private ProcessEventRelay processEventRelay;

	@Autowired
	private NamespaceConfigService namespaceConfigService;

	private int patches;

	@BeforeEach
	void setUp() {
		wiremock.resetAll();
		wiremock.stubFor(post(urlPathEqualTo("/api-gateway/token"))
			.willReturn(aResponse()
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("common/responses/api-gateway-token-response.json")));
		wiremock.stubFor(post(urlPathMatching("/api-eventlog/.*")).willReturn(aResponse().withStatus(202)));
		wiremock.stubFor(post(urlPathEqualTo(PW_ALKT_PATH)).willReturn(aResponse().withStatus(202)));
	}

	@Test
	@DisplayName("Verification that an errand created with a MANUAL label is published without the permission to start, and one with an AUTOMATIC label or no start mode with it")
	void test01_theStartModeOfTheLabelDecidesThePermissionOfACreation() {
		assertThat(namespaceConfigService.getProcessTriggers(NAMESPACE, MUNICIPALITY_ID)).containsOnly(ERRAND);

		assertThat(creationRowFor(MANUAL_APPLICATION_LABEL_ID).isStartAllowed()).isFalse();
		assertThat(creationRowFor(LABEL_WITHOUT_START_MODE_ID).isStartAllowed()).isTrue();

		final var automatic = creationRowFor(AUTOMATIC_APPLICATION_LABEL_ID);
		assertThat(automatic.isStartAllowed()).isTrue();

		processEventRelay.relayErrand(automatic.getErrandId());

		assertThat(eventsReceivedByProcess()).singleElement().satisfies(event -> {
			assertThat(event.path("eventId").asString()).isEqualTo(automatic.getId());
			assertThat(event.path("eventType").asString()).isEqualTo("CREATE");
			assertThat(event.path("processKey").asString()).isEqualTo(APPLICATION);
			assertThat(event.path("startAllowed").booleanValue()).isTrue();
		});
	}

	/**
	 * The errand runs the application process and wears a MANUAL application label and an AUTOMATIC supervision label.
	 * The row carries the key of the instance, and a start mode read off the supervision label would give it the
	 * permission.
	 */
	@Test
	@DisplayName("Verification that the start mode is read only off the label naming the key the event carries")
	void test02_theKeyAndTheStartModeComeFromTheSameLabel() {
		assertThat(rowWrittenByPatching(FAILED_ERRAND_WEARING_TWO_LABELS_ID)).satisfies(row -> {
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
			assertThat(row.isStartAllowed()).isFalse();
		});
	}

	@Test
	@DisplayName("Verification that an errand whose process ran to its end is published without the permission to start, and one whose only start failed with it")
	void test03_theProcessHistoryDecidesThePermissionOfAChange() {
		assertThat(rowWrittenByPatching(COMPLETED_ERRAND_ID).isStartAllowed()).isFalse();
		assertThat(rowWrittenByPatching(FAILED_ERRAND_ID).isStartAllowed()).isTrue();
	}

	@Test
	@DisplayName("Verification that a label write is refused when its process attributes cannot be read as written, and taken when they can")
	void test04_aLabelWriteIsHeldToTheStartModes() {
		for (final var method : List.of(POST, PUT)) {
			assertThat(writeLabel(method, """
				[{"classification": "CATEGORY", "resourceName": "NY_TILLSYN", "attributes": [
				  {"key": "processKey", "value": "alkt-tillsyn"}, {"key": "processStartMode", "value": "manual"}]}]""").getStatusCode())
				.isEqualTo(BAD_REQUEST);
			assertThat(writeLabel(method, """
				[{"classification": "CATEGORY", "resourceName": "NY_TILLSYN", "attributes": [{"key": "processStartMode", "value": "MANUAL"}]}]""").getStatusCode())
				.isEqualTo(BAD_REQUEST);
			assertThat(writeLabel(method, """
				[{"classification": "CATEGORY", "resourceName": "NY_TILLSYN", "attributes": [
				  {"key": "processKey", "value": "alkt-tillsyn"}, {"key": "processstartmode", "value": "MANUAL"}]}]""").getStatusCode())
				.isEqualTo(BAD_REQUEST);
		}

		assertThat(writeLabel(POST, """
			[{"classification": "CATEGORY", "resourceName": "NY_TILLSYN", "attributes": [
			  {"key": "processKey", "value": "alkt-tillsyn"}, {"key": "processStartMode", "value": "MANUAL"}]}]""").getStatusCode())
			.isEqualTo(ACCEPTED);

		final var labels = exchange(GET, LABELS_PATH, null);
		assertThat(labels.getStatusCode()).isEqualTo(OK);
		assertThat(OBJECT_MAPPER.readTree(labels.getBody()).path("labelStructure").valueStream()
			.filter(label -> "NY_TILLSYN".equals(label.path("resourceName").asString()))
			.flatMap(label -> label.path("attributes").valueStream())
			.map(attribute -> attribute.path("key").asString() + "=" + attribute.path("value").asString()))
			.containsExactlyInAnyOrder("processKey=alkt-tillsyn", "processStartMode=MANUAL");
	}

	/**
	 * Creates an errand wearing the label, and returns the one row its creation wrote.
	 */
	private ProcessEventOutboxEntity creationRowFor(final String labelId) {
		final var before = outboxIds();
		final var response = exchange(POST, ERRANDS_PATH, """
			{
			  "title": "Ansokan om serveringstillstand",
			  "priority": "MEDIUM",
			  "status": "STATUS-1",
			  "reporterUserId": "joe01doe",
			  "classification": {"category": "CATEGORY-1", "type": "TYPE-1"},
			  "labels": [{"id": "%s"}]
			}""".formatted(labelId));

		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		final var location = response.getHeaders().getLocation().getPath();
		final var errandId = location.substring(location.lastIndexOf('/') + 1);

		final var rows = rowsWrittenSince(before);
		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getEventType()).isEqualTo("CREATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
			assertThat(row.getProcessKey()).isEqualTo(APPLICATION);
		});
		return rows.getFirst();
	}

	/**
	 * Patches the description of the errand with a value it has not had before, and returns the one row the change
	 * wrote.
	 */
	private ProcessEventOutboxEntity rowWrittenByPatching(final String errandId) {
		final var before = outboxIds();
		final var body = """
			{"description": "Looked at in round %d"}""".formatted(++patches);

		assertThat(exchange(PATCH, ERRANDS_PATH + "/" + errandId, body).getStatusCode()).isEqualTo(OK);

		final var rows = rowsWrittenSince(before);
		assertThat(rows).singleElement().satisfies(row -> {
			assertThat(row.getErrandId()).isEqualTo(errandId);
			assertThat(row.getEventType()).isEqualTo("UPDATE");
			assertThat(row.getEventSubType()).isEqualTo("ERRAND");
		});
		return rows.getFirst();
	}

	private ResponseEntity<String> writeLabel(final HttpMethod method, final String body) {
		return exchange(method, LABELS_PATH, body);
	}

	private ResponseEntity<String> exchange(final HttpMethod method, final String path, final String body) {
		final var headers = new HttpHeaders();
		if (body != null) {
			headers.setContentType(APPLICATION_JSON);
		}
		headers.add(SENT_BY_HEADER, HANDLER_IDENTITY);
		return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
	}

	private List<JsonNode> eventsReceivedByProcess() {
		return wiremock.findAll(postRequestedFor(urlPathEqualTo(PW_ALKT_PATH))).stream()
			.map(LoggedRequest::getBodyAsString)
			.map(OBJECT_MAPPER::readTree)
			.toList();
	}

	private Set<String> outboxIds() {
		return outboxRepository.findAll().stream().map(ProcessEventOutboxEntity::getId).collect(toSet());
	}

	private List<ProcessEventOutboxEntity> rowsWrittenSince(final Set<String> before) {
		return outboxRepository.findAll().stream()
			.filter(row -> !before.contains(row.getId()))
			.toList();
	}
}
