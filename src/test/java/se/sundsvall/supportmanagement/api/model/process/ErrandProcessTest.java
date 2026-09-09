package se.sundsvall.supportmanagement.api.model.process;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;

class ErrandProcessTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void bean() {
		MatcherAssert.assertThat(ErrandProcess.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		final var started = now();
		final var ended = now().plusHours(1);
		final var created = now().plusMinutes(1);
		final var modified = now().plusMinutes(2);
		final var error = ProcessError.create().withCode("INCIDENT");
		final var activity = ProcessActivity.create().withActivityId("review_phase");

		final var process = ErrandProcess.create()
			.withId("id")
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessInstanceId("processInstanceId")
			.withProcessStatus(RUNNING)
			.withCurrentActivityId("investigation_phase")
			.withCurrentActivityName("Utredning")
			.withExternalTaskId("externalTaskId")
			.withErrandVersion(7L)
			.withStarted(started)
			.withEnded(ended)
			.withError(error)
			.withActivities(List.of(activity))
			.withCreated(created)
			.withModified(modified);

		assertThat(process.getId()).isEqualTo("id");
		assertThat(process.getProcessService()).isEqualTo("pw-alkt");
		assertThat(process.getProcessKey()).isEqualTo("alkt-ansokan");
		assertThat(process.getProcessInstanceId()).isEqualTo("processInstanceId");
		assertThat(process.getProcessStatus()).isEqualTo(RUNNING.name());
		assertThat(process.getCurrentActivityId()).isEqualTo("investigation_phase");
		assertThat(process.getCurrentActivityName()).isEqualTo("Utredning");
		assertThat(process.getExternalTaskId()).isEqualTo("externalTaskId");
		assertThat(process.getErrandVersion()).isEqualTo(7L);
		assertThat(process.getStarted()).isEqualTo(started);
		assertThat(process.getEnded()).isEqualTo(ended);
		assertThat(process.getError()).isEqualTo(error);
		assertThat(process.getActivities()).containsExactly(activity);
		assertThat(process.getCreated()).isEqualTo(created);
		assertThat(process.getModified()).isEqualTo(modified);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ErrandProcess.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandProcess()).hasAllNullFieldsOrProperties();
	}

	/**
	 * The model is written and read through the same class, and what keeps the three report only fields out of a read is
	 * that nothing sets them there and that nulls are left out of the serialised form. Both halves are asserted here,
	 * since either one alone would let the external task of a work step leak into the errand of a handler.
	 */
	@Test
	void aReadProjectionSerialisesWithoutTheWriteOnlyFields() {
		final var json = OBJECT_MAPPER.writeValueAsString(ErrandProcess.create()
			.withId("id")
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessInstanceId("processInstanceId")
			.withProcessStatus(RUNNING));

		assertThat(json)
			.doesNotContain("externalTaskId")
			.doesNotContain("errandVersion")
			.doesNotContain("activities")
			.doesNotContain("ended")
			.doesNotContain("error")
			.contains("\"processStatus\":\"RUNNING\"");
	}
}
