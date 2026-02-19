package se.sundsvall.supportmanagement.service.config;

import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static java.util.Objects.isNull;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

@Service
public class NamespaceConfigService {

	private static final String CACHE_NAME = "namespaceConfigCache";

	private static final String CONFIG_ENTITY_NOT_FOUND = "No config found in namespace '%s' for municipality '%s'";
	private static final String CONFIG_ENTITY_ALREADY_EXISTS = "Namespace '%s' already exists in municipality '%s'";

	private final NamespaceConfigRepository configRepository;
	private final NamespaceConfigMapper mapper;

	public NamespaceConfigService(NamespaceConfigRepository configRepository, NamespaceConfigMapper mapper) {
		this.configRepository = configRepository;
		this.mapper = mapper;
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}")
	})
	public void create(NamespaceConfig request, String namespace, String municipalityId) {
		if (configRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, CONFIG_ENTITY_ALREADY_EXISTS.formatted(namespace, municipalityId));
		}
		configRepository.save(mapper.toEntity(request, namespace, municipalityId));
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}")
	})
	public void replace(NamespaceConfig request, String namespace, String municipalityId) {
		final var entity = configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId)));

		final var replacement = mapper.toEntity(request, namespace, municipalityId)
			.withId(entity.getId())
			.withCreated(entity.getCreated());

		configRepository.save(replacement);
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public NamespaceConfig get(String namespace, String municipalityId) {
		final var entity = configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId)));
		return mapper.toNamespaceConfig(entity);
	}

	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #municipalityId}")
	public List<NamespaceConfig> findAll(String municipalityId) {
		final var entities = isNull(municipalityId) ? configRepository.findAll() : configRepository.findAllByMunicipalityId(municipalityId);
		return mapper.toNamespaceConfigs(entities);
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}")
	})
	public void delete(String namespace, String municipalityId) {
		if (configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId).isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId));
		}

		configRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}
}
