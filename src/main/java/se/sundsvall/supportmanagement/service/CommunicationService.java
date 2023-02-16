package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toSmsRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;

@Service
public class CommunicationService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found for municipality with id '%s'";

	@Autowired
	private ErrandsRepository repository;

	@Autowired
	private MessagingClient messagingClient;

	public void sendEmail(String municipalityId, String id, EmailRequest request) {
		messagingClient.sendEmail(toEmailRequest(fetchEntity(municipalityId, id), request));
	}

	public void sendSms(String municipalityId, String id, SmsRequest request) {
		messagingClient.sendSms(toSmsRequest(fetchEntity(municipalityId, id), request));
	}

	private ErrandEntity fetchEntity(String municipalityId, String id) {
		if (!repository.existsByIdAndMunicipalityId(id, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, municipalityId));
		}

		return repository.getReferenceById(id);
	}
}
