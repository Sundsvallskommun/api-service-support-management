package se.sundsvall.supportmanagement.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;

import java.time.OffsetDateTime;
import java.util.Random;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AttachmentDataEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(AttachmentDataEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {

		var id = 1;
		var blob = new MariaDbBlob();

		var attachmentData = AttachmentDataEntity.create()
			.withId(id)
			.withFile(blob);

		assertThat(attachmentData).hasNoNullFieldsOrProperties();
		assertThat(attachmentData.getFile()).isSameAs(blob);
		assertThat(attachmentData.getId()).isEqualTo(id);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(AttachmentDataEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new AttachmentDataEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}
