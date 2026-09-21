package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ArtefactAttachmentServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "b82bd8ac-1507-4d9a-958d-369261eecc15";
	private static final String ATTACHMENT_ID = "5f79a808-0ef3-4985-99b9-b12f23e202a7";

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private ErrandAttachmentService errandAttachmentServiceMock;

	@InjectMocks
	private ArtefactAttachmentService service;

	private AttachmentEntity mockAttachmentOnErrand() {
		final var attachmentEntity = AttachmentEntity.create()
			.withId(ATTACHMENT_ID)
			.withFileName("yttrande.pdf")
			.withPurpose(AttachmentPurposeEntity.create().withId("purposeId").withName("RESPONSE"));
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(attachmentEntity));
		return attachmentEntity;
	}

	/**
	 * What the answer says about the attachment, what it is for included, is read from the attachment itself.
	 */
	@Test
	void link() {

		// Arrange
		final var attachments = new ArrayList<AttachmentEntity>();
		final var attachmentEntity = mockAttachmentOnErrand();

		// Act
		final var result = service.link(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, attachments);

		// Verify
		assertThat(attachments).containsExactly(attachmentEntity);
		assertThat(result.getId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getFileName()).isEqualTo("yttrande.pdf");
		assertThat(result.getPurpose().getName()).isEqualTo("RESPONSE");
	}

	/**
	 * The attachment is fetched through the errand, and one belonging to another errand is answered as not found.
	 */
	@Test
	void linkingAnAttachmentOfAnotherErrandGivesNotFound() {

		// Arrange
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.empty());
		final var attachments = new ArrayList<AttachmentEntity>();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.link(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, attachments));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(ATTACHMENT_ID, ERRAND_ID);
		assertThat(attachments).isEmpty();
	}

	/**
	 * An attachment already linked is recognised by its id without regard to case.
	 */
	@Test
	void linkingTheSameAttachmentTwiceIsAConflict() {

		// Arrange
		final var attachments = new ArrayList<>(List.of(AttachmentEntity.create().withId(ATTACHMENT_ID.toUpperCase())));
		mockAttachmentOnErrand();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.link(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, attachments));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(attachments).hasSize(1);
	}

	@Test
	void uploadAndLink() {

		// Arrange
		final MultipartFile file = new MockMultipartFile("attachment", "yttrande.pdf", "application/pdf", "content".getBytes());
		final var attachments = new ArrayList<AttachmentEntity>();
		when(errandAttachmentServiceMock.createErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, file, null)).thenReturn(ATTACHMENT_ID);
		final var attachmentEntity = mockAttachmentOnErrand();

		// Act
		final var result = service.uploadAndLink(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, file, attachments);

		// Verify
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(attachments).containsExactly(attachmentEntity);
	}

	/**
	 * Unlinking takes the attachment out of the artefact and does nothing else - it stays on the errand.
	 */
	@Test
	void unlink() {

		// Arrange
		final var other = AttachmentEntity.create().withId("another-id");
		final var attachments = new ArrayList<>(List.of(AttachmentEntity.create().withId(ATTACHMENT_ID), other));

		// Act
		service.unlink(ATTACHMENT_ID, attachments);

		// Verify
		assertThat(attachments).containsExactly(other);
		verifyNoInteractions(attachmentRepositoryMock, errandAttachmentServiceMock);
	}

	@Test
	void unlinkingSomethingThatIsNotLinkedGivesNotFound() {

		// Arrange
		final var attachments = new ArrayList<>(List.of(AttachmentEntity.create().withId("another-id")));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.unlink(ATTACHMENT_ID, attachments));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(ATTACHMENT_ID);
		assertThat(attachments).hasSize(1);
	}

	/**
	 * An element without an id, or no element at all, is skipped and never matched on a null.
	 */
	@Test
	void anAttachmentWithoutAnIdIsNotMatched() {

		// Arrange
		final var attachments = new ArrayList<AttachmentEntity>();
		attachments.add(null);
		attachments.add(AttachmentEntity.create());

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.unlink(ATTACHMENT_ID, attachments));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(attachments).hasSize(2);
	}

	@Test
	void aNullCollectionIsTreatedAsEmpty() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.unlink(ATTACHMENT_ID, null));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
	}
}
