package se.sundsvall.supportmanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcess;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcesses;
import se.sundsvall.supportmanagement.api.model.process.ProcessActivity;
import se.sundsvall.supportmanagement.service.ErrandProcessService;
import se.sundsvall.supportmanagement.service.model.ErrandProcessResult;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;

@AutoConfigureWebTestClient
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
class ErrandProcessResourceTest {

	private static final String PROCESSES_PATH = "/{municipalityId}/{namespace}/errands/{errandId}/processes";
	private static final String PROCESS_PATH = PROCESSES_PATH + "/{processInstanceId}";
	private static final String ACTIVITIES_PATH = "/{municipalityId}/{namespace}/errands/{errandId}/process-activities";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PROCESS_INSTANCE_ID = "8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ErrandProcessService serviceMock;

	private static Map<String, Object> errandVariables() {
		return Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID);
	}

	private static Map<String, Object> instanceVariables() {
		return Map.of("namespace", NAMESPACE, "municipalityId", MUNICIPALITY_ID, "errandId", ERRAND_ID, "processInstanceId", PROCESS_INSTANCE_ID);
	}

	private static ErrandProcess report() {
		return ErrandProcess.create()
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessStatus(RUNNING);
	}

	@Test
	void reportProcessCreatingTheRowAnswersWithItsLocation() {
		when(serviceMock.reportProcess(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(PROCESS_INSTANCE_ID), any(ErrandProcess.class)))
			.thenReturn(new ErrandProcessResult(report().withId("rowId").withProcessInstanceId(PROCESS_INSTANCE_ID), true));

		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report())
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ErrandProcess.class)
			.returnResult();

		assertThat(response.getResponseHeaders().getLocation()).isNotNull();
		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/processes/" + PROCESS_INSTANCE_ID);
		assertThat(response.getResponseBody().getId()).isEqualTo("rowId");
		verify(serviceMock).reportProcess(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq(PROCESS_INSTANCE_ID), any(ErrandProcess.class));
	}

	@Test
	void reportProcessUpdatingTheRowAnswersOk() {
		when(serviceMock.reportProcess(any(), any(), any(), any(), any()))
			.thenReturn(new ErrandProcessResult(report().withId("rowId").withProcessInstanceId(PROCESS_INSTANCE_ID), false));

		final var response = webTestClient.put()
			.uri(builder -> builder.path(PROCESS_PATH).build(instanceVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report())
			.exchange()
			.expectStatus().isOk()
			.expectBody(ErrandProcess.class)
			.returnResult();

		assertThat(response.getResponseHeaders().getLocation()).isNull();
		assertThat(response.getResponseBody().getId()).isEqualTo("rowId");
	}

	@Test
	void registerProcessAnswersWithTheLocationOfTheInstance() {
		when(serviceMock.registerProcess(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(ErrandProcess.class)))
			.thenReturn(new ErrandProcessResult(report().withId("rowId").withProcessInstanceId(PROCESS_INSTANCE_ID), true));

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PROCESSES_PATH).build(errandVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report().withProcessInstanceId(PROCESS_INSTANCE_ID))
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ErrandProcess.class)
			.returnResult();

		assertThat(response.getResponseHeaders().getLocation().getPath())
			.isEqualTo("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/processes/" + PROCESS_INSTANCE_ID);
	}

	/**
	 * A start that failed produced no instance, so there is no subresource to point at - and building one anyway would
	 * hand out a link ending in the word null.
	 */
	@Test
	void registeringAStartThatFailedAnswersCreatedWithoutALocation() {
		when(serviceMock.registerProcess(any(), any(), any(), any()))
			.thenReturn(new ErrandProcessResult(report().withProcessStatus(FAILED).withProcessInstanceId(null), true));

		final var response = webTestClient.post()
			.uri(builder -> builder.path(PROCESSES_PATH).build(errandVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report().withProcessStatus(FAILED))
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ErrandProcess.class)
			.returnResult();

		assertThat(response.getResponseHeaders().getLocation()).isNull();
	}

	@Test
	void registerProcessOfAnInstanceAlreadyKnownAnswersOk() {
		when(serviceMock.registerProcess(any(), any(), any(), any()))
			.thenReturn(new ErrandProcessResult(report().withProcessInstanceId(PROCESS_INSTANCE_ID), false));

		webTestClient.post()
			.uri(builder -> builder.path(PROCESSES_PATH).build(errandVariables()))
			.contentType(APPLICATION_JSON)
			.bodyValue(report().withProcessInstanceId(PROCESS_INSTANCE_ID))
			.exchange()
			.expectStatus().isOk();
	}

	@Test
	void readErrandProcesses() {
		when(serviceMock.readProcesses(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(ErrandProcesses.create().withProcesses(List.of(report().withProcessInstanceId(PROCESS_INSTANCE_ID))));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PROCESSES_PATH).build(errandVariables()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ErrandProcesses.class)
			.returnResult();

		assertThat(response.getResponseBody().getProcesses()).hasSize(1);
		assertThat(response.getResponseBody().getStartable()).isNull();
		verify(serviceMock).readProcesses(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);
	}

	@Test
	void readErrandProcessActivitiesDefaultsToFiftyNewestFirst() {
		final var pageable = PageRequest.of(0, 50, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "occurredAt"));
		when(serviceMock.readProcessActivities(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), isNull(), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(ProcessActivity.create().withId("a").withOccurredAt(now(systemDefault()))), pageable, 1));

		webTestClient.get()
			.uri(builder -> builder.path(ACTIVITIES_PATH).build(errandVariables()))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).readProcessActivities(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(ERRAND_ID), isNull(), eq(pageable));
	}

	@Test
	void readErrandProcessActivitiesPassesTheInstanceFilterOn() {
		when(serviceMock.readProcessActivities(any(), any(), any(), eq(PROCESS_INSTANCE_ID), any()))
			.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

		webTestClient.get()
			.uri(builder -> builder.path(ACTIVITIES_PATH).queryParam("processInstanceId", PROCESS_INSTANCE_ID).build(errandVariables()))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).readProcessActivities(any(), any(), any(), eq(PROCESS_INSTANCE_ID), any());
	}
}
