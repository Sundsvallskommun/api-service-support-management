package se.sundsvall.supportmanagement.service;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	ErrandsRepository errandsRepositoryMock;

	@InjectMocks
	private MessageService messageService;

	@Mock
	private HttpServletResponse servletResponseMock;

	@Test
	void readMessages() {

		// TODO extend this test when the method is implemented properly

		// Parameter values
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var id = randomUUID().toString();

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class)))
			.thenReturn(true);

		// Call
		final var response = messageService.readMessages(namespace, municipalityId, id);

		// Verification
		assertThat(response).isNotNull().isEmpty();

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class));
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void updateViewedStatus() {

		// TODO extend this test when the method is implemented properly

		// Parameter values
		final var namespace = "namespace";
		final var municipalityId = "2281";
		final var id = randomUUID().toString();
		final var messageID = randomUUID().toString();
		final var isViewed = true;

		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class)))
			.thenReturn(true);

		// Call
		messageService.updateViewedStatus(namespace, municipalityId, id, messageID, isViewed);

		// Verification
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(any(String.class), any(String.class), any(String.class));
		verifyNoMoreInteractions(errandsRepositoryMock);
	}

	@Test
	void getMessageAttachmentStreamed() {

		// TODO extend this test when the method is implemented properly
		// Parameter values
		final var attachmentID = randomUUID().toString();

		//Call
		messageService.getMessageAttachmentStreamed(attachmentID, servletResponseMock);

		// Verification
		verifyNoInteractions(errandsRepositoryMock);
	}

}
