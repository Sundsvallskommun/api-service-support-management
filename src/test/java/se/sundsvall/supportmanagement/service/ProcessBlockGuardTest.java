package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.CUSTOM;
import static se.sundsvall.supportmanagement.service.ProcessActivityLog.CONFIG_ACTIVITY_TYPE;
import static se.sundsvall.supportmanagement.service.ProcessBlockGuard.REMOVES_BLOCK_ERROR_CODE;

@ExtendWith(MockitoExtension.class)
class ProcessBlockGuardTest {

	private static final String ERRAND_ID = "errandId";
	private static final String BLOCKING = "blocking-id";
	private static final String OTHER = "other-id";
	private static final String CONSEQUENCE = "The labels were not rebuilt.";
	private static final String REASON = """
		The change would take the label 'blocking-id' off errand 'errandId', and it carries processBlocked=true, which keeps \
		every process away from the errand. An ad account cannot take that label off; a service identity can.""";

	@Mock
	private ProcessKeySelector processKeySelectorMock;

	@Mock
	private ProcessActivityLog activityLogMock;

	@InjectMocks
	private ProcessBlockGuard guard;

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	@DisplayName("Verification that an ad account taking a label that blocks processes off an errand is refused with 409, naming the label")
	void anAdAccountTakingABlockOffIsRefused() {
		asCaller(AD_ACCOUNT);
		when(processKeySelectorMock.blockingLabelIdsOf(List.of(label(BLOCKING)))).thenReturn(Set.of(BLOCKING));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, List.of(label(BLOCKING), label(OTHER)), List.of(label(OTHER))))
			.satisfies(problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(409);
				assertThat(problem.getDetail()).isEqualTo(REASON);
			});
	}

	@Test
	@DisplayName("Verification that an ad account taking off only labels that do not block processes passes")
	void anAdAccountTakingOffAnotherLabelPasses() {
		asCaller(AD_ACCOUNT);
		when(processKeySelectorMock.blockingLabelIdsOf(List.of(label(OTHER)))).thenReturn(Set.of());

		assertThatNoException().isThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, List.of(label(BLOCKING), label(OTHER)), List.of(label(BLOCKING))));
	}

	@Test
	@DisplayName("Verification that a change taking no label off passes without the labels being read for a block")
	void aChangeTakingNoLabelOffReadsNothing() {
		asCaller(AD_ACCOUNT);

		assertThatNoException().isThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, List.of(label(BLOCKING)), List.of(label(BLOCKING), label(OTHER))));

		verifyNoInteractions(processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that a service identity, or a caller without an identity, may take a label that blocks processes off an errand, and that the labels are not read for it")
	void anyoneButAnAdAccountMayTakeABlockOff() {
		asCaller(CUSTOM);
		assertThatNoException().isThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, List.of(label(BLOCKING)), List.of()));

		Identifier.remove();
		assertThatNoException().isThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, List.of(label(BLOCKING)), List.of()));

		verifyNoInteractions(processKeySelectorMock);
	}

	@Test
	@DisplayName("Verification that a job started by an ad account taking a label that blocks processes off an errand is refused, and that the refusal is written on the errand")
	void aJobStartedByAnAdAccountTakingABlockOffIsRefused() {
		when(processKeySelectorMock.blockingLabelIdsOf(List.of(label(BLOCKING)))).thenReturn(Set.of(BLOCKING));

		assertThat(guard.refusesLabelChange(ERRAND_ID, List.of(label(BLOCKING)), List.of(label(OTHER)), true, CONSEQUENCE)).isTrue();

		verify(activityLogMock).writeOncePerWindow(ERRAND_ID, null, CONFIG_ACTIVITY_TYPE, REMOVES_BLOCK_ERROR_CODE, REASON + " " + CONSEQUENCE);
	}

	@Test
	@DisplayName("Verification that a job started by an ad account taking off only labels that do not block processes is not refused, and writes nothing")
	void aJobStartedByAnAdAccountTakingOffAnotherLabelPasses() {
		when(processKeySelectorMock.blockingLabelIdsOf(List.of(label(OTHER)))).thenReturn(Set.of());

		assertThat(guard.refusesLabelChange(ERRAND_ID, List.of(label(BLOCKING), label(OTHER)), List.of(label(BLOCKING)), true, CONSEQUENCE)).isFalse();

		verifyNoInteractions(activityLogMock);
	}

	@Test
	@DisplayName("Verification that a job started by a service identity may take a label that blocks processes off an errand, without the labels being read for it")
	void aJobStartedByAServiceMayTakeABlockOff() {
		asCaller(AD_ACCOUNT);

		assertThat(guard.refusesLabelChange(ERRAND_ID, List.of(label(BLOCKING)), List.of(), false, CONSEQUENCE)).isFalse();

		verifyNoInteractions(processKeySelectorMock, activityLogMock);
	}

	@Test
	@DisplayName("Verification that the labels taken off are named in a stable order when several block processes")
	void severalBlocksTakenOffAreNamedInOrder() {
		asCaller(AD_ACCOUNT);
		when(processKeySelectorMock.blockingLabelIdsOf(any())).thenReturn(Set.of("b-block", "a-block"));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> guard.verifyLabelChange(ERRAND_ID, List.of(label("a-block"), label("b-block")), List.of()))
			.satisfies(problem -> assertThat(problem.getDetail()).startsWith("The change would take the label 'a-block', 'b-block' off errand"));
	}

	private static void asCaller(final Identifier.Type type) {
		Identifier.set(Identifier.create().withType(type).withValue("joe01doe"));
	}

	private static ErrandLabelEmbeddable label(final String id) {
		return ErrandLabelEmbeddable.create().withMetadataLabelId(id);
	}
}
