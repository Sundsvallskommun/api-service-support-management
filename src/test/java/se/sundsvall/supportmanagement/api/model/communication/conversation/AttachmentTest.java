package se.sundsvall.supportmanagement.api.model.communication.conversation;

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

class AttachmentTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Attachment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var created = now();
		final var fileName = "fileName";
		final var fileSize = 1;
		final var id = randomUUID().toString();
		final var mimeType = "mimeType";

		final var object = Attachment.create()
			.withCreated(created)
			.withFileName(fileName)
			.withFileSize(fileSize)
			.withId(id)
			.withMimeType(mimeType);

		assertThat(object).isNotNull().hasNoNullFieldsOrPropertiesExcept("fileSize");
		assertThat(object.getCreated()).isEqualTo(created);
		assertThat(object.getFileName()).isEqualTo(fileName);
		assertThat(object.getFileSize()).isEqualTo(fileSize);
		assertThat(object.getId()).isEqualTo(id);
		assertThat(object.getMimeType()).isEqualTo(mimeType);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Attachment.create()).hasAllNullFieldsOrPropertiesExcept("fileSize");
		assertThat(new Attachment()).hasAllNullFieldsOrPropertiesExcept("fileSize");
	}
}
