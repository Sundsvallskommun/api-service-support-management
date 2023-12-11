package se.sundsvall.supportmanagement.service;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toSmsRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.CommunicationAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.service.mapper.CommunicationMapper;

@Service
public class CommunicationService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";

	private static final boolean ASYNCHRONOUSLY = true;

	private final ErrandsRepository errandsRepository;

	private final MessagingClient messagingClient;

	private final CommunicationRepository communicationRepository;

	private final CommunicationAttachmentRepository communicationAttachmentRepository;

	public CommunicationService(final ErrandsRepository errandsRepository,
		final MessagingClient messagingClient, final CommunicationRepository communicationRepository,
		final CommunicationAttachmentRepository communicationAttachmentRepository) {
		this.errandsRepository = errandsRepository;
		this.messagingClient = messagingClient;
		this.communicationRepository = communicationRepository;
		this.communicationAttachmentRepository = communicationAttachmentRepository;
	}


	public List<Communication> readCommunications(final String namespace, final String municipalityId, final String id) {
		final var entity = fetchEntity(id, namespace, municipalityId);

		return CommunicationMapper.toCommunications(communicationRepository.findByErrandNumber(entity.getErrandNumber()));
	}

	public void updateViewedStatus(final String namespace, final String municipalityId, final String id, final String communicationId, final boolean isViewed) {
		fetchEntity(id, namespace, municipalityId);

		final var message = communicationRepository
			.findById(communicationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Communication with id %s not found".formatted(communicationId)));

		message.setViewed(isViewed);
		communicationRepository.save(message);
	}

	public void getMessageAttachmentStreamed(final String attachmentID, final HttpServletResponse response) {

		try {
			final var attachmentEntity = communicationAttachmentRepository
				.findById(attachmentID)
				.orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND, "MessageAttachment not found"));

			final var file = attachmentEntity.getAttachmentData().getFile();

			response.addHeader(CONTENT_TYPE, attachmentEntity.getContentType());
			response.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentEntity.getName() + "\"");
			response.setContentLength((int) file.length());
			StreamUtils.copy(file.getBinaryStream(), response.getOutputStream());
		} catch (final IOException | SQLException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "%s occurred when copying file with attachment id '%s' to response: %s".formatted(e.getClass().getSimpleName(), attachmentID, e.getMessage()));

		}
	}

	public void sendEmail(final String namespace, final String municipalityId, final String id, final EmailRequest request) {
		messagingClient.sendEmail(ASYNCHRONOUSLY, toEmailRequest(fetchEntity(id, namespace, municipalityId), request));
	}

	public void sendSms(final String namespace, final String municipalityId, final String id, final SmsRequest request) {
		messagingClient.sendSms(ASYNCHRONOUSLY, toSmsRequest(fetchEntity(id, namespace, municipalityId), request));
	}

	private ErrandEntity fetchEntity(final String id, final String namespace, final String municipalityId) {
		if (!errandsRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
		return errandsRepository.getReferenceById(id);
	}

}
