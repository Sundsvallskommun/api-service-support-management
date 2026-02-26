package se.sundsvall.supportmanagement.api.model.errand;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ErrandActionTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ErrandAction.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = randomUUID().toString();
		final var actionName = "ADD_LABEL";
		final var executeAfter = OffsetDateTime.now().plusDays(1);
		final var actionConfigId = randomUUID().toString();
		final var displayValue = "Label will be added";

		final var bean = ErrandAction.create()
			.withId(id)
			.withActionName(actionName)
			.withExecuteAfter(executeAfter)
			.withActionConfigId(actionConfigId)
			.withDisplayValue(displayValue);

		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getActionName()).isEqualTo(actionName);
		assertThat(bean.getExecuteAfter()).isEqualTo(executeAfter);
		assertThat(bean.getActionConfigId()).isEqualTo(actionConfigId);
		assertThat(bean.getDisplayValue()).isEqualTo(displayValue);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandAction.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandAction()).hasAllNullFieldsOrProperties();
	}
}
