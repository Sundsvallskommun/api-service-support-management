package se.sundsvall.supportmanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Random;

import static com.google.code.beanmatchers.BeanMatchers.*;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.assertj.core.api.Assertions.assertThat;

class StakeholderEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(StakeholderEntity.class, allOf(
				hasValidBeanConstructor(),
				hasValidGettersAndSetters(),
				hasValidBeanHashCode(),
				hasValidBeanEquals(),
				hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		var id = 1;
		var stakeholderId = "stakeholderId";
		var type = "type";
		var errand = ErrandEntity.create();

		var stakeholderEntity = StakeholderEntity.create()
				.withId(id)
				.withStakeholderId(stakeholderId)
				.withType(type)
				.withErrandEntity(errand);

		assertThat(stakeholderEntity.getId()).isEqualTo(id);
		assertThat(stakeholderEntity.getStakeholderId()).isEqualTo(stakeholderId);
		assertThat(stakeholderEntity.getType()).isEqualTo(type);
		assertThat(stakeholderEntity.getErrandEntity()).isSameAs(errand);

	}

	@Test
	void hasNoDirtOnCreatedBean() {
		Assertions.assertThat(StakeholderEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		Assertions.assertThat(new StakeholderEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}
