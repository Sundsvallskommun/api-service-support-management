package se.sundsvall.supportmanagement.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
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
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var attachmentID = "1";
		final var content = "content";
		final var file = new MariaDbBlob(content.getBytes());
		final var attachmentData = CommunicationAttachmentDataEntity.create().withId(1).withFile(file);
		final var name = "name";
		final var contentType = "contentType";
		final var communicationID = CommunicationEntity.create();

		final var entity = CommunicationAttachmentEntity.create()
			.withAttachmentID(attachmentID)
			.withCommunicationID(communicationID)
			.withAttachmentData(attachmentData)
			.withName(name)
			.withContentType(contentType);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getAttachmentID()).isEqualTo(attachmentID);
		assertThat(entity.getCommunicationID()).isEqualTo(communicationID);
		assertThat(entity.getAttachmentData()).isEqualTo(attachmentData);
		assertThat(entity.getName()).isEqualTo(name);
		assertThat(entity.getContentType()).isEqualTo(contentType);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CommunicationAttachmentEntity.create()).hasAllNullFieldsOrPropertiesExcept("attachmentID");
		assertThat(new CommunicationAttachmentEntity()).hasAllNullFieldsOrPropertiesExcept("attachmentID");
	}

}
