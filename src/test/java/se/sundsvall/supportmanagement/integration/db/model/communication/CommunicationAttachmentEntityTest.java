package se.sundsvall.supportmanagement.integration.db.model.communication;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentDataEntity;

class CommunicationAttachmentEntityTest {

	@Test
	void testBean() {
		assertThat(CommunicationAttachmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("communicationEntity"),
			hasValidBeanEqualsExcluding("communicationEntity"),
			hasValidBeanToStringExcluding("communicationEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = "1";
		final var content = "content";
		final var file = new MariaDbBlob(content.getBytes());
		final var attachmentData = AttachmentDataEntity.create().withId(1).withFile(file);
		final var name = "name";
		final var contentType = "contentType";
		final var communicationEntity = CommunicationEntity.create();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";
		final var foreignId = "foreignId";
		final var fileSize = 100;

		final var entity = CommunicationAttachmentEntity.create()
			.withId(id)
			.withCommunicationEntity(communicationEntity)
			.withAttachmentData(attachmentData)
			.withFileName(name)
			.withContentType(contentType)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withForeignId(foreignId)
			.withFileSize(fileSize);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getCommunicationEntity()).isEqualTo(communicationEntity);
		assertThat(entity.getAttachmentData()).isEqualTo(attachmentData);
		assertThat(entity.getFileName()).isEqualTo(name);
		assertThat(entity.getMimeType()).isEqualTo(contentType);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getForeignId()).isEqualTo(foreignId);
		assertThat(entity.getFileSize()).isEqualTo(fileSize);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CommunicationAttachmentEntity.create()).hasAllNullFieldsOrPropertiesExcept("attachmentID");
		assertThat(new CommunicationAttachmentEntity()).hasAllNullFieldsOrPropertiesExcept("attachmentID");
	}

}
