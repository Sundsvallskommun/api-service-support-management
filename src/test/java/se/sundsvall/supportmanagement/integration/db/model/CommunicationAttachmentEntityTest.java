package se.sundsvall.supportmanagement.integration.db.model;

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
		final var attachmentData = CommunicationAttachmentDataEntity.create().withId(1L).withFile(file);
		final var name = "name";
		final var contentType = "contentType";
		final var communicationEntity = CommunicationEntity.create();
		final var namespace = "namespace";
		final var municipalityId = "municipalityId";

		final var entity = CommunicationAttachmentEntity.create()
			.withId(id)
			.withCommunicationEntity(communicationEntity)
			.withAttachmentData(attachmentData)
			.withName(name)
			.withContentType(contentType)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getCommunicationEntity()).isEqualTo(communicationEntity);
		assertThat(entity.getAttachmentData()).isEqualTo(attachmentData);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getContentType()).isEqualTo(contentType);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CommunicationAttachmentEntity.create()).hasAllNullFieldsOrPropertiesExcept("attachmentID");
		assertThat(new CommunicationAttachmentEntity()).hasAllNullFieldsOrPropertiesExcept("attachmentID");
	}

}
