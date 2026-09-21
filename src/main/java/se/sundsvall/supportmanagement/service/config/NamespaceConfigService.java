package se.sundsvall.supportmanagement.service.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.supportmanagement.api.model.config.AccessDefinition;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.config.RoleFieldRestriction;
import se.sundsvall.supportmanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.supportmanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.AccessGrantScope;
import se.sundsvall.supportmanagement.integration.db.model.enums.EventSubType;
import se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor;
import se.sundsvall.supportmanagement.service.mapper.NamespaceConfigMapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.integration.db.util.ConfigPropertyExtractor.PROPERTY_PROCESS_CONSUMER;
import static se.sundsvall.supportmanagement.integration.pwalkt.configuration.PwAlktConfiguration.CLIENT_ID;

@Service
public class NamespaceConfigService {

	private static final String CACHE_NAME = "namespaceConfigCache";

	private static final String CONFIG_ENTITY_NOT_FOUND = "No config found in namespace '%s' for municipality '%s'";
	private static final String CONFIG_ENTITY_ALREADY_EXISTS = "Namespace '%s' already exists in municipality '%s'";
	private static final String ROLE_OCCURS_MORE_THAN_ONCE = "Role '%s' occurs more than once in role access";
	private static final String ROLE_NAME_IS_RESERVED = "Role '%s' is reserved and may not be used in role access";
	private static final String KEYS_NOT_ALLOWED = "Keys may not be set for field '%s' of '%s' as the field holds no keyed collection";
	private static final String DUPLICATE_GRANT = "'%s' occurs more than once as %s in '%s'";
	private static final String LEVEL_NOT_ALLOWED = "Level may not be set for field '%s' of '%s' as the field holds no keyed collection";
	private static final String LEVEL_NOT_SUPPORTED = "Level '%s' may not be set for field '%s' of '%s' as a field is held at read or read/write";
	private static final String DUPLICATE_PROCESS_TRIGGER = "'%s' occurs more than once among the process triggers";
	private static final String UNKNOWN_PROCESS_CONSUMER = "'%s' is not a known process consumer. The process consumer of a namespace is the address events are delivered to, and must be '%s'";
	private static final String PROCESS_CONSUMER_EXCLUDES_ACCESS_CONTROL = "Access control may not be active for a namespace with the process consumer '%s'. A process consumer is not an AD account, and the access mapper grants access to nothing else, so every read and write the process makes for the namespace would be denied";
	private static final String COMMAND_AS_PROCESS_TRIGGER = "'%s' is a command to the process rather than a change to the errand. Commands always reach the process and are never filtered by the process triggers, so it may not be listed among them";
	private static final String MISSING_PROCESS_TRIGGERS = "A namespace with the process consumer '%s' must list %s among its process triggers. Without ERRAND an errand given its process label after it was created never starts its process, and without DECISION a process waiting for a decision is never told that it has been made";

	/** The errand changes every process depends on being told about, in the order they are named when missing. */
	private static final List<EventSubType> REQUIRED_PROCESS_TRIGGERS = List.of(EventSubType.ERRAND, EventSubType.DECISION);

	private final NamespaceConfigRepository configRepository;
	private final NamespaceConfigMapper mapper;

	public NamespaceConfigService(NamespaceConfigRepository configRepository, NamespaceConfigMapper mapper) {
		this.configRepository = configRepository;
		this.mapper = mapper;
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'isAccessControlActive', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'getProcessConsumer', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'getProcessTriggers', #namespace, #municipalityId}")
	})
	public void create(NamespaceConfig request, String namespace, String municipalityId) {
		if (configRepository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(BAD_REQUEST, CONFIG_ENTITY_ALREADY_EXISTS.formatted(namespace, municipalityId));
		}
		validateAccessConfiguration(request);
		validateProcessConfiguration(request);
		final var config = mapper.toEntity(request, namespace, municipalityId);
		validateNoDuplicateGrants(config);
		configRepository.save(config);
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
	 * Verifies the process configuration of the namespace.
	 * <p>
	 * A trigger may be listed only once, and a command may not be listed at all. The consumer, when set, must be the one
	 * process engine the service delivers to, access control may not be active alongside it, and the namespace has to
	 * trigger on {@link #REQUIRED_PROCESS_TRIGGERS}.
	 */
	private void validateProcessConfiguration(NamespaceConfig request) {
		final var triggers = ofNullable(request.getProcessTriggers()).orElse(emptyList());
		final var seenTriggers = new HashSet<EventSubType>();

		triggers.stream()
			.filter(trigger -> !seenTriggers.add(trigger))
			.findFirst()
			.ifPresent(trigger -> {
				throw Problem.valueOf(BAD_REQUEST, DUPLICATE_PROCESS_TRIGGER.formatted(trigger));
			});

		triggers.stream()
			.filter(trigger -> nonNull(trigger) && trigger.isCommand())
			.findFirst()
			.ifPresent(trigger -> {
				throw Problem.valueOf(BAD_REQUEST, COMMAND_AS_PROCESS_TRIGGER.formatted(trigger));
			});

		ofNullable(request.getProcessConsumer()).ifPresent(consumer -> {
			if (!CLIENT_ID.equals(consumer)) {
				throw Problem.valueOf(BAD_REQUEST, UNKNOWN_PROCESS_CONSUMER.formatted(consumer, CLIENT_ID));
			}
			if (request.isAccessControl()) {
				throw Problem.valueOf(BAD_REQUEST, PROCESS_CONSUMER_EXCLUDES_ACCESS_CONTROL.formatted(consumer));
			}

			final var missing = REQUIRED_PROCESS_TRIGGERS.stream()
				.filter(required -> !triggers.contains(required))
				.map(EventSubType::name)
				.toList();
			if (!missing.isEmpty()) {
				throw Problem.valueOf(BAD_REQUEST, MISSING_PROCESS_TRIGGERS.formatted(consumer, String.join(" and ", missing)));
			}
		});
	}

	/**
	 * Rejects any grant the namespace would store twice.
	 * <p>
	 * Checked on the mapped rows, by scope, type and value as the unique constraint is, which catches a resource listed
	 * twice, the same resource listed at two levels, two field entries naming the same field, or a key repeated within one
	 * field. The level is left out of the comparison.
	 */
	private void validateNoDuplicateGrants(NamespaceConfigEntity config) {
		final var seen = new HashSet<String>();

		ofNullable(config.getAccessGrants()).orElse(emptyList()).stream()
			.filter(grant -> !seen.add(grant.getScope() + "|" + grant.getType() + "|" + grant.getValue()))
			.findFirst()
			.ifPresent(grant -> {
				throw Problem.valueOf(BAD_REQUEST, DUPLICATE_GRANT.formatted(grant.getValue(), grant.getType(), grant.getScope()));
			});
	}

	/**
	 * Verifies the field entries of one scope. Keys and a level may only be set for a field holding a keyed collection,
	 * and limited read is not accepted as the level of a field.
	 */
	private void validateFields(List<FieldAccess> fields, String scope) {
		final var applicable = ofNullable(fields).orElse(emptyList());

		applicable.stream()
			.filter(field -> !isEmpty(field.getKeys()))
			.filter(field -> isNull(field.getField()) || !field.getField().isKeyed())
			.findFirst()
			.ifPresent(field -> {
				throw Problem.valueOf(BAD_REQUEST, KEYS_NOT_ALLOWED.formatted(field.getField(), scope));
			});

		applicable.stream()
			.filter(field -> nonNull(field.getLevel()))
			.filter(field -> isNull(field.getField()) || !field.getField().isKeyed())
			.findFirst()
			.ifPresent(field -> {
				throw Problem.valueOf(BAD_REQUEST, LEVEL_NOT_ALLOWED.formatted(field.getField(), scope));
			});

		applicable.stream()
			.filter(field -> AccessLevel.LR == field.getLevel())
			.findFirst()
			.ifPresent(field -> {
				throw Problem.valueOf(BAD_REQUEST, LEVEL_NOT_SUPPORTED.formatted(field.getLevel(), field.getField(), scope));
			});
	}

	@Caching(evict = {
		@CacheEvict(value = CACHE_NAME, key = "{'get', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'findAll', #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'isAccessControlActive', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'getProcessConsumer', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'getProcessTriggers', #namespace, #municipalityId}")
	})
	public void replace(NamespaceConfig request, String namespace, String municipalityId) {
		validateAccessConfiguration(request);
		validateProcessConfiguration(request);
		final var entity = configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId)));

		final var replacement = mapper.toEntity(request, namespace, municipalityId)
			.withId(entity.getId())
			.withCreated(entity.getCreated());

		validateNoDuplicateGrants(replacement);
		configRepository.save(replacement);
	}

	/**
	 * Signals if access control is active for the namespace. A namespace with no configuration at all answers false. Cached
	 * in its own right.
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

	/**
	 * The process engine the namespace delivers its events to, or empty for a namespace that runs no process at all or has
	 * no configuration. Cached in its own right.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @return                the process consumer of the namespace, or empty if it has none
	 */
	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public Optional<String> getProcessConsumer(String namespace, String municipalityId) {
		return configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(entity -> ConfigPropertyExtractor.<String>getNullableValue(entity, PROPERTY_PROCESS_CONSUMER));
	}

	/**
	 * The event sub types that wake the process of the namespace, and an empty set for a namespace that has named none or
	 * has no configuration. Cached in its own right.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @return                the sub types configured as process triggers for the namespace
	 */
	@Cacheable(value = CACHE_NAME, key = "{#root.methodName, #namespace, #municipalityId}")
	public Set<EventSubType> getProcessTriggers(String namespace, String municipalityId) {
		return configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(mapper::toProcessTriggers)
			.map(Set::copyOf)
			.orElseGet(Collections::emptySet);
	}

	/**
	 * The values the access configuration accepts, resolved from the enums that are enforced. The same for every
	 * namespace.
	 */
	public AccessDefinition getAccessDefinition() {
		return mapper.toAccessDefinition();
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
		@CacheEvict(value = CACHE_NAME, key = "{'isAccessControlActive', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'getProcessConsumer', #namespace, #municipalityId}"),
		@CacheEvict(value = CACHE_NAME, key = "{'getProcessTriggers', #namespace, #municipalityId}")
	})
	public void delete(String namespace, String municipalityId) {
		if (configRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId).isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, CONFIG_ENTITY_NOT_FOUND.formatted(namespace, municipalityId));
		}

		configRepository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}
}
