package se.sundsvall.supportmanagement.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.api.model.process.ErrandProcessReport;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockingDetails;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;

/**
 * The recovery from a lost race for the unique keys of the process table, with the database raising the violation.
 * <p>
 * The race is staged by letting one read of the process rows of the errand miss the live instance already written, as
 * the read of a concurrent writer would. The write then runs into {@code uq_ep_one_active_per_errand}, and the attempt
 * made after it reads the live instance and refuses the report.
 * <p>
 * Runs without a test transaction, since the recovery depends on the failed attempt rolling back a transaction of its
 * own.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@ExtendWith(OutputCaptureExtension.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-process-event.sql"
})
class ErrandProcessCollisionTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String ERRAND_ID = "ec677eb3-604c-4935-bff7-f8f0b500c8f4";
	private static final String PROCESS_SERVICE = "pw-alkt";
	private static final String PROCESS_KEY = "alkt-ansokan";
	private static final String LIVE_INSTANCE_ID = "pi-live";
	private static final String RACING_INSTANCE_ID = "pi-racing";

	@MockitoSpyBean
	private ErrandProcessRepository processRepositorySpy;

	@Autowired
	private ErrandProcessService errandProcessService;

	@BeforeEach
	void identifyAsTheProcessEngine() {
		Identifier.set(Identifier.create().withType(Identifier.Type.CUSTOM).withTypeString("processEngine").withValue(PROCESS_SERVICE));
	}

	@AfterEach
	void clearIdentifier() {
		Identifier.remove();
	}

	@Test
	@DisplayName("Verification that a second live instance written past a stale read is refused with 409 once the database rejects it")
	void aSecondLiveInstanceRejectedByTheDatabaseIsRefused(final CapturedOutput output) {
		errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, LIVE_INSTANCE_ID, report());
		final var theDatabase = mockingDetails(processRepositorySpy).getMockCreationSettings().getDefaultAnswer();
		doReturn(List.of()).doAnswer(theDatabase).when(processRepositorySpy).findByErrandIdOrderByCreatedDesc(ERRAND_ID);

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> errandProcessService.reportProcess(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, RACING_INSTANCE_ID, report()))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).contains(LIVE_INSTANCE_ID);
			});

		assertThat(output.getAll()).contains("Retrying the write for process instance '" + RACING_INSTANCE_ID + "' on errand '" + ERRAND_ID + "' after an integrity violation");
		assertThat(processRepositorySpy.findByErrandIdOrderByCreatedDesc(ERRAND_ID))
			.extracting(ErrandProcessEntity::getProcessInstanceId)
			.containsExactly(LIVE_INSTANCE_ID);
	}

	private static ErrandProcessReport report() {
		return ErrandProcessReport.create()
			.withProcessService(PROCESS_SERVICE)
			.withProcessKey(PROCESS_KEY)
			.withProcessStatus(RUNNING);
	}
}
