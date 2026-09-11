package se.sundsvall.supportmanagement.service;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.StatementOutcome;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatementValidatorTest {

	private final StatementValidator validator = new StatementValidator();

	@Test
	void draftNeedsNothing() {
		assertThatCode(() -> validator.validate(StatementEntity.create().withStatus(ItemStatus.DRAFT))).doesNotThrowAnyException();
	}

	@Test
	void cancelledNeedsNothing() {
		assertThatCode(() -> validator.validate(StatementEntity.create().withStatus(ItemStatus.CANCELLED))).doesNotThrowAnyException();
	}

	@Test
	void activeRequiresSentAt() {

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(StatementEntity.create().withStatus(ItemStatus.ACTIVE)))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("A statement cannot be ACTIVE without sentAt being set");
	}

	@Test
	void activeWithSentAtIsAccepted() {
		assertThatCode(() -> validator.validate(StatementEntity.create().withStatus(ItemStatus.ACTIVE).withSentAt(now())))
			.doesNotThrowAnyException();
	}

	@Test
	void completedRequiresOutcome() {

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(StatementEntity.create().withStatus(ItemStatus.COMPLETED)))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("A statement cannot be COMPLETED without an outcome");
	}

	@Test
	void completedRequiresRespondedAt() {

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(StatementEntity.create()
			.withStatus(ItemStatus.COMPLETED)
			.withOutcome(StatementOutcome.SUPPORTS)))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("A statement with outcome 'SUPPORTS' cannot be COMPLETED without respondedAt being set");
	}

	@Test
	void completedWithOutcomeAndRespondedAtIsAccepted() {
		assertThatCode(() -> validator.validate(StatementEntity.create()
			.withStatus(ItemStatus.COMPLETED)
			.withOutcome(StatementOutcome.SUPPORTS)
			.withRespondedAt(now()))).doesNotThrowAnyException();
	}

	/**
	 * A statement closed because the deadline passed has no response, so demanding a timestamp for one that never
	 * arrived would leave no way to close it.
	 */
	@Test
	void noResponseIsCompletedWithoutRespondedAt() {
		assertThatCode(() -> validator.validate(StatementEntity.create()
			.withStatus(ItemStatus.COMPLETED)
			.withOutcome(StatementOutcome.NO_RESPONSE))).doesNotThrowAnyException();
	}

	@Test
	void theProblemIsABadRequest() {
		assertThatThrownBy(() -> validator.validate(StatementEntity.create().withStatus(ItemStatus.ACTIVE)))
			.hasMessageStartingWith("Bad Request:");
	}
}
