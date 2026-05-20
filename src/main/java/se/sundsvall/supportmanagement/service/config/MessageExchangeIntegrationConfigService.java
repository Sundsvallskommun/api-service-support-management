package se.sundsvall.supportmanagement.service.config;

import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.MessageExchangeIntegration;
import se.sundsvall.supportmanagement.integration.db.MessageExchangeIntegrationConfigRepository;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.mapper.MessageExchangeIntegrationMapper.toEntity;
import static se.sundsvall.supportmanagement.service.mapper.MessageExchangeIntegrationMapper.toMessageExchangeIntegration;

@Service
public class MessageExchangeIntegrationConfigService {

	private static final String CONFIG_ENTITY_NOT_FOUND = "No config found in namespace '%s' for municipality '%s'";

	private final MessageExchangeIntegrationConfigRepository configRepository;
	private final NamespaceConfigRepository namespaceConfigRepository;

	public MessageExchangeIntegrationConfigService(
		final MessageExchangeIntegrationConfigRepository configRepository,
		final NamespaceConfigRepository namespaceConfigRepository) {
		this.configRepository = configRepository;
		this.namespaceConfigRepository = namespaceConfigRepository;
	}

	public void create(final MessageExchangeIntegration request, final String namespace, final String municipalityId) {
		if (!namespaceConfigRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Namespace config must be created before enabling message exchange integration config. Add via /namespaceConfig resource");
		}
		configRepository.save(toEntity(request, namespace, municipalityId));
	}

	public void replace(final MessageExchangeIntegration request, final String namespace, final String municipalityId) {
		final var entity = configRepository.getByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId)));

		final var replacement = toEntity(request, namespace, municipalityId)
			.withId(entity.getId())
			.withCreated(entity.getCreated());

		configRepository.save(replacement);
	}

	public MessageExchangeIntegration get(final String namespace, final String municipalityId) {
		final var entity = configRepository.getByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId)));
		return toMessageExchangeIntegration(entity);
	}

	public void delete(final String namespace, final String municipalityId) {
		if (!configRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId));
		}

		configRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}
}
