package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ErrandActionEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandActionEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = UUID.randomUUID().toString();
		final var errandEntity = ErrandEntity.create().withId("errand-id");
		final var executeAfter = OffsetDateTime.now().plusDays(1);
		final var actionConfigEntity = ActionConfigEntity.create().withId("config-id");

		final var entity = ErrandActionEntity.create()
			.withId(id)
			.withErrandEntity(errandEntity)
			.withExecuteAfter(executeAfter)
			.withActionConfigEntity(actionConfigEntity);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(entity.getExecuteAfter()).isEqualTo(executeAfter);
		assertThat(entity.getActionConfigEntity()).isEqualTo(actionConfigEntity);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ErrandActionEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandActionEntity()).hasAllNullFieldsOrProperties();
	}
}
