package se.sundsvall.supportmanagement.apptest;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.supportmanagement.Application;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessActivityRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandProcessRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessActivityEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandProcessEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static java.time.Clock.systemDefaultZone;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ActivitySeverity.ERROR;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.COMPLETED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

/**
 * The rules the process tables lean on live in the database rather than in the code: the errand cascade that keeps
 * orphans out, and the unique index that lets an errand carry one live process instance and any number of finished
 * ones. None of them exist in a schema generated from the entities, so they can only be verified where Flyway has run -
 * which is the same reason {@code ShedlockConfigurationIT} sits here.
 * <p>
 * That the context starts at all is a check in itself: the IT profile validates the mapped entities against the
 * migrated schema.
 * <p>
 * What a cascade left behind is asked with {@code existsById}, which runs a count against the database. {@code findById}
 * would answer out of the persistence context, which still holds the instance it loaded and would report a row the
 * database removed without telling JPA.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("it")
@Transactional
class ProcessIntegrationDataModelIT {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "PROCESS-DATA-MODEL-IT";

	@Autowired
	private ErrandsRepository errandsRepository;

	@Autowired
	private ErrandProcessRepository errandProcessRepository;

	@Autowired
	private ErrandProcessActivityRepository errandProcessActivityRepository;

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

		assertThat(errandProcessRepository.findByErrandIdAndActiveMarkerIsNotNull(errandId))
			.get()
			.extracting(ErrandProcessEntity::getId)
			.isEqualTo(live.getId());
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
