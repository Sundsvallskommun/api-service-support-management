package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ErrandProcessSignalEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandProcessSignalEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var created = OffsetDateTime.now();

		final var entity = ErrandProcessSignalEntity.create()
			.withId("id")
			.withErrandProcessId("errandProcessId")
			.withName("granskning-godkand")
			.withLabel("Godkänn granskning")
			.withSortOrder(2)
			.withCreated(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandProcessId()).isEqualTo("errandProcessId");
		assertThat(entity.getName()).isEqualTo("granskning-godkand");
		assertThat(entity.getLabel()).isEqualTo("Godkänn granskning");
		assertThat(entity.getSortOrder()).isEqualTo(2);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void testOnCreate() {
		final var entity = ErrandProcessSignalEntity.create();
		entity.onCreate();

		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "sortOrder");
		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandProcessSignalEntity.create()).hasAllNullFieldsOrPropertiesExcept("sortOrder");
		assertThat(new ErrandProcessSignalEntity()).hasAllNullFieldsOrPropertiesExcept("sortOrder");
		assertThat(ErrandProcessSignalEntity.create().getSortOrder()).isZero();
	}
}
