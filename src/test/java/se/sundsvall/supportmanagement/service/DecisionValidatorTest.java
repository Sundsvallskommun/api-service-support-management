package se.sundsvall.supportmanagement.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.DecisionOutcomeRepository;
import se.sundsvall.supportmanagement.integration.db.DecisionRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.DecisionEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigValueEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.enums.ItemStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;
import se.sundsvall.supportmanagement.integration.db.model.enums.ValueType;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.AUTOMATIC;
import static se.sundsvall.supportmanagement.integration.db.model.enums.DecisionMethod.MANUAL;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_SINGLE_DECISION_PER_ERRAND;

@ExtendWith(MockitoExtension.class)
class DecisionValidatorTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String DECISION_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String ATTACHMENT_ID = "8f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String INVESTIGATION_ID = "9f79a808-0ef3-4985-99b9-b12f23e202a7";
	private static final String PROCESS_CONSUMER = "pw-alkt";

	@Mock
	private DecisionRepository decisionRepositoryMock;

	@Mock
	private DecisionOutcomeRepository decisionOutcomeRepositoryMock;

	@Mock
	private NamespaceConfigRepository namespaceConfigRepositoryMock;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@Mock
	private ErrandProcessRepository processRepositoryMock;

	@InjectMocks
	private DecisionValidator validator;

	/**
	 * The identifier is bound to the thread, which the test classes run before this one share.
	 */
	@BeforeEach
	@AfterEach
	void clearIdentifier() {
		Identifier.remove();
	}

	private static NamespaceConfigEntity configWithSingleDecision(final boolean value) {
		return NamespaceConfigEntity.create()
			.withValues(List.of(NamespaceConfigValueEmbeddable.create()
				.withKey(PROPERTY_SINGLE_DECISION_PER_ERRAND)
				.withType(ValueType.BOOLEAN)
				.withValue(String.valueOf(value))));
	}

	/**
	 * A namespace that has not asked for the restriction is not restricted, and the errand is then not even asked about.
	 */
	@Test
	void cardinalityIsUncheckedWhenTheNamespaceHasNoConfiguration() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
		verifyNoInteractions(decisionRepositoryMock);
	}

	@Test
	void cardinalityIsUncheckedWhenTheNamespaceHasNotAskedForIt() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(configWithSingleDecision(false)));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
		verifyNoInteractions(decisionRepositoryMock);
	}

	/**
	 * A namespace configured before the setting existed reads as unrestricted rather than failing the request.
	 */
	@Test
	void cardinalityIsUncheckedWhenTheSettingIsAbsentFromTheConfiguration() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(NamespaceConfigEntity.create()));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
		verifyNoInteractions(decisionRepositoryMock);
	}

	@Test
	void theFirstDecisionIsAllowedEvenWhenTheNamespaceAllowsOnlyOne() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(configWithSingleDecision(true)));
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(false);

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));
	}

	@Test
	void aSecondDecisionIsAConflictWhenTheNamespaceAllowsOnlyOne() {

		// Arrange
		when(namespaceConfigRepositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(configWithSingleDecision(true)));
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(true);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateCardinality(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	/**
	 * A patch that says nothing about the method leaves the stored one standing, so there is nothing to check.
	 */
	@Test
	void aMissingMethodIsLeftAlone() {

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, null));
		verifyNoInteractions(namespaceConfigServiceMock);
	}

	@Test
	void anAdAccountMayWriteAManualDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, MANUAL));
		verifyNoInteractions(namespaceConfigServiceMock);
	}

	/**
	 * The consumer is recognised by the value of its identifier, whatever type the header gives it.
	 */
	@Test
	void theProcessConsumerMayWriteAnAutomaticDecision() {

		// Arrange
		Identifier.set(Identifier.parse(PROCESS_CONSUMER + "; type=processEngine"));
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROCESS_CONSUMER));

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC));
	}

	/**
	 * The difference between a decision a person made and one a process made has to be answerable afterwards, which is why
	 * neither side may claim the other.
	 */
	@Test
	void theProcessConsumerCannotWriteAManualDecision() {

		// Arrange
		Identifier.set(Identifier.parse(PROCESS_CONSUMER + "; type=processEngine"));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, MANUAL));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(problem.getMessage()).contains("MANUAL");
	}

	@Test
	void aManualDecisionWithoutAnIdentifierAtAllIsRejected() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, MANUAL));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
	}

	@Test
	void anAdAccountCannotWriteAnAutomaticDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROCESS_CONSUMER));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(problem.getMessage()).contains("AUTOMATIC", PROCESS_CONSUMER);
	}

	/**
	 * An ad account is refused even when its name happens to be that of the consumer - a person never writes an automatic
	 * decision.
	 */
	@Test
	void anAdAccountNamedLikeTheProcessConsumerCannotWriteAnAutomaticDecision() {

		// Arrange
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(PROCESS_CONSUMER));
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROCESS_CONSUMER));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"other-engine; type=processEngine", "81471222-5798-11e9-ae24-57fa13b361e1; type=partyId", "PW-ALKT; type=processEngine"
	})
	void anotherConsumerCannotWriteAnAutomaticDecision(final String sentBy) {

		// Arrange
		Identifier.set(Identifier.parse(sentBy));
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROCESS_CONSUMER));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(problem.getMessage()).contains("AUTOMATIC", PROCESS_CONSUMER, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void anAutomaticDecisionWithoutAnIdentifierAtAllIsRejected() {

		// Arrange
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(PROCESS_CONSUMER));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
	}

	/**
	 * A namespace that runs no process has nobody who can write an automatic decision.
	 */
	@Test
	void anAutomaticDecisionInANamespaceWithoutAProcessConsumerIsRejected() {

		// Arrange
		Identifier.set(Identifier.parse(PROCESS_CONSUMER + "; type=processEngine"));
		when(namespaceConfigServiceMock.getProcessConsumer(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateMethod(NAMESPACE, MUNICIPALITY_ID, AUTOMATIC));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(problem.getMessage()).contains("AUTOMATIC", NAMESPACE, MUNICIPALITY_ID);
	}

	/**
	 * A patch that says nothing about the outcome leaves the stored one standing, so there is nothing to ask.
	 */
	@Test
	void aMissingOutcomeIsLeftAlone() {

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateOutcome(NAMESPACE, MUNICIPALITY_ID, null));
		verifyNoInteractions(decisionOutcomeRepositoryMock);
	}

	@Test
	void anOutcomeTheNamespaceHasRegisteredIsAccepted() {

		// Arrange
		when(decisionOutcomeRepositoryMock.existsByNamespaceAndMunicipalityIdAndName(NAMESPACE, MUNICIPALITY_ID, "APPROVAL")).thenReturn(true);

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateOutcome(NAMESPACE, MUNICIPALITY_ID, "APPROVAL"));
	}

	@Test
	void anOutcomeTheNamespaceHasNotRegisteredIsABadRequest() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateOutcome(NAMESPACE, MUNICIPALITY_ID, "UNKNOWN"));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getMessage()).contains("UNKNOWN", NAMESPACE, MUNICIPALITY_ID);
	}

	/**
	 * An errand without a process is never locked - not even a completed decision on it - since there is no process for
	 * the decision to have been handed on to. The revision history is what makes its changes traceable.
	 */
	@ParameterizedTest
	@EnumSource(ItemStatus.class)
	void aDecisionOnAnErrandWithoutAProcessIsAlwaysChangeable(final ItemStatus status) {

		// Arrange
		givenProcesses();

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateChangeable(ERRAND_ID, decision(status)));
		assertThatNoException().isThrownBy(() -> validator.validateChangeable(ERRAND_ID, null));
	}

	/**
	 * While the process lives, the decision can be written again - the step preparing it may have to correct itself, and a
	 * caseworker may find a typing error. A failed process leaves the process life open, since it is recovered from.
	 */
	@ParameterizedTest
	@EnumSource(value = ItemStatus.class, names = "COMPLETED", mode = EXCLUDE)
	void aDecisionNotYetCompletedIsChangeableWhileTheProcessLifeIsOpen(final ItemStatus status) {

		// Arrange
		givenProcesses(RUNNING, FAILED);

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateChangeable(ERRAND_ID, decision(status)));
		assertThatNoException().isThrownBy(() -> validator.validateChangeable(ERRAND_ID, null));
	}

	@Test
	void aCompletedDecisionOnAnErrandWithAProcessIsLocked() {

		// Arrange
		givenProcesses(WAITING);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateChangeable(ERRAND_ID, decision(ItemStatus.COMPLETED)));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(DECISION_ID);
	}

	/**
	 * The same completed process that is never started again. It locks every decision of the errand, and a new one too.
	 */
	@ParameterizedTest
	@EnumSource(ItemStatus.class)
	void everyDecisionIsLockedOnceTheProcessHasRunToItsEnd(final ItemStatus status) {

		// Arrange
		givenProcesses(FAILED, COMPLETED);

		// Act
		final var existing = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateChangeable(ERRAND_ID, decision(status)));
		final var created = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateChangeable(ERRAND_ID, null));

		// Verify
		assertThat(existing.getStatus()).isEqualTo(CONFLICT);
		assertThat(existing.getMessage()).contains(ERRAND_ID);
		assertThat(created.getStatus()).isEqualTo(CONFLICT);
	}

	/**
	 * Most attachments belong to no decision, and those are not held up by a single further question.
	 */
	@Test
	void anAttachmentNoDecisionHasLinkedIsRemovable() {

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateAttachmentRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));
		verify(decisionRepositoryMock).existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID);
		verifyNoMoreInteractions(decisionRepositoryMock);
		verifyNoInteractions(processRepositoryMock);
	}

	@Test
	void anAttachmentOfADecisionOnAnErrandWithoutAProcessIsRemovable() {

		// Arrange
		givenAttachmentLinked();
		givenProcesses();

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateAttachmentRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));
	}

	@Test
	void anAttachmentOfADecisionNotYetCompletedIsRemovableWhileTheProcessLives() {

		// Arrange
		givenAttachmentLinked();
		givenProcesses(RUNNING);

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateAttachmentRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));
		verify(decisionRepositoryMock).existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, ItemStatus.COMPLETED);
	}

	@Test
	void anAttachmentOfACompletedDecisionOnAnErrandWithAProcessIsNotRemovable() {

		// Arrange
		givenAttachmentLinked();
		givenProcesses(WAITING);
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, ItemStatus.COMPLETED))
			.thenReturn(true);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateAttachmentRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(ATTACHMENT_ID, ERRAND_ID);
	}

	@Test
	void anAttachmentOfAnyDecisionIsNotRemovableOnceTheProcessHasRunToItsEnd() {

		// Arrange
		givenAttachmentLinked();
		givenProcesses(COMPLETED);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateAttachmentRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
	}

	/**
	 * The database would set the reference of the decision to null, which is a change to it.
	 */
	@Test
	void anInvestigationNoDecisionRestsOnIsRemovable() {

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateInvestigationRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID));
		verify(decisionRepositoryMock).existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID);
		verifyNoMoreInteractions(decisionRepositoryMock);
		verifyNoInteractions(processRepositoryMock);
	}

	@Test
	void anInvestigationADecisionRestsOnIsRemovableOnAnErrandWithoutAProcess() {

		// Arrange
		givenInvestigationRestedOn();
		givenProcesses();

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateInvestigationRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID));
	}

	@Test
	void anInvestigationADecisionNotYetCompletedRestsOnIsRemovableWhileTheProcessLives() {

		// Arrange
		givenInvestigationRestedOn();
		givenProcesses(RUNNING);

		// Act & Verify
		assertThatNoException().isThrownBy(() -> validator.validateInvestigationRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID));
		verify(decisionRepositoryMock).existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID,
			ItemStatus.COMPLETED);
	}

	@Test
	void anInvestigationACompletedDecisionRestsOnIsNotRemovableOnAnErrandWithAProcess() {

		// Arrange
		givenInvestigationRestedOn();
		givenProcesses(WAITING);
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityIdAndStatus(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID,
			ItemStatus.COMPLETED)).thenReturn(true);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateInvestigationRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(problem.getMessage()).contains(INVESTIGATION_ID, ERRAND_ID);
	}

	@Test
	void anInvestigationAnyDecisionRestsOnIsNotRemovableOnceTheProcessHasRunToItsEnd() {

		// Arrange
		givenInvestigationRestedOn();
		givenProcesses(COMPLETED);

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> validator.validateInvestigationRemovable(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
	}

	private void givenInvestigationRestedOn() {
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndInvestigationEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, INVESTIGATION_ID)).thenReturn(true);
	}

	private void givenProcesses(final ProcessStatus... statuses) {
		when(processRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(Stream.of(statuses)
			.map(status -> {
				final var instance = ErrandProcessEntity.create().withErrandId(ERRAND_ID);
				instance.applyStatus(status, Clock.systemUTC());
				return instance;
			})
			.toList());
	}

	private void givenAttachmentLinked() {
		when(decisionRepositoryMock.existsByNamespaceAndMunicipalityIdAndErrandEntityIdAndAttachmentsId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID)).thenReturn(true);
	}

	private static DecisionEntity decision(final ItemStatus status) {
		return DecisionEntity.create().withId(DECISION_ID).withStatus(status);
	}
}
