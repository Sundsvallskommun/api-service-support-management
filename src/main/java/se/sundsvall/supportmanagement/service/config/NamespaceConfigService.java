package se.sundsvall.supportmanagement.service.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static org.zalando.problem.Status.NOT_FOUND;

@Service
public class NamespaceConfigService {

	private static final String CACHE_NAME = "namespaceConfigCache";

	private static final String CONFIG_ENTITY_NOT_FOUND = "No config found in namespace '%s' for municipality '%s'";

	private NamespaceConfigRepository configRepository;
	private NamespaceConfigMapper mapper;

	public NamespaceConfigService(NamespaceConfigRepository configRepository, NamespaceConfigMapper mapper) {
		this.configRepository = configRepository;
		this. mapper = mapper;
	}

	public void create(NamespaceConfig request, String namespace, String municipalityId) {
		configRepository.save(mapper.toEntity(request, namespace, municipalityId));
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}")
	})
	public void replace(NamespaceConfig request, String namespace, String municipalityId) {
		var entity = configRepository.getByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId)));

		var replacement = mapper.toEntity(request, namespace, municipalityId)
			.withId(entity.getId())
			.withCreated(entity.getCreated());

		configRepository.save(replacement);
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public NamespaceConfig get(String namespace, String municipalityId) {
		var entity = configRepository.getByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId)));
		return mapper.toNamespaceConfig(entity);
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}")
	})
	public void delete(String namespace, String municipalityId) {
		if(configRepository.getByNamespaceAndMunicipalityId(namespace, municipalityId).isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, String.format(CONFIG_ENTITY_NOT_FOUND, namespace, municipalityId));
		}

		configRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

}
