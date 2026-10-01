package se.sundsvall.supportmanagement.api.model.metadata;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class LabelClassificationTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(LabelClassification.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testCreatePattern() {
		final var id = "5f79a808-0ef3-4985-99b9-b12f23e202a7";
		final var classification = "subtype";
		final var displayName = "Undertyp";
		final var created = OffsetDateTime.now();
		final var modified = OffsetDateTime.now().plusDays(1);

		final var bean = LabelClassification.create()
			.withId(id)
			.withClassification(classification)
			.withDisplayName(displayName)
			.withCreated(created)
			.withModified(modified);

		assertThat(bean.getId()).isEqualTo(id);
		assertThat(bean.getClassification()).isEqualTo(classification);
		assertThat(bean.getDisplayName()).isEqualTo(displayName);
		assertThat(bean.getCreated()).isEqualTo(created);
		assertThat(bean.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LabelClassification.create()).hasAllNullFieldsOrProperties();
		assertThat(new LabelClassification()).hasAllNullFieldsOrProperties();
	}
}
