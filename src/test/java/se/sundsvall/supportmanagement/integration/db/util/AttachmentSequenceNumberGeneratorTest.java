package se.sundsvall.supportmanagement.integration.db.util;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.AttachmentSequenceRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AttachmentSequenceEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.SequenceNumberProjection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentSequenceNumberGeneratorTest {

	private static final String ERRAND_ID = "errandId";
	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private AttachmentSequenceRepository attachmentSequenceRepositoryMock;

	@Mock
	private AttachmentRepository attachmentRepositoryMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Mock
	private ErrandEntity errandReferenceMock;

	@InjectMocks
	private AttachmentSequenceNumberGenerator generator;

	private static ErrandEntity errand() {
		return ErrandEntity.create().withId(ERRAND_ID).withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);
	}

	@Test
	void startSequenceSavesAnEmptySequenceForTheErrand() {
		final var errand = errand();

		generator.startSequence(errand);

		final var captor = ArgumentCaptor.forClass(AttachmentSequenceEntity.class);
		verify(attachmentSequenceRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getErrandEntity()).isSameAs(errand);
		assertThat(captor.getValue().getLastSequenceNumber()).isZero();
		verifyNoInteractions(errandsRepositoryMock, attachmentRepositoryMock);
	}

	@Test
	void nextSequenceNumberContinuesFromLastGiven() {
		final var sequence = AttachmentSequenceEntity.create().withErrandId(ERRAND_ID).withLastSequenceNumber(4);
		when(attachmentSequenceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(sequence));

		final var result = generator.nextSequenceNumber(errand());

		assertThat(result).isEqualTo(5);
		assertThat(sequence.getLastSequenceNumber()).isEqualTo(5);
		final var inOrder = inOrder(errandsRepositoryMock, attachmentSequenceRepositoryMock);
		inOrder.verify(errandsRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		inOrder.verify(attachmentSequenceRepositoryMock).findByErrandId(ERRAND_ID);
		inOrder.verify(attachmentSequenceRepositoryMock).save(sequence);
		verifyNoInteractions(attachmentRepositoryMock);
	}

	@Test
	void nextSequenceNumberStartsAtOneForErrandWithoutAttachments() {
		when(attachmentSequenceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		when(attachmentRepositoryMock.findTopByErrandEntityIdOrderBySequenceNumberDesc(ERRAND_ID)).thenReturn(Optional.empty());
		when(errandsRepositoryMock.getReferenceById(ERRAND_ID)).thenReturn(errandReferenceMock);

		final var result = generator.nextSequenceNumber(errand());

		assertThat(result).isEqualTo(1);
		final var captor = ArgumentCaptor.forClass(AttachmentSequenceEntity.class);
		verify(attachmentSequenceRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getErrandEntity()).isSameAs(errandReferenceMock);
		assertThat(captor.getValue().getLastSequenceNumber()).isEqualTo(1);
	}

	@Test
	void nextSequenceNumberWithoutSequenceContinuesFromHighestExisting() {
		final SequenceNumberProjection highest = () -> 3;
		when(attachmentSequenceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		when(attachmentRepositoryMock.findTopByErrandEntityIdOrderBySequenceNumberDesc(ERRAND_ID)).thenReturn(Optional.of(highest));
		when(errandsRepositoryMock.getReferenceById(ERRAND_ID)).thenReturn(errandReferenceMock);

		final var result = generator.nextSequenceNumber(errand());

		assertThat(result).isEqualTo(4);
	}
}
