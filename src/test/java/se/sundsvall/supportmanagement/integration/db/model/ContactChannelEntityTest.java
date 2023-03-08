package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import java.time.OffsetDateTime;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.code.beanmatchers.BeanMatchers;

class ContactChannelEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(ContactChannelEntity.class, allOf(
				hasValidBeanConstructor(),
				hasValidGettersAndSetters(),
				hasValidBeanHashCode(),
				hasValidBeanEquals(),
				hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		var type = "type";
		var value = "value";

		var contactChannelEntity = ContactChannelEntity.create()
				.withType(type)
				.withValue(value);

		Assertions.assertThat(contactChannelEntity.getType()).isEqualTo(type);
		Assertions.assertThat(contactChannelEntity.getValue()).isEqualTo(value);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		Assertions.assertThat(ContactChannelEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		Assertions.assertThat(new ContactChannelEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}