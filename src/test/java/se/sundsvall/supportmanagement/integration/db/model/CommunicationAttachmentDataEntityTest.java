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

class CommunicationAttachmentDataEntityTest {

	@Test
	void testBean() {
		assertThat(CommunicationAttachmentDataEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {

		final var id = 1L;
		final var content = "content";
		final var file = new MariaDbBlob(content.getBytes());

		final var entity = CommunicationAttachmentDataEntity.create()
			.withId(id)
			.withFile(file);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getFile()).isEqualTo(file);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CommunicationAttachmentDataEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new CommunicationAttachmentDataEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}

}
