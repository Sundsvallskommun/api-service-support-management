package se.sundsvall.supportmanagement.integration.db.model;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSettersExcluding;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.FAILED;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.RUNNING;
import static se.sundsvall.supportmanagement.integration.db.model.enums.ProcessStatus.WAITING;

class ErrandProcessEntityTest {

	private static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-07T10:15:30.123Z"), ZoneId.of("UTC"));

	private static final String[] OWNED_BY_APPLY_STATUS = {
		"processStatus", "activeMarker", "ended"
	};

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	/**
	 * The three properties left out are the ones applyStatus owns. They have no setter to exercise, which is the point of
	 * them, and the bean matcher counts a property it cannot write to as broken.
	 */
	@Test
	void testBean() {
		assertThat(ErrandProcessEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSettersExcluding(OWNED_BY_APPLY_STATUS),
			hasValidBeanToStringExcluding(OWNED_BY_APPLY_STATUS)));
	}

	@Test
	void testHashCodeAndEquals() {
		final var entity1 = ErrandProcessEntity.create().withId("id-1").withErrandId("errand-1");
		final var entity2 = ErrandProcessEntity.create().withId("id-1").withErrandId("errand-1");
		final var entity3 = ErrandProcessEntity.create().withId("id-2").withErrandId("errand-1");

		assertThat(entity1)
			.isEqualTo(entity2)
			.hasSameHashCodeAs(entity2)
			.isNotEqualTo(entity3);
	}

	@Test
	void hasValidBuilderMethods() {
		final var created = OffsetDateTime.now().minusHours(2);
		final var currentActivityId = "granska-ansokan";
		final var currentActivityName = "Granska ansokan";
		final var errandId = "ERRAND_ID-1";
		final var errorCode = "PROCESS_NOT_DEPLOYED";
		final var errorMessage = "No deployed process with key alkt-ansokan";
		final var id = "6a5b8c9d-1234-5678-abcd-ef0123456789";
		final var modified = OffsetDateTime.now();
		final var municipalityId = "2281";
		final var namespace = "ALKT";
		final var processInstanceId = "8f3d1e2a-0000-4444-8888-cccccccccccc";
		final var processKey = "alkt-ansokan";
		final var processService = "pw-alkt";
		final var started = OffsetDateTime.now().minusHours(1);

		final var entity = ErrandProcessEntity.create()
			.withCreated(created)
			.withCurrentActivityId(currentActivityId)
			.withCurrentActivityName(currentActivityName)
			.withErrandId(errandId)
			.withErrorCode(errorCode)
			.withErrorMessage(errorMessage)
			.withId(id)
			.withModified(modified)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withProcessInstanceId(processInstanceId)
			.withProcessKey(processKey)
			.withProcessService(processService)
			.withStarted(started);
		entity.applyStatus(RUNNING, FIXED);

		assertThat(entity)
			.hasNoNullFieldsOrPropertiesExcept("ended")
			.satisfies(e -> {
				assertThat(e.getCreated()).isEqualTo(created);
				assertThat(e.getCurrentActivityId()).isEqualTo(currentActivityId);
				assertThat(e.getCurrentActivityName()).isEqualTo(currentActivityName);
				assertThat(e.getErrandId()).isEqualTo(errandId);
				assertThat(e.getErrorCode()).isEqualTo(errorCode);
				assertThat(e.getErrorMessage()).isEqualTo(errorMessage);
				assertThat(e.getId()).isEqualTo(id);
				assertThat(e.getModified()).isEqualTo(modified);
				assertThat(e.getMunicipalityId()).isEqualTo(municipalityId);
				assertThat(e.getNamespace()).isEqualTo(namespace);
				assertThat(e.getProcessInstanceId()).isEqualTo(processInstanceId);
				assertThat(e.getProcessKey()).isEqualTo(processKey);
				assertThat(e.getProcessService()).isEqualTo(processService);
				assertThat(e.getProcessStatus()).isEqualTo(RUNNING);
				assertThat(e.getStarted()).isEqualTo(started);
			});
	}

	@ParameterizedTest
	@EnumSource(ProcessStatus.class)
	@DisplayName("Verification that the active marker follows what the status itself says about being terminal")
	void applyStatusKeepsTheMarkerInStep(final ProcessStatus status) {
		final var entity = ErrandProcessEntity.create();

		entity.applyStatus(status, FIXED);

		assertThat(entity.getProcessStatus()).isEqualTo(status);
		if (status.isTerminal()) {
			assertThat(entity.getActiveMarker()).isNull();
			assertThat(entity.getEnded()).isEqualTo(OffsetDateTime.now(FIXED).truncatedTo(MILLIS));
		} else {
			assertThat(entity.getActiveMarker()).isNotNull();
			assertThat(entity.getEnded()).isNull();
		}
	}

	@Test
	@DisplayName("Verification that an instance resumed by hand does not keep the end time it was given when it failed")
	void applyStatusClearsEndedWhenTheProcessLivesAgain() {
		final var entity = ErrandProcessEntity.create();
		entity.applyStatus(FAILED, FIXED);

		assertThat(entity.getEnded()).isNotNull();
		assertThat(entity.getActiveMarker()).isNull();

		entity.applyStatus(RUNNING, FIXED);

		assertThat(entity.getEnded()).isNull();
		assertThat(entity.getActiveMarker()).isNotNull();
	}

	@Test
	@DisplayName("Verification that a process which is merely waiting keeps its place, so the errand cannot be given a second live instance")
	void waitingKeepsTheMarker() {
		final var entity = ErrandProcessEntity.create();

		entity.applyStatus(WAITING, FIXED);

		assertThat(entity.getActiveMarker()).isNotNull();
		assertThat(entity.getEnded()).isNull();
	}

	@Test
	@DisplayName("Verification that the status has no way in past applyStatus, which is what keeps the marker from drifting")
	void statusHasNoPublicSetter() {
		assertThat(Arrays.stream(ErrandProcessEntity.class.getMethods()).map(Method::getName))
			.doesNotContain("setProcessStatus", "withProcessStatus", "setActiveMarker", "withActiveMarker", "setEnded", "withEnded");
	}

	@Test
	void testOnCreate() {
		final var entity = ErrandProcessEntity.create();
		entity.onCreate();

		assertThat(entity)
			.hasAllNullFieldsOrPropertiesExcept("created")
			.satisfies(e -> assertThat(e.getCreated()).isCloseTo(now(), within(1, SECONDS)));
	}

	@Test
	void testOnUpdate() {
		final var entity = ErrandProcessEntity.create();
		entity.onUpdate();

		assertThat(entity)
			.hasAllNullFieldsOrPropertiesExcept("modified")
			.satisfies(e -> assertThat(e.getModified()).isCloseTo(now(), within(1, SECONDS)));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandProcessEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandProcessEntity()).hasAllNullFieldsOrProperties();
	}
}
