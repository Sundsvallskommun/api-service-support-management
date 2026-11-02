package se.sundsvall.supportmanagement.api.model.process;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

class ErrandProcessReportTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void bean() {
		MatcherAssert.assertThat(ErrandProcessReport.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {
		final var started = now();
		final var error = ProcessError.create().withCode("INCIDENT");
		final var activity = ProcessActivity.create().withActivityId("review_phase");
		final var signal = ProcessSignal.create().withName("granskning-godkand");

		final var report = ErrandProcessReport.create()
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessInstanceId("processInstanceId")
			.withProcessStatus(WAITING)
			.withCurrentActivityId("review_phase")
			.withCurrentActivityName("Granskning")
			.withExternalTaskId("externalTaskId")
			.withErrandVersion(7L)
			.withStarted(started)
			.withError(error)
			.withActivities(List.of(activity))
			.withAwaitingSignals(List.of(signal));

		assertThat(report.getProcessService()).isEqualTo("pw-alkt");
		assertThat(report.getProcessKey()).isEqualTo("alkt-ansokan");
		assertThat(report.getProcessInstanceId()).isEqualTo("processInstanceId");
		assertThat(report.getProcessStatus()).isEqualTo(WAITING.name());
		assertThat(report.getCurrentActivityId()).isEqualTo("review_phase");
		assertThat(report.getCurrentActivityName()).isEqualTo("Granskning");
		assertThat(report.getExternalTaskId()).isEqualTo("externalTaskId");
		assertThat(report.getErrandVersion()).isEqualTo(7L);
		assertThat(report.getStarted()).isEqualTo(started);
		assertThat(report.getError()).isEqualTo(error);
		assertThat(report.getActivities()).containsExactly(activity);
		assertThat(report.getAwaitingSignals()).containsExactly(signal);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ErrandProcessReport.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandProcessReport()).hasAllNullFieldsOrProperties();
	}
}
