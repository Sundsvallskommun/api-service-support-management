package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.zalando.problem.Status.INSUFFICIENT_STORAGE;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailAttachments;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toEmailRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toMessagingMessageRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toSmsRequest;
import static se.sundsvall.supportmanagement.service.mapper.MessagingMapper.toWebMessageRequest;

import generated.se.sundsvall.employee.PortalPersonData;
import generated.se.sundsvall.messaging.Message;
import generated.se.sundsvall.messaging.MessageParty;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.zalando.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.communication.Communication;
import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.api.model.communication.SmsRequest;
import se.sundsvall.supportmanagement.api.model.communication.WebMessageRequest;
import se.sundsvall.supportmanagement.integration.citizen.CitizenIntegration;
import se.sundsvall.supportmanagement.integration.db.CommunicationAttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationAttachmentEntity;
import se.sundsvall.supportmanagement.integration.db.model.communication.CommunicationEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.EmailHeader;
import se.sundsvall.supportmanagement.integration.messaging.MessagingClient;
import se.sundsvall.supportmanagement.integration.messagingsettings.MessagingSettingsIntegration;
import se.sundsvall.supportmanagement.service.mapper.CommunicationMapper;
import se.sundsvall.supportmanagement.service.model.MessagingSettings;

@Service
public class CommunicationService {

	private static final String COMMUNICATION_NOT_FOUND = "Communication with id %s not found";
	private static final String ATTACHMENT_NOT_FOUND = "Communication attachment not found";
	private static final String ATTACHMENT_WITH_ERRAND_NUMBER_NOT_FOUND = "Communication attachment not found for this errand";
	private static final boolean ASYNCHRONOUSLY = false;

	private final AccessControlService accessControlService;
	private final CommunicationRepository communicationRepository;
	private final CommunicationAttachmentRepository communicationAttachmentRepository;
	private final ErrandAttachmentService errandAttachmentService;
	private final MessagingClient messagingClient;
	private final CommunicationMapper communicationMapper;
	private final Semaphore semaphore;
	private final EmployeeService employeeService;
	private final CitizenIntegration citizenIntegration;
	private final MessagingSettingsIntegration messagingSettingsIntegration;

	public CommunicationService(
		final AccessControlService accessControlService,
		final MessagingClient messagingClient,
		final CommunicationRepository communicationRepository,
		final CommunicationAttachmentRepository communicationAttachmentRepository,
		final CommunicationMapper communicationMapper,
		final ErrandAttachmentService errandAttachmentService,
		final Semaphore semaphore,
		final EmployeeService employeeService,
		final CitizenIntegration citizenIntegration, final MessagingSettingsIntegration messagingSettingsIntegration) {

		this.accessControlService = accessControlService;
		this.messagingClient = messagingClient;
		this.communicationRepository = communicationRepository;
		this.communicationAttachmentRepository = communicationAttachmentRepository;
		this.communicationMapper = communicationMapper;
		this.errandAttachmentService = errandAttachmentService;
		this.semaphore = semaphore;
		this.employeeService = employeeService;
		this.citizenIntegration = citizenIntegration;
		this.messagingSettingsIntegration = messagingSettingsIntegration;
	}

	public List<Communication> readCommunications(final String namespace, final String municipalityId, final String errandId) {
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);

		return communicationMapper.toCommunications(communicationRepository.findByErrandNumber(errand.getErrandNumber()));
	}

	public List<Communication> readExternalCommunications(final String namespace, final String municipalityId, final String errandId) {
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
		final var communications = communicationMapper.toCommunications(communicationRepository.findByErrandNumberAndInternal(errand.getErrandNumber(), false));
		communications.forEach(communication -> communication.setViewed(null));
		return communications;
	}

	public void updateViewedStatus(final String namespace, final String municipalityId, final String id, final String communicationId, final boolean isViewed) {
		accessControlService.verifyExistingErrandAndAuthorization(namespace, municipalityId, id, RW);

		final var message = communicationRepository
			.findById(communicationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, COMMUNICATION_NOT_FOUND.formatted(communicationId)));

		message.setViewed(isViewed);
		communicationRepository.save(message);
	}

	public void getMessageAttachmentStreamed(final String namespace, final String municipalityId, final String errandId, final String communicationId, final String attachmentId, final HttpServletResponse response) {
		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, R, RW);
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
			response.addHeader(CONTENT_TYPE, attachment.getMimeType());
			response.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"");
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
		final var errandEntity = accessControlService.getErrand(namespace, municipalityId, id, false, RW);
		sendEmail(errandEntity, request);
	}

	public void sendEmail(final ErrandEntity errandEntity, final EmailRequest request) {

		Optional.ofNullable(request.getEmailHeaders()).ifPresentOrElse(headers -> {
			if (!headers.containsKey(EmailHeader.MESSAGE_ID)) {
				headers.put(EmailHeader.MESSAGE_ID, List.of("<" + UUID.randomUUID() + "@" + errandEntity.getNamespace() + ">"));
			}
		}, () -> request.setEmailHeaders(Map.of(EmailHeader.MESSAGE_ID, List.of("<" + UUID.randomUUID() + "@" + errandEntity.getNamespace() + ">"))));

		final var errandAttachments = errandAttachmentService.findByNamespaceAndMunicipalityIdAndIdIn(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), request.getAttachmentIds());

		final var emailRequest = toEmailRequest(errandEntity, request, toEmailAttachments(errandAttachments));

		messagingClient.sendEmail(errandEntity.getMunicipalityId(), ASYNCHRONOUSLY, emailRequest);

		final var communicationEntity = communicationMapper.toCommunicationEntity(errandEntity.getNamespace(), errandEntity.getMunicipalityId(), request)
			.withErrandAttachments(errandAttachments)
			.withViewed(true)
			.withErrandNumber(errandEntity.getErrandNumber());

		saveCommunication(communicationEntity);
		saveAttachment(communicationEntity, errandEntity);

	}

	public void sendSms(final String namespace, final String municipalityId, final String id, final SmsRequest request) {

		final var entity = accessControlService.getErrand(namespace, municipalityId, id, false, RW);
		messagingClient.sendSms(municipalityId, ASYNCHRONOUSLY, toSmsRequest(entity, request));

		final var communicationEntity = communicationMapper.toCommunicationEntity(namespace, municipalityId, request)
			.withViewed(true)
			.withErrandNumber(entity.getErrandNumber());

		saveCommunication(communicationEntity);
		saveAttachment(communicationEntity, entity);
	}

	public void sendWebMessage(final String namespace, final String municipalityId, final String id, final WebMessageRequest request) {
		final var entity = accessControlService.getErrand(namespace, municipalityId, id, false, RW);
		final var errandAttachments = errandAttachmentService.findByNamespaceAndMunicipalityIdAndIdIn(namespace, municipalityId, request.getAttachmentIds());

		final var fullName = getFullName(municipalityId);

		final var identifier = Optional.ofNullable(Identifier.get())
			.map(Identifier::getValue)
			.orElse("UNKNOWN");

		final var communicationEntity = communicationMapper.toCommunicationEntity(namespace, municipalityId, entity.getErrandNumber(), request, fullName, identifier)
			.withViewed(true)
			.withErrandAttachments(errandAttachments);

		if (request.isDispatch()) {
			messagingClient.sendWebMessage(municipalityId, ASYNCHRONOUSLY, toWebMessageRequest(entity, request, errandAttachments, identifier));
		}

		saveCommunication(communicationEntity);
		saveAttachment(communicationEntity, entity);
	}

	String getFullName(final String municipalityId) {

		final var identifier = Identifier.get();

		if (isNull(identifier)) {
			return null;
		}

		return switch (identifier.getType()) {
			case null -> null;
			case AD_ACCOUNT -> getEmployeeName(municipalityId, identifier.getValue()); // If senderType is adAccount, use the senderId to get the employee name
			case PARTY_ID -> getCitizenName(municipalityId, identifier.getValue()); // If senderType is partyId, use the senderId to get the citizen name
			default -> null; // This should be unreachable, but if it happens, return null
		};
	}

	String getEmployeeName(final String municipalityId, final String adUser) {
		return Optional.ofNullable(adUser)
			.map(user -> employeeService.getEmployeeByLoginName(municipalityId, user))
			.map(PortalPersonData::getFullname)
			.orElse(null);
	}

	String getCitizenName(final String municipalityId, final String partyId) {
		return Optional.ofNullable(partyId)
			.map(party -> citizenIntegration.getCitizenName(municipalityId, party))
			.orElse(null);
	}

	public void saveAttachment(final CommunicationEntity communicationEntity, final ErrandEntity entity) {
		communicationMapper.toAttachments(communicationEntity)
			.forEach(attachmentEntity -> {
				attachmentEntity.withErrandEntity(entity);
				errandAttachmentService.createErrandAttachment(attachmentEntity, entity);
			});
	}

	public void saveCommunication(final CommunicationEntity communicationEntity) {
		communicationRepository.saveAndFlush(communicationEntity);
	}

	public void sendMessageNotification(final String municipalityId, final String namespace, final String errandId, final String departmentName) {

		final var errand = accessControlService.getErrand(namespace, municipalityId, errandId, false, RW);

		final var messagingSettings = messagingSettingsIntegration.getMessagingsettings(municipalityId, namespace, departmentName);

		sendMessageNotification(errand, messagingSettings);
	}

	public void sendMessageNotification(final ErrandEntity errandEntity, final MessagingSettings messagingSettings) {

		final var request = toMessagingMessageRequest(errandEntity, messagingSettings);

		final var partyId = Optional.ofNullable(request.getMessages())
			.map(List::getFirst)
			.map(Message::getParty)
			.map(MessageParty::getPartyId)
			.map(UUID::toString)
			.orElse(null);

		if (Identifier.get() != null && !Identifier.get().getValue().equalsIgnoreCase(partyId)) {
			final var message = messagingClient.sendMessage(errandEntity.getMunicipalityId(), request);

			if (message == null) {
				throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to create message notification");
			}
		}

	}

	@Transactional
	public void deleteAllCommunicationsByErrandNumber(final String errandNumber) {
		final var list = communicationRepository.findByErrandNumber(errandNumber);
		communicationRepository.deleteAll(list);
	}
}
