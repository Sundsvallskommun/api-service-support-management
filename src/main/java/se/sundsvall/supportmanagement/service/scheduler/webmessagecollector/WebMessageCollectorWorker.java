package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import static se.sundsvall.supportmanagement.Constants.ERRAND_STATUS_ONGOING;
import static se.sundsvall.supportmanagement.Constants.ERRAND_STATUS_SOLVED;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorProperties;

import generated.se.sundsvall.webmessagecollector.MessageDTO;

@Component
public class WebMessageCollectorWorker {

	private static final Logger LOG = LoggerFactory.getLogger(WebMessageCollectorWorker.class);

	private final WebMessageCollectorClient webMessageCollectorClient;

	private final WebMessageCollectorProperties webMessageCollectorProperties;

	private final ErrandsRepository errandsRepository;

	private final CommunicationRepository communicationRepository;

	public WebMessageCollectorWorker(final WebMessageCollectorClient webMessageCollectorClient, final WebMessageCollectorProperties webMessageCollectorProperties, final ErrandsRepository errandsRepository, final CommunicationRepository communicationRepository) {
		this.webMessageCollectorClient = webMessageCollectorClient;
		this.webMessageCollectorProperties = webMessageCollectorProperties;
		this.errandsRepository = errandsRepository;
		this.communicationRepository = communicationRepository;
	}

	void fetchWebMessages() {
		webMessageCollectorProperties.familyIds().forEach(this::getWebMessages);
	}

	private void getWebMessages(final String familyId) {
		final var messages = webMessageCollectorClient.getMessages(familyId);
		LOG.info("Got {} messages from the WebMessageCollectorClient", messages.size());
		processMessages(messages);
	}

	private void processMessages(final List<MessageDTO> messages) {

		messages.forEach(messageDTO ->
			errandsRepository.findByExternalTagValue(messageDTO.getExternalCaseId())
				.filter(this::shouldBeUpdated)
				.ifPresent(errand ->
				{
					updateErrandStatus(errand);
					communicationRepository.save(WebMessageCollectorMapper.toCommunicationEntity(messageDTO, errand.getErrandNumber()));
				}));
	}

	private void updateErrandStatus(final ErrandEntity errand) {
		if (errand.getStatus().equals(ERRAND_STATUS_SOLVED)) {
			errand.setStatus(ERRAND_STATUS_ONGOING);
			errandsRepository.save(errand);
		}
	}

	private boolean shouldBeUpdated(final ErrandEntity errand) {
		return errand.getStatus().equals(ERRAND_STATUS_SOLVED)
			&& errand.getTouched().isAfter(OffsetDateTime.now().minusDays(5));
	}


}
