package se.sundsvall.supportmanagement.service.scheduler.emailreader;

import static se.sundsvall.supportmanagement.service.scheduler.emailreader.ErrandNumberParser.parseSubject;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.EmailWorkerConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;

import generated.se.sundsvall.emailreader.Email;


@Service
public class EmailReaderWorker {

	private final EmailReaderClient emailReaderClient;

	private final ErrandService errandService;

	private final CommunicationService communicationService;

	private final EmailReaderMapper emailReaderMapper;

	private final ErrandsRepository errandRepository;

	private final EmailWorkerConfigRepository emailWorkerConfigRepository;

	public EmailReaderWorker(final EmailReaderClient emailReaderClient,
		final ErrandsRepository errandRepository, final ErrandService errandService, final CommunicationService communicationService,
		final EmailReaderMapper emailReaderMapper, final EmailWorkerConfigRepository emailWorkerConfigRepository) {
		this.emailReaderClient = emailReaderClient;
		this.errandRepository = errandRepository;
		this.errandService = errandService;
		this.communicationService = communicationService;
		this.emailReaderMapper = emailReaderMapper;
		this.emailWorkerConfigRepository = emailWorkerConfigRepository;
	}

	@Transactional
	public void getAndProcessEmails() {
		emailWorkerConfigRepository.findAll().stream()
			.filter(EmailWorkerConfigEntity::getEnabled)
			.forEach(this::processEmailConfig);
	}

	private void processEmailConfig(final EmailWorkerConfigEntity config) {
		emailReaderClient.getEmails(config.getMunicipalityId(), config.getNamespace())
			.forEach(email -> processEmail(email, config));
	}

	private void processEmail(final Email email, final EmailWorkerConfigEntity config) {

		final var errandNumber = parseSubject(email.getSubject());

		getErrand(errandNumber, email, config).ifPresent(errand -> {
			processErrand(errand, email, config);
			emailReaderClient.deleteEmail(email.getId());
		});

	}

	private Optional<ErrandEntity> getErrand(final String errandNumber, final Email email, final EmailWorkerConfigEntity config) {

		return Optional.ofNullable(errandNumber)
			.map(errandRepository::findByErrandNumber)
			.orElseGet(() -> errandRepository
				.findById(errandService.createErrand(
					config.getNamespace(),
					config.getMunicipalityId(),
					emailReaderMapper.toErrand(
						email,
						config.getStatusForNew(),
						config.isAddSenderAsStakeholder(),
						config.getStakeholderRole(),
						config.getErrandChannel()))));
	}

	private void processErrand(final ErrandEntity errand, final Email email, final EmailWorkerConfigEntity config) {

		if (isErrandInactive(errand, config)) {
			sendEmail(errand, email, config);
		} else if (config.getTriggerStatusChangeOn() != null && errand.getStatus().equals(config.getTriggerStatusChangeOn())) {
			errandService.updateErrand(errand.getNamespace(), errand.getMunicipalityId(), errand.getId(), Errand.create().withStatus(config.getStatusChangeTo()));
		}
		saveEmail(email, errand);
	}

	private void saveEmail(final Email email, final ErrandEntity errand) {

		final var communicationEntity = emailReaderMapper.toCommunicationEntity(email).withErrandNumber(errand.getErrandNumber());
		communicationService.saveCommunication(communicationEntity);
		communicationService.saveAttachment(communicationEntity, errand);
	}

	private boolean isErrandInactive(final ErrandEntity errand, final EmailWorkerConfigEntity config) {

		return config.getInactiveStatus() != null
			&& errand.getStatus().equals(config.getInactiveStatus())
			&& config.getDaysOfInactivityBeforeReject() != null
			&& errand.getTouched().isBefore(OffsetDateTime.now().minusDays(config.getDaysOfInactivityBeforeReject()));
	}

	private void sendEmail(final ErrandEntity errand, final Email email, final EmailWorkerConfigEntity config) {

		final var emailRequest = emailReaderMapper.createEmailRequest(email, config.getErrandClosedEmailSender(), config.getErrandClosedEmailTemplate());
		communicationService.sendEmail(config.getNamespace(), config.getMunicipalityId(), errand.getId(), emailRequest);
	}

}
