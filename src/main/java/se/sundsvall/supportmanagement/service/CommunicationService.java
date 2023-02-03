package se.sundsvall.supportmanagement.service;

import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.api.model.errand.CustomerType.EMPLOYEE;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toSmsRequest;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.isValidUuid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.errand.CustomerType;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;

@Service
public class CommunicationService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found";
	private static final String NOT_VALID_CUSTOMER_UUID = "Errand with id '%s' has an employee customer reference with other identifier than an uuid";

	@Autowired
	private ErrandsRepository repository;

	@Autowired
	private MessagingClient messagingClient;

	public void sendEmail(String id, EmailRequest request) {
		messagingClient.sendEmail(toEmailRequest(fetchEntity(id), request));
	}

	public void sendSms(String id, SmsRequest request) {
		messagingClient.sendSms(toSmsRequest(fetchEntity(id), request));
	}

	private ErrandEntity fetchEntity(String id) {
		final var entity = ofNullable(repository.getReferenceById(id))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ENTITY_NOT_FOUND, id)));

		if (EMPLOYEE == CustomerType.valueOf(entity.getCustomer().getType()) && !isValidUuid(entity.getCustomer().getId())) {
			throw Problem.valueOf(BAD_REQUEST, String.format(NOT_VALID_CUSTOMER_UUID, id));
		}

		return entity;
	}
}
