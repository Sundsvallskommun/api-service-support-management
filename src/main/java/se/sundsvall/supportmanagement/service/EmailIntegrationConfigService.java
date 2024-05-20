package se.sundsvall.supportmanagement.service;

import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.EmailIntegration;
import se.sundsvall.supportmanagement.integration.db.EmailWorkerConfigRepository;
import se.sundsvall.supportmanagement.service.mapper.EmailIntegrationMapper;

import static org.zalando.problem.Status.NOT_FOUND;

@Service
public class EmailIntegrationConfigService {

	private static final String CONFIG_ENTITY_NOT_FOUND = "No config found in namespace '%s' for municipality '%s'";

	private EmailWorkerConfigRepository configRepository;
	private EmailIntegrationMapper mapper;


	public EmailIntegrationConfigService(EmailWorkerConfigRepository configRepository, EmailIntegrationMapper mapper) {
		this.configRepository = configRepository;
		this.mapper = mapper;
	}

	public void create(EmailIntegration request, String namespace, String municipalityId) {
		configRepository.save(mapper.toEntity(request, namespace, municipalityId));
	}

	public void replace(EmailIntegration request, String namespace, String municipalityId) {

		var entity = configRepository.getByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId)));

		var replacement = mapper.toEntity(request, namespace, municipalityId)
			.withId(entity.getId())
			.withCreated(entity.getCreated());

		configRepository.save(replacement);
	}

	public EmailIntegration get(String namespace, String municipalityId) {
		var entity = configRepository.getByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId)));
		return mapper.toEmailIntegration(entity);
	}

	public void delete(String namespace, String municipalityId) {
		if(!configRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId));
		}

		configRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}
}
