package se.sundsvall.supportmanagement.service;

import static org.zalando.problem.Status.NOT_FOUND;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import se.sundsvall.supportmanagement.api.model.message.Message;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;

@Service
public class MessageService {

	private static final String ERRAND_ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";

	private final ErrandsRepository errandsRepository;

	public MessageService(final ErrandsRepository errandsRepository) {this.errandsRepository = errandsRepository;}

	public List<Message> readMessages(final String namespace, final String municipalityId, final String id) {
		verifyExistingErrand(id, namespace, municipalityId);

		return Collections.emptyList();
	}

	public void updateViewedStatus(final String namespace, final String municipalityId, final String id, final String messageID, final boolean isViewed) {
		verifyExistingErrand(id, namespace, municipalityId);
	}

	public void getMessageAttachmentStreamed(final String attachmentID, final HttpServletResponse response) {

		// TODO: Implement this method

	}

	private void verifyExistingErrand(final String id, final String namespace, final String municipalityId) {
		if (!errandsRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(ERRAND_ENTITY_NOT_FOUND, id, namespace, municipalityId));
		}
	}

}
