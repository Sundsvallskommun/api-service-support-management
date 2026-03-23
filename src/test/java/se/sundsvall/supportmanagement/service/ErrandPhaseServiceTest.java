package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.PhaseRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandPhaseEntity;
import se.sundsvall.supportmanagement.integration.db.model.PhaseEntity;
import se.sundsvall.supportmanagement.integration.db.model.PhaseTransitionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class ErrandPhaseServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private PhaseRepository phaseRepositoryMock;

	@InjectMocks
	private ErrandPhaseService service;

	@Test
	void processPhaseChange_nullPhaseId_doesNothing() {
		final var errandEntity = ErrandEntity.create();

		service.processPhaseChange(errandEntity, null, NAMESPACE, MUNICIPALITY_ID);

		verifyNoInteractions(phaseRepositoryMock);
	}

	@Test
	void processPhaseChange_firstPhaseAssignment_createsPhase() {
		final var phaseId = "phase-id-1";
		final var phaseEntity = PhaseEntity.create().withId(phaseId).withName("INVESTIGATION");
		final var errandEntity = ErrandEntity.create();

		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(phaseId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(phaseEntity));

		service.processPhaseChange(errandEntity, phaseId, NAMESPACE, MUNICIPALITY_ID);

		assertThat(errandEntity.getPhases()).hasSize(1);
		final var createdPhase = errandEntity.getPhases().getFirst();
		assertThat(createdPhase.getPhaseEntity()).isEqualTo(phaseEntity);
		assertThat(createdPhase.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(createdPhase.getStarted()).isNotNull();

		verify(phaseRepositoryMock).findByIdAndNamespaceAndMunicipalityId(phaseId, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void processPhaseChange_phaseNotFound_throwsException() {
		final var phaseId = "non-existent-phase";
		final var errandEntity = ErrandEntity.create();

		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(phaseId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.processPhaseChange(errandEntity, phaseId, NAMESPACE, MUNICIPALITY_ID))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains(phaseId);
			});
	}

	@Test
	void processPhaseChange_validTransition_endsCurrentAndCreatesNew() {
		final var newPhaseId = "phase-id-2";
		final var currentPhaseEntity = PhaseEntity.create()
			.withId("phase-id-1")
			.withName("INVESTIGATION")
			.withTransitions(List.of(PhaseTransitionEntity.create().withTargetPhaseId(newPhaseId)));
		final var newPhaseEntity = PhaseEntity.create().withId(newPhaseId).withName("DECISION");

		final var activeErrandPhase = ErrandPhaseEntity.create()
			.withPhaseEntity(currentPhaseEntity);

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(activeErrandPhase)));

		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(newPhaseId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newPhaseEntity));

		service.processPhaseChange(errandEntity, newPhaseId, NAMESPACE, MUNICIPALITY_ID);

		assertThat(activeErrandPhase.getEnded()).isNotNull();
		assertThat(errandEntity.getPhases()).hasSize(2);
		assertThat(errandEntity.getPhases().getLast().getPhaseEntity()).isEqualTo(newPhaseEntity);
	}

	@Test
	void processPhaseChange_invalidTransition_throwsException() {
		final var newPhaseId = "phase-id-2";
		final var currentPhaseEntity = PhaseEntity.create()
			.withId("phase-id-1")
			.withName("INVESTIGATION")
			.withTransitions(List.of(PhaseTransitionEntity.create().withTargetPhaseId("phase-id-3")));
		final var newPhaseEntity = PhaseEntity.create().withId(newPhaseId).withName("DECISION");

		final var activeErrandPhase = ErrandPhaseEntity.create()
			.withPhaseEntity(currentPhaseEntity);

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(activeErrandPhase)));

		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(newPhaseId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newPhaseEntity));

		assertThatThrownBy(() -> service.processPhaseChange(errandEntity, newPhaseId, NAMESPACE, MUNICIPALITY_ID))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("INVESTIGATION").contains("DECISION");
			});
	}

	@Test
	void processPhaseChange_noTransitionsConfigured_throwsException() {
		final var newPhaseId = "phase-id-2";
		final var currentPhaseEntity = PhaseEntity.create()
			.withId("phase-id-1")
			.withName("CLOSED")
			.withTransitions(List.of());
		final var newPhaseEntity = PhaseEntity.create().withId(newPhaseId).withName("INVESTIGATION");

		final var activeErrandPhase = ErrandPhaseEntity.create()
			.withPhaseEntity(currentPhaseEntity);

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(activeErrandPhase)));

		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(newPhaseId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(newPhaseEntity));

		assertThatThrownBy(() -> service.processPhaseChange(errandEntity, newPhaseId, NAMESPACE, MUNICIPALITY_ID))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("CLOSED").contains("no configured transitions");
			});
	}

	@Test
	void processPhaseChange_samePhaseAsActive_noop() {
		final var phaseId = "phase-id-1";
		final var phaseEntity = PhaseEntity.create().withId(phaseId).withName("INVESTIGATION");
		final var activeErrandPhase = ErrandPhaseEntity.create()
			.withPhaseEntity(phaseEntity);

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(activeErrandPhase)));

		when(phaseRepositoryMock.findByIdAndNamespaceAndMunicipalityId(phaseId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(phaseEntity));

		service.processPhaseChange(errandEntity, phaseId, NAMESPACE, MUNICIPALITY_ID);

		assertThat(activeErrandPhase.getEnded()).isNull();
		assertThat(errandEntity.getPhases()).hasSize(1);
	}

	@Test
	void validateStatus_noPhases_passes() {
		final var errandEntity = ErrandEntity.create();

		service.validateStatusAgainstActivePhase(errandEntity, "NEW");

		verifyNoInteractions(phaseRepositoryMock);
	}

	@Test
	void validateStatus_noActivePhase_passes() {
		final var endedPhase = ErrandPhaseEntity.create()
			.withPhaseEntity(PhaseEntity.create().withName("OLD").withAllowedStatuses(List.of("CLOSED")))
			.withEnded(java.time.OffsetDateTime.now());

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(endedPhase)));

		service.validateStatusAgainstActivePhase(errandEntity, "NEW");
	}

	@Test
	void validateStatus_statusAllowed_passes() {
		final var phaseEntity = PhaseEntity.create()
			.withName("INVESTIGATION")
			.withAllowedStatuses(List.of("NEW", "IN_PROGRESS", "WAITING"));

		final var activePhase = ErrandPhaseEntity.create().withPhaseEntity(phaseEntity);

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(activePhase)));

		service.validateStatusAgainstActivePhase(errandEntity, "IN_PROGRESS");
	}

	@Test
	void validateStatus_statusNotAllowed_throwsException() {
		final var phaseEntity = PhaseEntity.create()
			.withName("INVESTIGATION")
			.withAllowedStatuses(List.of("NEW", "IN_PROGRESS"));

		final var activePhase = ErrandPhaseEntity.create().withPhaseEntity(phaseEntity);

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(activePhase)));

		assertThatThrownBy(() -> service.validateStatusAgainstActivePhase(errandEntity, "CLOSED"))
			.asInstanceOf(InstanceOfAssertFactories.type(ThrowableProblem.class))
			.satisfies(problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(problem.getMessage()).contains("CLOSED").contains("INVESTIGATION");
			});
	}

	@Test
	void validateStatus_emptyAllowedStatuses_passes() {
		final var phaseEntity = PhaseEntity.create()
			.withName("INVESTIGATION")
			.withAllowedStatuses(List.of());

		final var activePhase = ErrandPhaseEntity.create().withPhaseEntity(phaseEntity);

		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(activePhase)));

		service.validateStatusAgainstActivePhase(errandEntity, "ANY_STATUS");
	}

	@Test
	void validateStatus_nullStatus_passes() {
		final var errandEntity = ErrandEntity.create()
			.withPhases(new ArrayList<>(List.of(ErrandPhaseEntity.create())));

		service.validateStatusAgainstActivePhase(errandEntity, null);

		verifyNoInteractions(phaseRepositoryMock);
	}
}
