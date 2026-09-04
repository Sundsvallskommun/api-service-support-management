package se.sundsvall.supportmanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSettersExcluding;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AttachmentEntityTest {

	// The id of the data row is mapped for reading only - the association to the data is what writes that column - so it
	// is the one property with a getter and no setter, and the one a builder leaves for the database to fill in. The
	// bean matchers reach a property through its setter, so it is left out of each of them rather than tested through
	// one it does not have.
	private static final String READ_ONLY_PROPERTY = "attachmentDataId";

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(AttachmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSettersExcluding(READ_ONLY_PROPERTY),
			hasValidBeanHashCodeExcluding(READ_ONLY_PROPERTY),
			hasValidBeanEqualsExcluding(READ_ONLY_PROPERTY),
			hasValidBeanToStringExcluding("errandEntity", READ_ONLY_PROPERTY)));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = UUID.randomUUID().toString();
		final var fileName = "fileName";
		final var file = new AttachmentDataEntity().withFile(new MariaDbBlob("file".getBytes()));
		final var mimeType = "mimeType";
		final var channel = "EMAIL";
		final var errandEntity = ErrandEntity.create().withId(UUID.randomUUID().toString());
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var fileSize = 100;
		final var hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

		final var attachmentEntity = AttachmentEntity.create()
			.withId(id)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withFileName(fileName)
			.withAttachmentData(file)
			.withMimeType(mimeType)
			.withChannel(channel)
			.withErrandEntity(errandEntity)
			.withCreated(now().truncatedTo(SECONDS))
			.withModified(now().truncatedTo(SECONDS))
			.withFileSize(fileSize)
			.withHash(hash);

		assertThat(attachmentEntity).hasNoNullFieldsOrPropertiesExcept(READ_ONLY_PROPERTY);
		assertThat(attachmentEntity.getId()).isEqualTo(id);
		assertThat(attachmentEntity.getNamespace()).isEqualTo(namespace);
		assertThat(attachmentEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(attachmentEntity.getFileName()).isEqualTo(fileName);
		assertThat(attachmentEntity.getAttachmentData()).isEqualTo(file);
		assertThat(attachmentEntity.getMimeType()).isEqualTo(mimeType);
		assertThat(attachmentEntity.getChannel()).isEqualTo(channel);
		assertThat(attachmentEntity.getErrandEntity()).isEqualTo(errandEntity);
		assertThat(attachmentEntity.getFileSize()).isEqualTo(fileSize);
		assertThat(attachmentEntity.getHash()).isEqualTo(hash);
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
