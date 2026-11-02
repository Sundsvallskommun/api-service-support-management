package se.sundsvall.supportmanagement.api.model.attachment;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class UpdateErrandAttachmentRequestTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(UpdateErrandAttachmentRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		// Act
		final var purpose = ErrandAttachmentPurpose.create().withId("5f79a808-0ef3-4985-99b9-b12f23e202a7");
		final var received = now().minusDays(1);
		final var bean = UpdateErrandAttachmentRequest.create().withPurpose(purpose).withReceived(received);

		// Assert
		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getPurpose()).isEqualTo(purpose);
		assertThat(bean.getReceived()).isEqualTo(received);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(UpdateErrandAttachmentRequest.create()).hasAllNullFieldsOrProperties();
		assertThat(new UpdateErrandAttachmentRequest()).hasAllNullFieldsOrProperties();
	}
}
