package se.sundsvall.supportmanagement.service.config;

import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.config.RoleFieldRestriction;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.enums.AccessGrantScope;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NamespaceConfigService {

	private static final String CACHE_NAME = "namespaceConfigCache";

	private static final String CONFIG_ENTITY_NOT_FOUND = "No config found in namespace '%s' for municipality '%s'";
	private static final String CONFIG_ENTITY_ALREADY_EXISTS = "Namespace '%s' already exists in municipality '%s'";
	private static final String ROLE_OCCURS_MORE_THAN_ONCE = "Role '%s' occurs more than once in role access";
	private static final String ROLE_NAME_IS_RESERVED = "Role '%s' is reserved and may not be used in role access";
	private static final String KEYS_NOT_ALLOWED = "Keys may not be set for field '%s' of '%s' as the field holds no keyed collection";

	private final NamespaceConfigRepository configRepository;
	private final NamespaceConfigMapper mapper;

	public NamespaceConfigService(NamespaceConfigRepository configRepository, NamespaceConfigMapper mapper) {
		this.configRepository = configRepository;
		this.mapper = mapper;
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'isAccessControlActive', #namespace, #municipalityId}")
	})
	public void create(NamespaceConfig request, String namespace, String municipalityId) {
		if (configRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, CONFIG_ENTITY_ALREADY_EXISTS.formatted(namespace, municipalityId));
		}
		validateAccessConfiguration(request);
		configRepository.save(mapper.toEntity(request, namespace, municipalityId));
	}

	/**
	 * Verifies the access configuration. Keys only make sense for fields holding a keyed collection, a role may only
	 * appear once, and a role may not be named after one of the scopes this service resolves itself.
	 */
	private void validateAccessConfiguration(NamespaceConfig request) {
		final var restrictions = ofNullable(request.getRoleFieldRestrictions()).orElse(emptyList());

		final var duplicatedRole = restrictions.stream()
			.collect(groupingBy(restriction -> ofNullable(restriction.getRole()).map(String::toUpperCase).orElse(""), counting()))
			.entrySet().stream()
			.filter(entry -> entry.getValue() > 1)
			.map(Entry::getKey)
			.findFirst();

		if (duplicatedRole.isPresent()) {
			throw Problem.valueOf(BAD_REQUEST, ROLE_OCCURS_MORE_THAN_ONCE.formatted(duplicatedRole.get()));
		}

		// A role stored under a reserved scope would be read back as the limited read or reporter configuration of the
		// namespace, granting its fields to entirely different principals, so it is rejected rather than silently mutated.
		restrictions.stream()
			.map(RoleFieldRestriction::getRole)
			.filter(role -> EnumUtils.isValidEnumIgnoreCase(AccessGrantScope.class, role))
			.findFirst()
			.ifPresent(role -> {
				throw Problem.valueOf(BAD_REQUEST, ROLE_NAME_IS_RESERVED.formatted(role));
			});

		validateFields(ofNullable(request.getLimitedReadAccess()).map(LimitedReadAccess::getFields).orElse(null), "limitedReadAccess");
		validateFields(ofNullable(request.getReporterAccess()).map(ReporterAccess::getFields).orElse(null), "reporterAccess");
		restrictions.forEach(restriction -> validateFields(restriction.getFields(), restriction.getRole()));
	}

	/**
	 * Keys expose single entries of a collection, so they only make sense for a field holding one.
	 */
	private void validateFields(List<FieldAccess> fields, String scope) {
		ofNullable(fields).orElse(emptyList()).stream()
			.filter(field -> !isEmpty(field.getKeys()))
			.filter(field -> isNull(field.getField()) || !field.getField().isKeyed())
			.findFirst()
			.ifPresent(field -> {
				throw Problem.valueOf(BAD_REQUEST, KEYS_NOT_ALLOWED.formatted(field.getField(), scope));
			});
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'isAccessControlActive', #namespace, #municipalityId}")
	})
	public void replace(NamespaceConfig request, String namespace, String municipalityId) {
		validateAccessConfiguration(request);
		final var entity = configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId)));

		final var replacement = mapper.toEntity(request, namespace, municipalityId)
			.withId(entity.getId())
			.withCreated(entity.getCreated());

		configRepository.save(replacement);
	}

	/**
	 * Signals if access control is active for the namespace. A namespace with no configuration at all answers false rather
	 * than failing, since it cannot have access control switched on, which is also why this cannot simply delegate to
	 * {@link #get(String, String)}.
	 * <p>
	 * Cached in its own right, since it is asked on every request reaching a namespace scoped resource.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @return                true if access control is active
	 */
	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public boolean isAccessControlActive(String namespace, String municipalityId) {
		return configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(mapper::toNamespaceConfig)
			.map(NamespaceConfig::isAccessControl)
			.orElse(false);
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
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'isAccessControlActive', #namespace, #municipalityId}")
	})
	public void delete(String namespace, String municipalityId) {
		if (configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId).isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId));
		}

		configRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}
}
