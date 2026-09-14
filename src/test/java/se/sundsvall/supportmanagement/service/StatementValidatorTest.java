package se.sundsvall.supportmanagement.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.StatementOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.model.StatementEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementOutcomeEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class StatementValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private StatementOutcomeRepository statementOutcomeRepositoryMock;

	@InjectMocks
	private StatementValidator validator;

	private static StatementEntity statement(final ItemStatus status) {
		return StatementEntity.create().withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID).withStatus(status);
	}

	private void mockOutcome(final String name, final boolean responded) {
		when(statementOutcomeRepositoryMock.findByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, name))
			.thenReturn(Optional.of(StatementOutcomeEntity.create().withName(name).withResponded(responded)));
	}

	@Test
	void draftNeedsNothing() {
		assertThatCode(() -> validator.validate(statement(ItemStatus.DRAFT), true)).doesNotThrowAnyException();
	}

	@Test
	void cancelledNeedsNothing() {
		assertThatCode(() -> validator.validate(statement(ItemStatus.CANCELLED), true)).doesNotThrowAnyException();
	}

	@Test
	void activeRequiresSentAt() {

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(statement(ItemStatus.ACTIVE), true))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("A statement cannot be ACTIVE without sentAt being set");
	}

	@Test
	void activeWithSentAtIsAccepted() {
		assertThatCode(() -> validator.validate(statement(ItemStatus.ACTIVE).withSentAt(now()), true))
			.doesNotThrowAnyException();
	}

	@Test
	void completedRequiresOutcome() {

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(statement(ItemStatus.COMPLETED), true))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("A statement cannot be COMPLETED without an outcome");
	}

	@Test
	void completedRequiresRespondedAt() {

		// Arrange
		mockOutcome("SUPPORTS", true);

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(statement(ItemStatus.COMPLETED).withOutcome("SUPPORTS"), true))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("A statement with outcome 'SUPPORTS' cannot be COMPLETED without respondedAt being set");
	}

	/**
	 * With the response on record, there is nothing to ask the namespace about the outcome.
	 */
	@Test
	void completedWithOutcomeAndRespondedAtIsAccepted() {
		assertThatCode(() -> validator.validate(statement(ItemStatus.COMPLETED)
			.withOutcome("SUPPORTS")
			.withRespondedAt(now()), true)).doesNotThrowAnyException();
		verifyNoInteractions(statementOutcomeRepositoryMock);
	}

	/**
	 * A statement closed because the deadline passed has no response, so demanding a timestamp for one that never
	 * arrived would leave no way to close it. Which outcome says so is for the namespace to register.
	 */
	@Test
	void anOutcomeWithoutAResponseIsCompletedWithoutRespondedAt() {

		// Arrange
		mockOutcome("NO_RESPONSE", false);

		// Act & Assert
		assertThatCode(() -> validator.validate(statement(ItemStatus.COMPLETED).withOutcome("NO_RESPONSE"), true)).doesNotThrowAnyException();
	}

	/**
	 * An outcome the namespace has since removed cannot say whether it means a response, and does not hold the statement
	 * to one.
	 */
	@Test
	void anOutcomeTheNamespaceNoLongerHoldsDoesNotRequireRespondedAt() {

		// Arrange
		when(statementOutcomeRepositoryMock.findByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "REMOVED")).thenReturn(Optional.empty());

		// Act & Assert
		assertThatCode(() -> validator.validate(statement(ItemStatus.COMPLETED).withOutcome("REMOVED"), true)).doesNotThrowAnyException();
	}

	/**
	 * How the namespace registers an outcome can change after a statement was completed with it. A request that sets
	 * neither the status nor the outcome is not held to the registration as it stands now, or the statement could no
	 * longer be changed at all.
	 */
	@Test
	void aCompletedStatementIsNotHeldToALaterRegistrationOfItsOutcome() {
		assertThatCode(() -> validator.validate(statement(ItemStatus.COMPLETED).withOutcome("NO_RESPONSE"), false)).doesNotThrowAnyException();
		verifyNoInteractions(statementOutcomeRepositoryMock);
	}

	/**
	 * Only an answered statement has an outcome: one drafted, sent or withdrawn has not been answered yet, and one that
	 * has been answered cannot be moved back out of COMPLETED with its outcome still on it.
	 */
	@ParameterizedTest
	@EnumSource(value = ItemStatus.class, names = "COMPLETED", mode = EXCLUDE)
	void anOutcomeRequiresCompleted(final ItemStatus status) {

		// Act & Assert
		assertThatThrownBy(() -> validator.validate(statement(status)
			.withSentAt(now())
			.withOutcome("SUPPORTS"), true))
			.isInstanceOf(Problem.class)
			.hasMessageContaining("A statement cannot have an outcome while it is " + status);
	}

	@Test
	void theProblemIsABadRequest() {
		assertThatThrownBy(() -> validator.validate(statement(ItemStatus.ACTIVE), true))
			.hasMessageStartingWith("Bad Request:");
	}

	/**
	 * A patch that says nothing about the outcome leaves the stored one standing, so there is nothing to ask.
	 */
	@Test
	void aMissingOutcomeIsLeftAlone() {
		assertThatCode(() -> validator.validateOutcome(NAMESPACE, MUNICIPALITY_ID, null)).doesNotThrowAnyException();
		verifyNoInteractions(statementOutcomeRepositoryMock);
	}

	@Test
	void anOutcomeTheNamespaceHasRegisteredIsAccepted() {

		// Arrange
		when(statementOutcomeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "SUPPORTS")).thenReturn(true);

		// Act & Assert
		assertThatCode(() -> validator.validateOutcome(NAMESPACE, MUNICIPALITY_ID, "SUPPORTS")).doesNotThrowAnyException();
	}

	@Test
	void anOutcomeTheNamespaceHasNotRegisteredIsABadRequest() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateOutcome(NAMESPACE, MUNICIPALITY_ID, "UNKNOWN"));

		// Assert
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getMessage()).contains("UNKNOWN", NAMESPACE, MUNICIPALITY_ID);
	}
}
