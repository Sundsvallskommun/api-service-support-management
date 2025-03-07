package se.sundsvall.supportmanagement.service;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.zalando.problem.Status.INSUFFICIENT_STORAGE;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailAttachments;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toSmsRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toWebMessageRequest;

import generated.se.sundsvall.employee.PortalPersonData;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.filter.ExecutingUserSupplier;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.integration.db.CommunicationAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.service.mapper.CommunicationMapper;

@Service
public class CommunicationService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String COMMUNICATION_NOT_FOUND = "Communication with id %s not found";
	private static final String ATTACHMENT_NOT_FOUND = "Communication attachment not found";
	private static final String ATTACHMENT_WITH_ERRAND_NUMBER_NOT_FOUND = "Communication attachment not found for this errand";
	private static final boolean ASYNCHRONOUSLY = true;
	private static final String UNKNOWN_AD_USER = "UNKNOWN";

	private final ErrandsRepository errandsRepository;
	private final CommunicationRepository communicationRepository;
	private final CommunicationAttachmentRepository communicationAttachmentRepository;
	private final ErrandAttachmentService errandAttachmentService;
	private final MessagingClient messagingClient;
	private final CommunicationMapper communicationMapper;
	private final Semaphore semaphore;
	private final ExecutingUserSupplier executingUserSupplier;
	private final EmployeeService employeeService;

	public CommunicationService(
		final ErrandsRepository errandsRepository,
		final MessagingClient messagingClient,
		final CommunicationRepository communicationRepository,
		final CommunicationAttachmentRepository communicationAttachmentRepository,
		final CommunicationMapper communicationMapper,
		final ErrandAttachmentService errandAttachmentService,
		final Semaphore semaphore,
		final ExecutingUserSupplier executingUserSupplier,
		final EmployeeService employeeService) {

		this.errandsRepository = errandsRepository;
		this.messagingClient = messagingClient;
		this.communicationRepository = communicationRepository;
		this.communicationAttachmentRepository = communicationAttachmentRepository;
		this.communicationMapper = communicationMapper;
		this.errandAttachmentService = errandAttachmentService;
		this.semaphore = semaphore;
		this.executingUserSupplier = executingUserSupplier;
		this.employeeService = employeeService;
	}

	public List<Communication> readCommunications(final String namespace, final String municipalityId, final String errandId) {
		final var errand = fetchErrand(errandId, namespace, municipalityId);

		return communicationMapper.toCommunications(communicationRepository.findByErrandNumber(errand.getErrandNumber()));
	}

	public List<Communication> readExternalCommunications(final String namespace, final String municipalityId, final String errandId) {
		final var errand = fetchErrand(errandId, namespace, municipalityId);
		final var communications = communicationMapper.toCommunications(communicationRepository.findByErrandNumberAndInternal(errand.getErrandNumber(), false));
		communications.forEach(communication -> communication.setViewed(null));
		return communications;
	}

	public void updateViewedStatus(final String namespace, final String municipalityId, final String id, final String communicationId, final boolean isViewed) {
		fetchErrand(id, namespace, municipalityId);

		final var message = communicationRepository
			.findById(communicationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, COMMUNICATION_NOT_FOUND.formatted(communicationId)));

		message.setViewed(isViewed);
		communicationRepository.save(message);
	}

	public void getMessageAttachmentStreamed(final String namespace, final String municipalityId, final String errandId, final String communicationId, final String attachmentId, final HttpServletResponse response) {
		final var errand = fetchErrand(errandId, namespace, municipalityId);
		final var communicationAttachment = communicationAttachmentRepository.findByNamespaceAndMunicipalityIdAndCommunicationEntityIdAndId(namespace, municipalityId, communicationId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND));

		if (!Objects.equals(communicationAttachment.getCommunicationEntity().getErrandNumber(), errand.getErrandNumber())) {
			throw Problem.valueOf(NOT_FOUND, ATTACHMENT_WITH_ERRAND_NUMBER_NOT_FOUND);
		}

		streamCommunicationAttachmentData(communicationAttachment, response);
	}

	void streamCommunicationAttachmentData(final CommunicationAttachmentEntity attachment, final HttpServletResponse response) {

		final var fileLength = attachment.getFileSize();

		if (fileLength == null || fileLength == 0) {
			throw Problem.valueOf(NOT_FOUND, "Attachment with id '%s' has no data".formatted(attachment.getId()));
		}

		try {
			if (!semaphore.tryAcquire(fileLength, 5, TimeUnit.SECONDS)) {
				throw Problem.valueOf(INSUFFICIENT_STORAGE, "Insufficient storage available to process the request.");
			}
			response.addHeader(CONTENT_TYPE, attachment.getContentType());
			response.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getName() + "\"");
			response.setContentLength(fileLength);
			StreamUtils.copy(attachment.getAttachmentData().getFile().getBinaryStream(), response.getOutputStream());
		} catch (final IOException | SQLException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "%s occurred when copying file with attachment id '%s' to response: %s".formatted(e.getClass().getSimpleName(), attachment.getId(), e.getMessage()));
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			semaphore.release(fileLength);
		}
	}

	public void sendEmail(final String namespace, final String municipalityId, final String id, final EmailRequest request) {

		final var errandEntity = fetchErrand(id, namespace, municipalityId);

		Optional.ofNullable(request.getEmailHeaders()).ifPresentOrElse(headers -> {
			if (!headers.containsKey(EmailHeader.MESSAGE_ID)) {
				headers.put(EmailHeader.MESSAGE_ID, List.of("<" + UUID.randomUUID() + "@" + namespace + ">"));
			}
		}, () -> request.setEmailHeaders(Map.of(EmailHeader.MESSAGE_ID, List.of("<" + UUID.randomUUID() + "@" + namespace + ">"))));

		final var errandAttachments = errandAttachmentService.findByNamespaceAndMunicipalityIdAndIdIn(namespace, municipalityId, request.getAttachmentIds());

		final var emailRequest = toEmailRequest(errandEntity, request, toEmailAttachments(errandAttachments));

		messagingClient.sendEmail(municipalityId, ASYNCHRONOUSLY, emailRequest);

		final var communicationEntity = communicationMapper.toCommunicationEntity(namespace, municipalityId, request)
			.withErrandAttachments(errandAttachments)
			.withErrandNumber(errandEntity.getErrandNumber());

		saveCommunication(communicationEntity);
		saveAttachment(communicationEntity, errandEntity);

	}

	public void sendSms(final String namespace, final String municipalityId, final String id, final SmsRequest request) {

		final var entity = fetchErrand(id, namespace, municipalityId);
		messagingClient.sendSms(municipalityId, ASYNCHRONOUSLY, toSmsRequest(entity, request));

		final var communicationEntity = communicationMapper.toCommunicationEntity(namespace, municipalityId, request)
			.withErrandNumber(entity.getErrandNumber());

		saveCommunication(communicationEntity);
		saveAttachment(communicationEntity, entity);
	}

	public void sendWebMessage(final String namespace, final String municipalityId, final String id, final WebMessageRequest request) {
		final var entity = fetchErrand(id, namespace, municipalityId);
		final var errandAttachments = errandAttachmentService.findByNamespaceAndMunicipalityIdAndIdIn(namespace, municipalityId, request.getAttachmentIds());
		var adUser = executingUserSupplier.getAdUser();

		if (UNKNOWN_AD_USER.equals(adUser)) {
			adUser = null;
		}
		final var fullName = getFullName(municipalityId, adUser);

		final var communicationEntity = communicationMapper.toCommunicationEntity(namespace, municipalityId, entity.getErrandNumber(), request, fullName, adUser)
			.withErrandAttachments(errandAttachments);

		messagingClient.sendWebMessage(municipalityId, ASYNCHRONOUSLY, toWebMessageRequest(entity, request, errandAttachments, adUser));

		saveCommunication(communicationEntity);
		saveAttachment(communicationEntity, entity);
	}

	private ErrandEntity fetchErrand(final String id, final String namespace, final String municipalityId) {
		if (!errandsRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
		return errandsRepository.findById(id).orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId)));
	}

	public void saveAttachment(final CommunicationEntity communicationEntity, final ErrandEntity entity) {
		communicationMapper.toAttachments(communicationEntity)
			.forEach(attachmentEntity -> {
				attachmentEntity.withErrandEntity(entity);
				errandAttachmentService.createErrandAttachment(attachmentEntity, entity);
			});
	}

	public void saveCommunication(final CommunicationEntity communicationEntity) {
		communicationRepository.save(communicationEntity);
	}

	private String getFullName(final String municipalityId, final String adUser) {
		return Optional.ofNullable(adUser)
			.map(user -> employeeService.getEmployeeByLoginName(municipalityId, user))
			.map(PortalPersonData::getFullname)
			.orElse(null);
	}

}
