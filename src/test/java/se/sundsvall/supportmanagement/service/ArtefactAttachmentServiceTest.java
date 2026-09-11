package se.sundsvall.supportmanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.supportmanagement.api.model.errand.ArtefactAttachmentLink;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.StatementAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentPurposeEntity;
import se.sundsvall.supportmanagement.integration.db.model.StatementAttachmentEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
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

	private static final Function<AttachmentEntity, StatementAttachmentEntity> LINK_FACTORY = attachment -> StatementAttachmentEntity.create().withAttachmentEntity(attachment);

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private ErrandAttachmentService errandAttachmentServiceMock;

	@Mock
	private StatementAttachmentRepository linkRepositoryMock;

	@InjectMocks
	private ArtefactAttachmentService service;

	private static StatementAttachmentEntity linkTo(final String attachmentId) {
		return StatementAttachmentEntity.create().withAttachmentEntity(AttachmentEntity.create().withId(attachmentId));
	}

	private void mockAttachmentOnErrand() {
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.of(AttachmentEntity.create().withId(ATTACHMENT_ID).withFileName("yttrande.pdf")));
	}

	private void mockLinkSave() {
		when(linkRepositoryMock.save(any(StatementAttachmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void link() {

		// Arrange
		final var links = new ArrayList<StatementAttachmentEntity>();
		mockAttachmentOnErrand();
		mockLinkSave();

		// Act
		final var result = service.link(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, 2, links, LINK_FACTORY, linkRepositoryMock);

		// Verify
		assertThat(links).hasSize(1);
		assertThat(result.getAttachmentId()).isEqualTo(ATTACHMENT_ID);
		assertThat(result.getSortOrder()).isEqualTo(2);
	}

	/**
	 * The invariant JPA cannot express: the two foreign keys of a link know nothing about each other, so the attachment is
	 * fetched through the errand. One belonging to another errand is answered as the absence it is.
	 */
	@Test
	void linkingAnAttachmentOfAnotherErrandGivesNotFound() {

		// Arrange
		when(attachmentRepositoryMock.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID))
			.thenReturn(Optional.empty());
		final var links = new ArrayList<StatementAttachmentEntity>();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.link(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, null, links, LINK_FACTORY, linkRepositoryMock));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(problem.getMessage()).contains(ATTACHMENT_ID, ERRAND_ID);
		assertThat(links).isEmpty();
		verifyNoInteractions(linkRepositoryMock);
	}

	@Test
	void linkingTheSameAttachmentTwiceIsAConflict() {

		// Arrange
		final var links = new ArrayList<>(List.of(linkTo(ATTACHMENT_ID)));
		mockAttachmentOnErrand();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.link(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ATTACHMENT_ID, null, links, LINK_FACTORY, linkRepositoryMock));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(CONFLICT);
		assertThat(links).hasSize(1);
		verifyNoInteractions(linkRepositoryMock);
	}

	@Test
	void uploadAndLink() {

		// Arrange
		final MultipartFile file = new MockMultipartFile("attachment", "yttrande.pdf", "application/pdf", "content".getBytes());
		final var links = new ArrayList<StatementAttachmentEntity>();
		when(errandAttachmentServiceMock.createErrandAttachment(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, file, null)).thenReturn(ATTACHMENT_ID);
		mockAttachmentOnErrand();
		mockLinkSave();

		// Act
		final var result = service.uploadAndLink(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, file, 1, links, LINK_FACTORY, linkRepositoryMock);

		// Verify
		assertThat(result).isEqualTo(ATTACHMENT_ID);
		assertThat(links).hasSize(1);
	}

	@Test
	void update() {

		// Arrange
		final var link = linkTo(ATTACHMENT_ID).withSortOrder(1);
		final var links = new ArrayList<>(List.of(link));

		// Act
		final var result = service.update(ATTACHMENT_ID, ArtefactAttachmentLink.create().withSortOrder(5), links);

		// Verify
		assertThat(result.getSortOrder()).isEqualTo(5);
	}

	/**
	 * A patch says nothing about the fields it omits, so what it leaves out stands.
	 */
	@Test
	void updateLeavesOmittedFieldsAlone() {

		// Arrange
		final var link = linkTo(ATTACHMENT_ID).withSortOrder(1);
		final var links = new ArrayList<>(List.of(link));

		// Act
		final var result = service.update(ATTACHMENT_ID, ArtefactAttachmentLink.create(), links);

		// Verify
		assertThat(result.getSortOrder()).isEqualTo(1);
	}

	/**
	 * What the attachment is for is read from the attachment rather than from the link, since that is where it now lives.
	 */
	@Test
	void purposeIsReadFromTheAttachment() {

		// Arrange
		final var link = StatementAttachmentEntity.create()
			.withAttachmentEntity(AttachmentEntity.create().withId(ATTACHMENT_ID).withPurpose(AttachmentPurposeEntity.create().withId("purposeId").withName("RESPONSE")))
			.withSortOrder(1);
		final var links = new ArrayList<>(List.of(link));

		// Act
		final var result = service.update(ATTACHMENT_ID, ArtefactAttachmentLink.create().withSortOrder(2), links);

		// Verify
		assertThat(result.getPurpose().getName()).isEqualTo("RESPONSE");
		assertThat(result.getSortOrder()).isEqualTo(2);
	}

	@Test
	void updatingAnAttachmentThatIsNotLinkedGivesNotFound() {

		// Arrange
		final var links = new ArrayList<StatementAttachmentEntity>();

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class,
			() -> service.update(ATTACHMENT_ID, ArtefactAttachmentLink.create().withSortOrder(1), links));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		verifyNoInteractions(linkRepositoryMock);
	}

	/**
	 * Unlinking removes the link and nothing else - the attachment stays on the errand.
	 */
	@Test
	void unlink() {

		// Arrange
		final var links = new ArrayList<>(List.of(linkTo(ATTACHMENT_ID), linkTo("another-id")));

		// Act
		service.unlink(ATTACHMENT_ID, links);

		// Verify
		assertThat(links).hasSize(1);
		verifyNoInteractions(attachmentRepositoryMock);
	}

	@Test
	void unlinkingSomethingThatIsNotLinkedGivesNotFound() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.unlink(ATTACHMENT_ID, new ArrayList<StatementAttachmentEntity>()));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
	}

	/**
	 * A link whose attachment has gone is skipped rather than being matched on a null id.
	 */
	@Test
	void aLinkWithoutAnAttachmentIsNotMatched() {

		// Arrange
		final var links = new ArrayList<>(List.of(StatementAttachmentEntity.create()));

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.unlink(ATTACHMENT_ID, links));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(links).hasSize(1);
	}

	@Test
	void aNullCollectionIsTreatedAsEmpty() {

		// Act
		final var problem = catchThrowableOfType(ThrowableProblem.class, () -> service.unlink(ATTACHMENT_ID, null));

		// Verify
		assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
	}
}
