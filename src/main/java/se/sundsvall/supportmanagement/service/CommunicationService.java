package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toSmsRequest;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;

@Service
public class CommunicationService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final boolean ASYNCHRONOUSLY = true;

	@Autowired
	private ErrandsRepository repository;

	@Autowired
	private MessagingClient messagingClient;


	public List<Communication> readCommunications(final String namespace, final String municipalityId, final String id) {
		fetchEntity(id, namespace, municipalityId);

		return Collections.emptyList();
	}

	public void updateViewedStatus(final String namespace, final String municipalityId, final String id, final String messageID, final boolean isViewed) {
		fetchEntity(id, namespace, municipalityId);
	}

	public void getMessageAttachmentStreamed(final String attachmentID, final HttpServletResponse response) {

		// TODO: Implement this method

	}

	public void sendEmail(String namespace, String municipalityId, String id, EmailRequest request) {
		messagingClient.sendEmail(ASYNCHRONOUSLY, toEmailRequest(fetchEntity(id, namespace, municipalityId), request));
	}

	public void sendSms(String namespace, String municipalityId, String id, SmsRequest request) {
		messagingClient.sendSms(ASYNCHRONOUSLY, toSmsRequest(fetchEntity(id, namespace, municipalityId), request));
	}

	private ErrandEntity fetchEntity(String id, String namespace, String municipalityId) {
		if (!repository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}

		return repository.getReferenceById(id);
	}
}
