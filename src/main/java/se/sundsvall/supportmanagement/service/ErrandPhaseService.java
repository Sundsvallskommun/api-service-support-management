package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.integration.db.PhaseRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandPhaseEntity;
import se.sundsvall.supportmanagement.integration.db.model.PhaseEntity;
import se.sundsvall.supportmanagement.integration.db.model.PhaseTransitionEntity;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class ErrandPhaseService {

	private static final String PHASE_NOT_FOUND = "Phase with id '%s' not found for namespace '%s' and municipality with id '%s'";
	private static final String INVALID_TRANSITION = "Transition from phase '%s' to phase '%s' is not allowed";
	private static final String NO_TRANSITIONS_CONFIGURED = "Phase '%s' has no configured transitions";
	private static final String MULTIPLE_ACTIVE_PHASES = "Data integrity error: errand has %d active phases";
	private static final String STATUS_NOT_ALLOWED = "Status '%s' is not allowed in the active phase '%s'. Allowed statuses: %s";

	private final PhaseRepository phaseRepository;

	public ErrandPhaseService(final PhaseRepository phaseRepository) {
		this.phaseRepository = phaseRepository;
	}

	public void processPhaseChange(final ErrandEntity errandEntity, final String newPhaseId, final String namespace, final String municipalityId) {
		if (newPhaseId == null) {
			return;
		}

		final var targetPhase = lookupPhase(newPhaseId, namespace, municipalityId);

		final var activePhase = findActivePhase(errandEntity);
		if (activePhase.isPresent() && isAlreadyInPhase(activePhase.get(), newPhaseId)) {
			return;
		}

		activePhase.ifPresent(current -> {
			validateTransition(current.getPhaseEntity(), targetPhase);
			current.setEnded(now(systemDefault()).truncatedTo(MILLIS));
		});

		addPhase(errandEntity, targetPhase);
	}

	public void validateStatusAgainstActivePhase(final ErrandEntity errandEntity, final String newStatus) {
		if (newStatus == null) {
			return;
		}

		final var activePhase = findActivePhase(errandEntity);
		if (activePhase.isEmpty()) {
			return;
		}

		final var phase = activePhase.get().getPhaseEntity();
		final var allowedStatuses = getOrEmpty(phase.getAllowedStatuses());

		if (!allowedStatuses.isEmpty() && !allowedStatuses.contains(newStatus)) {
			throw Problem.valueOf(BAD_REQUEST, STATUS_NOT_ALLOWED.formatted(newStatus, phase.getName(), allowedStatuses));
		}
	}

	private PhaseEntity lookupPhase(final String phaseId, final String namespace, final String municipalityId) {
		return phaseRepository.findByIdAndNamespaceAndMunicipalityId(phaseId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, PHASE_NOT_FOUND.formatted(phaseId, namespace, municipalityId)));
	}

	private boolean isAlreadyInPhase(final ErrandPhaseEntity activePhase, final String phaseId) {
		return phaseId.equals(activePhase.getPhaseEntity().getId());
	}

	private void validateTransition(final PhaseEntity fromPhase, final PhaseEntity toPhase) {
		final var transitions = getOrEmpty(fromPhase.getTransitions());

		if (transitions.isEmpty()) {
			throw Problem.valueOf(BAD_REQUEST, NO_TRANSITIONS_CONFIGURED.formatted(fromPhase.getName()));
		}

		if (transitions.stream().map(PhaseTransitionEntity::getTargetPhaseId).noneMatch(toPhase.getId()::equals)) {
			throw Problem.valueOf(BAD_REQUEST, INVALID_TRANSITION.formatted(fromPhase.getName(), toPhase.getName()));
		}
	}

	private void addPhase(final ErrandEntity errandEntity, final PhaseEntity phaseEntity) {
		if (errandEntity.getPhases() == null) {
			errandEntity.setPhases(new ArrayList<>());
		}

		errandEntity.getPhases().add(ErrandPhaseEntity.create()
			.withErrandEntity(errandEntity)
			.withPhaseEntity(phaseEntity)
			.withStarted(now(systemDefault()).truncatedTo(MILLIS)));
	}

	private Optional<ErrandPhaseEntity> findActivePhase(final ErrandEntity errandEntity) {
		final var activePhases = getOrEmpty(errandEntity.getPhases()).stream()
			.filter(phase -> phase.getEnded() == null)
			.toList();

		if (activePhases.size() > 1) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, MULTIPLE_ACTIVE_PHASES.formatted(activePhases.size()));
		}

		return activePhases.stream().findFirst();
	}

	private static <T> List<T> getOrEmpty(final List<T> list) {
		return ofNullable(list).orElse(emptyList());
	}
}
