package se.sundsvall.supportmanagement.integration.db.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
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
import static org.hamcrest.CoreMatchers.allOf;

class TeliaAceContactEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(TeliaAceContactEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var errandId = "errand-id";
		final var emailUid = "42.CallGuideWorkItem@computer";
		final var created = now();

		final var bean = TeliaAceContactEntity.create()
			.withErrandId(errandId)
			.withEmailUid(emailUid)
			.withCreated(created);

		assertThat(bean.getErrandId()).isEqualTo(errandId);
		assertThat(bean.getEmailUid()).isEqualTo(emailUid);
		assertThat(bean.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TeliaAceContactEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new TeliaAceContactEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersistSetsCreated() {
		final var bean = TeliaAceContactEntity.create();
		bean.onCreate();
		assertThat(bean.getCreated()).isCloseTo(now(), within(2, SECONDS));
		assertThat(bean).hasAllNullFieldsOrPropertiesExcept("created");
	}
}
