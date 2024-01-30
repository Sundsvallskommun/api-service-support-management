package se.sundsvall.supportmanagement.service.scheduler.webmessagecollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.supportmanagement.integration.db.CommunicationRepository;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.webmessagecollector.WebMessageCollectorClient;
import se.sundsvall.supportmanagement.integration.webmessagecollector.configuration.WebMessageCollectorProperties;

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

		webMessageCollectorProperties.familyIds().forEach(familyId -> {
			final var messages = webMessageCollectorClient.getMessages(familyId);
			LOG.info("Got {} messages from the WebMessageCollectorClient", messages.size());
			messages.forEach(messageDTO -> errandsRepository.findByExternalTagValue(messageDTO.getExternalCaseId())
				.ifPresent(errand -> communicationRepository.save(WebMessageCollectorMapper.toCommunicationEntity(messageDTO, errand.getErrandNumber()))));
		});
	}

}
