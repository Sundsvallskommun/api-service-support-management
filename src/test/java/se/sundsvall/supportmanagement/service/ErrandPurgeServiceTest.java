package se.sundsvall.supportmanagement.service;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.purge.ErrandPurgeRequest;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

/**
 * The purge machinery is not built yet, so what is worth pinning down is that every operation says so plainly instead
 * of
 * answering as though a run had been started.
 */
class ErrandPurgeServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String JOB_ID = randomUUID().toString();

	private final ErrandPurgeService service = new ErrandPurgeService();

	@Test
	void startPurgeIsNotImplemented() {
		final var request = ErrandPurgeRequest.create()
			.withOlderThan(OffsetDateTime.parse("2020-08-28T00:00:00+02:00"))
			.withDryRun(true);

		assertThatThrownBy(() -> service.startPurge(NAMESPACE, MUNICIPALITY_ID, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_IMPLEMENTED)
			.hasMessageContaining("Errand purge is not implemented yet");
	}

	@Test
	void readPurgeStatusIsNotImplemented() {
		assertThatThrownBy(() -> service.readPurgeStatus(NAMESPACE, MUNICIPALITY_ID, JOB_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_IMPLEMENTED)
			.hasMessageContaining("Errand purge is not implemented yet");
	}

	@Test
	void stopPurgeIsNotImplemented() {
		assertThatThrownBy(() -> service.stopPurge(NAMESPACE, MUNICIPALITY_ID, JOB_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_IMPLEMENTED)
			.hasMessageContaining("Errand purge is not implemented yet");
	}
}
