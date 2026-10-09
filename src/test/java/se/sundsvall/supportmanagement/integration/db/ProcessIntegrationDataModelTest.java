package se.sundsvall.supportmanagement.integration.db;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessSignalEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static java.time.Clock.systemDefaultZone;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.INFO;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

/**
 * Verifies the rules the process tables get from the database, on the schema Flyway migrates under the dbtest profile:
 * the errand cascade that keeps orphans out, and the unique index that lets an errand carry one live process instance
 * and any number of finished ones.
 * <p>
 * The context starting is a check in itself: the profile validates the mapped entities against the migrated schema.
 * <p>
 * What a cascade left behind is asked with {@code existsById}, which runs a count against the database.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles({
	"junit", "dbtest"
})
@Transactional
class ProcessIntegrationDataModelTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-DATA-MODEL-IT";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private ErrandProcessRepository errandProcessRepository;

	@Autowired
	private ErrandProcessActivityRepository errandProcessActivityRepository;

	@Autowired
	private ErrandProcessSignalRepository errandProcessSignalRepository;

	@Test
	@DisplayName("Verification that an entry written before any process instance exists is removed with the errand, since errand_id is the only thing tying it to anything")
	void anInstancelessActivityIsCascadedAwayWithTheErrand() {
		final var errandId = createErrand();
		final var activityId = errandProcessActivityRepository.saveAndFlush(ErrandProcessActivityEntity.create()
			.withErrandId(errandId)
			.withActivityType("CONFIG")
			.withSeverity(ERROR)
			.withMessage("Two labels resolve to different process keys")
			.withOccurredAt(now(systemDefault()))).getId();

		assertThat(errandProcessActivityRepository.existsById(activityId)).isTrue();

		errandsRepository.deleteById(errandId);
		errandsRepository.flush();

		assertThat(errandProcessActivityRepository.existsById(activityId)).isFalse();
	}

	@Test
	@DisplayName("Verification that a process instance is removed with the errand it belongs to, and its log with it")
	void aProcessAndItsLogAreCascadedAwayWithTheErrand() {
		final var errandId = createErrand();
		final var process = saveProcess(errandId, RUNNING);
		final var activityId = errandProcessActivityRepository.saveAndFlush(ErrandProcessActivityEntity.create()
			.withErrandProcessId(process.getId())
			.withErrandId(errandId)
			.withActivityType("TASK")
			.withSeverity(INFO)
			.withOccurredAt(now(systemDefault()))).getId();

		errandsRepository.deleteById(errandId);
		errandsRepository.flush();

		assertThat(errandProcessActivityRepository.existsById(activityId)).isFalse();
		assertThat(errandProcessRepository.existsById(process.getId())).isFalse();
	}

	@Test
	@DisplayName("Verification that the database, and not discipline, is what holds an errand to one live process instance")
	void anErrandCannotBeGivenASecondLiveProcess() {
		final var errandId = createErrand();
		saveProcess(errandId, RUNNING);

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> saveProcess(errandId, WAITING));
	}

	@Test
	@DisplayName("Verification that finished instances give their place back, so an errand can be through any number of processes")
	void anErrandCanCarryAnyNumberOfFinishedProcesses() {
		final var errandId = createErrand();
		saveProcess(errandId, COMPLETED);
		saveProcess(errandId, COMPLETED);
		final var live = saveProcess(errandId, WAITING);

		assertThat(errandProcessRepository.findByErrandIdOrderByCreatedDesc(errandId))
			.filteredOn(ErrandProcessEntity::isLive)
			.extracting(ErrandProcessEntity::getId)
			.containsExactly(live.getId());
	}

	@Test
	@DisplayName("Verification that what a process waits for goes with its process row, and so with the errand, two cascades down")
	void theSignalsOfAProcessAreCascadedAwayWithTheErrand() {
		final var errandId = createErrand();
		final var process = saveProcess(errandId, WAITING);
		final var signalId = saveSignal(process.getId(), "granskning-godkand").getId();

		errandsRepository.deleteById(errandId);
		errandsRepository.flush();

		assertThat(errandProcessSignalRepository.existsById(signalId)).isFalse();
		assertThat(errandProcessRepository.existsById(process.getId())).isFalse();
	}

	@Test
	@DisplayName("Verification that an instance can wait for a name once")
	void anInstanceWaitsForANameOnce() {
		final var processId = saveProcess(createErrand(), WAITING).getId();
		saveSignal(processId, "granskning-godkand");

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> saveSignal(processId, "granskning-godkand"));
	}

	@Test
	@DisplayName("Verification that names differing only in case, accents or a trailing space are different signals")
	void namesDifferingOnlyInCaseAccentsOrATrailingSpaceAreDifferentSignals() {
		final var process = saveProcess(createErrand(), WAITING);
		final var names = List.of("aterremiss", "Aterremiss", "återremiss", "aterremiss ");

		names.forEach(name -> saveSignal(process.getId(), name));

		assertThat(errandProcessSignalRepository.findByErrandProcessIdOrderBySortOrderAsc(process.getId()))
			.extracting(ErrandProcessSignalEntity::getName)
			.containsExactlyInAnyOrderElementsOf(names);
	}

	@Test
	@DisplayName("Verification that two instances may wait for the same name")
	void twoInstancesMayWaitForTheSameName() {
		final var first = saveSignal(saveProcess(createErrand(), WAITING).getId(), "granskning-godkand").getId();
		final var second = saveSignal(saveProcess(createErrand(), WAITING).getId(), "granskning-godkand").getId();

		assertThat(errandProcessSignalRepository.existsById(first)).isTrue();
		assertThat(errandProcessSignalRepository.existsById(second)).isTrue();
	}

	private ErrandProcessSignalEntity saveSignal(final String errandProcessId, final String name) {
		return errandProcessSignalRepository.saveAndFlush(ErrandProcessSignalEntity.create()
			.withErrandProcessId(errandProcessId)
			.withName(name));
	}

	private String createErrand() {
		return errandsRepository.saveAndFlush(ErrandEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withErrandNumber("PDM-" + UUID.randomUUID())
			.withTitle("TITLE")
			.withStatus("STATUS")
			.withPriority("MEDIUM")
			.withReporterUserId("joe01doe")).getId();
	}

	private ErrandProcessEntity saveProcess(final String errandId, final ProcessStatus status) {
		final var entity = ErrandProcessEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withProcessService("pw-alkt")
			.withProcessKey("alkt-ansokan")
			.withProcessInstanceId(UUID.randomUUID().toString());
		entity.applyStatus(status, systemDefaultZone());

		return errandProcessRepository.saveAndFlush(entity);
	}
}
