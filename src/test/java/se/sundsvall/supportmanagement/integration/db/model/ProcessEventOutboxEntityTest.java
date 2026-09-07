package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ProcessEventOutboxEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ProcessEventOutboxEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanToString()));
	}

	@Test
	void testHashCodeAndEquals() {
		final var entity1 = ProcessEventOutboxEntity.create().withId("id-1").withErrandId("errand-1");
		final var entity2 = ProcessEventOutboxEntity.create().withId("id-1").withErrandId("errand-1");
		final var entity3 = ProcessEventOutboxEntity.create().withId("id-2").withErrandId("errand-1");

		assertThat(entity1)
			.isEqualTo(entity2)
			.hasSameHashCodeAs(entity2)
			.isNotEqualTo(entity3);
	}

	@Test
	void hasValidBuilderMethods() {
		final var created = OffsetDateTime.now().minusMinutes(5);
		final var deliveredAt = OffsetDateTime.now();
		final var errandId = "9a1b2c3d-4e5f-6789-abcd-ef0123456789";
		final var eventSubType = "SIGNAL";
		final var eventType = "UPDATE";
		final var executedBy = "joe01doe";
		final var id = "6a5b8c9d-1234-5678-abcd-ef0123456789";
		final var municipalityId = "2281";
		final var namespace = "ALKT";
		final var processKey = "alkt-ansokan";
		final var processService = "pw-alkt";
		final var requestGroupId = "1a2b3c4d-5e6f-7890-abcd-ef0123456789";
		final var signalName = "granskning-godkand";

		final var entity = ProcessEventOutboxEntity.create()
			.withCreated(created)
			.withDeliveredAt(deliveredAt)
			.withErrandId(errandId)
			.withEventSubType(eventSubType)
			.withEventType(eventType)
			.withExecutedBy(executedBy)
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withProcessKey(processKey)
			.withProcessService(processService)
			.withRequestGroupId(requestGroupId)
			.withSignalName(signalName)
			.withStartAllowed(true);

		assertThat(entity)
			.hasNoNullFieldsOrProperties()
			.satisfies(e -> {
				assertThat(e.getCreated()).isEqualTo(created);
				assertThat(e.getDeliveredAt()).isEqualTo(deliveredAt);
				assertThat(e.getErrandId()).isEqualTo(errandId);
				assertThat(e.getEventSubType()).isEqualTo(eventSubType);
				assertThat(e.getEventType()).isEqualTo(eventType);
				assertThat(e.getExecutedBy()).isEqualTo(executedBy);
				assertThat(e.getId()).isEqualTo(id);
				assertThat(e.getMunicipalityId()).isEqualTo(municipalityId);
				assertThat(e.getNamespace()).isEqualTo(namespace);
				assertThat(e.getProcessKey()).isEqualTo(processKey);
				assertThat(e.getProcessService()).isEqualTo(processService);
				assertThat(e.getRequestGroupId()).isEqualTo(requestGroupId);
				assertThat(e.getSignalName()).isEqualTo(signalName);
				assertThat(e.isStartAllowed()).isTrue();
			});
	}

	@Test
	void testOnCreate() {
		final var entity = ProcessEventOutboxEntity.create();
		entity.onCreate();

		assertThat(entity)
			.hasAllNullFieldsOrPropertiesExcept("created", "startAllowed")
			.satisfies(e -> {
				assertThat(e.getCreated()).isCloseTo(now(), within(1, SECONDS));
				assertThat(e.isStartAllowed()).isFalse();
			});
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ProcessEventOutboxEntity.create()).hasAllNullFieldsOrPropertiesExcept("startAllowed");
		assertThat(new ProcessEventOutboxEntity()).hasAllNullFieldsOrPropertiesExcept("startAllowed");
	}
}
