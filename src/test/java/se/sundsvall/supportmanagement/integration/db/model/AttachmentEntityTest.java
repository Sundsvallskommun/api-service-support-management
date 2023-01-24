package se.sundsvall.supportmanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AttachmentEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(AttachmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = UUID.randomUUID().toString();
		final var fileName = "fileName";
		final var file = "file".getBytes();
		final var mimeType = "mimeType";
		final var errandEntity = ErrandEntity.create().withId(UUID.randomUUID().toString());

		final var attachmentEntity = AttachmentEntity.create()
			.withId(id)
			.withFileName(fileName)
			.withFile(file)
			.withMimeType(mimeType)
			.withErrandEntity(errandEntity)
			.withCreated(now().truncatedTo(SECONDS))
			.withModified(now().truncatedTo(SECONDS));

		Assertions.assertThat(attachmentEntity).hasNoNullFieldsOrProperties();
		Assertions.assertThat(attachmentEntity.getId()).isEqualTo(id);
		Assertions.assertThat(attachmentEntity.getFileName()).isEqualTo(fileName);
		Assertions.assertThat(attachmentEntity.getFile()).isEqualTo(file);
		Assertions.assertThat(attachmentEntity.getMimeType()).isEqualTo(mimeType);
		Assertions.assertThat(attachmentEntity.getErrandEntity()).isEqualTo(errandEntity);
	}

	@Test
	void testOnCreate() {
		final var entity = new AttachmentEntity();
		entity.onCreate();

		Assertions.assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = new AttachmentEntity();
		entity.onUpdate();

		Assertions.assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		Assertions.assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		Assertions.assertThat(AttachmentEntity.create()).hasAllNullFieldsOrProperties();
		Assertions.assertThat(new AttachmentEntity()).hasAllNullFieldsOrProperties();
	}

}
