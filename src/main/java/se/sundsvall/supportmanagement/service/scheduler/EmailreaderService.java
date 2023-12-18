package se.sundsvall.supportmanagement.service.scheduler;

import static se.sundsvall.supportmanagement.service.scheduler.ErrandNumberParser.parseSubject;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;

import se.sundsvall.supportmanagement.api.model.communication.EmailRequest;
import se.sundsvall.supportmanagement.integration.db.AttachmentRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.emailreader.EmailReaderClient;
import se.sundsvall.supportmanagement.integration.emailreader.configuration.EmailReaderProperties;
import se.sundsvall.supportmanagement.service.CommunicationService;
import se.sundsvall.supportmanagement.service.ErrandService;

import generated.se.sundsvall.emailreader.Email;

public class EmailreaderService {

	private static final String ERRAND_STATUS_SOLVED = "SOLVED";

	private static final String ERRAND_STATUS_ONGOING = "ONGOING";

	private final EmailReaderProperties emailReaderProperties;

	private final EmailReaderClient emailReaderClient;

	private final ErrandService errandService;

	private final CommunicationService communicationService;

	private final EmailReaderMapper emailReaderMapper;

	private final ErrandsRepository errandRepository;

	private final AttachmentRepository attachmentRepository;

	public EmailreaderService(final EmailReaderProperties emailReaderProperties, final EmailReaderClient emailReaderClient,
		final ErrandsRepository errandRepository, final ErrandService errandService, final CommunicationService communicationService,
		final EmailReaderMapper emailReaderMapper,
		final AttachmentRepository attachmentRepository) {
		this.emailReaderProperties = emailReaderProperties;
		this.emailReaderClient = emailReaderClient;
		this.errandRepository = errandRepository;
		this.errandService = errandService;
		this.communicationService = communicationService;
		this.emailReaderMapper = emailReaderMapper;
		this.attachmentRepository = attachmentRepository;
	}


	@Scheduled(initialDelayString = "${scheduler.emailreader.initialDelay}", fixedRateString = "${scheduler.emailreader.fixedRate}", timeUnit = TimeUnit.SECONDS)
	void getAndProcessEmails() {

		emailReaderClient.getEmails(emailReaderProperties.municipalityId(), emailReaderProperties.namespace())
			.forEach(this::processEmail);

	}

	private void processEmail(final Email email) {

		final var errandNumber = parseSubject(email.getSubject());

		getErrand(errandNumber, email).ifPresent(errand -> {
			processErrand(errand, email, errandNumber);
			emailReaderClient.deleteEmail(email.getId());
		});

	}

	private Optional<ErrandEntity> getErrand(final String errandNumber, final Email email) {

		return Optional.ofNullable(errandNumber)
			.map(errandRepository::findByErrandNumber)
			.orElseGet(() -> errandRepository
				.findById(errandService.createErrand(emailReaderProperties.namespace(),
					emailReaderProperties.municipalityId(), emailReaderMapper.toErrand(email))));
	}

	private void processErrand(final ErrandEntity errand, final Email email, final String errandNumber) {

		if (isErrandInactive(errand)) {
			sendEmail(errand, email);
		} else if (errand.getStatus().equals(ERRAND_STATUS_SOLVED)) {
			errand.setStatus(ERRAND_STATUS_ONGOING);
			errandRepository.save(errand);
		}
		saveEmail(email, errand, errandNumber);
	}

	private void saveEmail(final Email email, final ErrandEntity errand, final String errandNumber) {

		communicationService.saveCommunication(emailReaderMapper.toCommunicationEntity(email).withErrandNumber(errandNumber));
		attachmentRepository.saveAll(emailReaderMapper.toAttachments(email).stream()
			.map(attachment -> attachment.withErrandEntity(errand))
			.toList());
	}

	private boolean isErrandInactive(final ErrandEntity errand) {

		return errand.getStatus().equals(ERRAND_STATUS_SOLVED)
			&& errand.getTouched().isBefore(OffsetDateTime.now().minusDays(5));
	}

	private void sendEmail(final ErrandEntity errand, final Email email) {

		final var emailRequest = createEmailRequest(email);
		communicationService.sendEmail(emailReaderProperties.municipalityId(), emailReaderProperties.namespace(), errand.getId(), emailRequest);
	}

	private EmailRequest createEmailRequest(final Email email) {

		return EmailRequest.create()
			.withSubject(email.getSubject())
			.withRecipient(email.getSender())
			.withSender("noreply@sundsvall.se")
			.withMessage("""
				Ditt ärende är nu stängt på grund av saknad återkoppling. Vänligen återkom per telefon 060-19 15 00 eller via e-post kundcenter@sundsvall.se för att skapa nytt ärende.
				Detta e-postmeddelande går ej att svara på.""");
	}

}
