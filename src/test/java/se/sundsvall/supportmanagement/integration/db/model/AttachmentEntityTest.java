package se.sundsvall.supportmanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
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
		final var file = new AttachmentDataEntity().withFile(new MariaDbBlob("file".getBytes()));
		final var mimeType = "mimeType";
		final var errandEntity = ErrandEntity.create().withId(UUID.randomUUID().toString());
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var fileSize = 100;

		final var attachmentEntity = AttachmentEntity.create()
			.withId(id)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withFileName(fileName)
			.withAttachmentData(file)
			.withMimeType(mimeType)
			.withErrandEntity(errandEntity)
			.withCreated(now().truncatedTo(SECONDS))
			.withModified(now().truncatedTo(SECONDS))
			.withFileSize(fileSize);

		assertThat(attachmentEntity).hasNoNullFieldsOrProperties();
		assertThat(attachmentEntity.getId()).isEqualTo(id);
		assertThat(attachmentEntity.getNamespace()).isEqualTo(namespace);
		assertThat(attachmentEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(attachmentEntity.getFileName()).isEqualTo(fileName);
		assertThat(attachmentEntity.getAttachmentData()).isEqualTo(file);
		assertThat(attachmentEntity.getMimeType()).isEqualTo(mimeType);
		assertThat(attachmentEntity.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(attachmentEntity.getFileSize()).isEqualTo(fileSize);
	}

	@Test
	void testOnCreate() {
		final var entity = new AttachmentEntity();
		entity.onCreate();

		assertThat(entity.getCreated()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created");
	}

	@Test
	void testOnUpdate() {
		final var entity = new AttachmentEntity();
		entity.onUpdate();

		assertThat(entity.getModified()).isCloseTo(now(), within(1, SECONDS));
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("modified");
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(AttachmentEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new AttachmentEntity()).hasAllNullFieldsOrProperties();
	}

}
