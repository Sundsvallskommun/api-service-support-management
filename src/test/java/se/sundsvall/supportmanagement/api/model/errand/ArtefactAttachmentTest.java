package se.sundsvall.supportmanagement.api.model.errand;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.supportmanagement.api.model.attachment.ErrandAttachmentPurpose;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class ArtefactAttachmentTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);

	}

	@Test
	void bean() {
		MatcherAssert.assertThat(ArtefactAttachment.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builder() {

		// Arrange
		final var attachmentId = "attachmentId";
		final var fileName = "fileName";
		final var mimeType = "application/pdf";
		final var fileSize = 40960;
		final var purpose = ErrandAttachmentPurpose.create().withId("5f79a808-0ef3-4985-99b9-b12f23e202a7").withName("RESPONSE").withDisplayName("Inkommen handling");
		final var sortOrder = 1;
		final var created = now();
		final var createdBy = "createdBy";

		// Act
		final var result = ArtefactAttachment.create()
			.withAttachmentId(attachmentId)
			.withFileName(fileName)
			.withMimeType(mimeType)
			.withFileSize(fileSize)
			.withPurpose(purpose)
			.withSortOrder(sortOrder)
			.withCreated(created)
			.withCreatedBy(createdBy);

		// Assert
		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getAttachmentId()).isEqualTo(attachmentId);
		assertThat(result.getFileName()).isEqualTo(fileName);
		assertThat(result.getMimeType()).isEqualTo(mimeType);
		assertThat(result.getFileSize()).isEqualTo(fileSize);
		assertThat(result.getPurpose()).isEqualTo(purpose);
		assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getCreatedBy()).isEqualTo(createdBy);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(ArtefactAttachment.create()).hasAllNullFieldsOrProperties();
		assertThat(new ArtefactAttachment()).hasAllNullFieldsOrProperties();
	}
}
