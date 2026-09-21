package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.access.AccessScope;
import se.sundsvall.supportmanagement.service.access.AccessSnapshot;
import se.sundsvall.supportmanagement.service.access.ErrandAccessResolution;
import se.sundsvall.supportmanagement.service.access.ErrandAccessSpecifications;
import se.sundsvall.supportmanagement.service.access.ErrandKeyAccess;
import se.sundsvall.supportmanagement.service.access.FieldAccessResolution;
import se.sundsvall.supportmanagement.service.access.KeyAccess;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant;
import se.sundsvall.supportmanagement.service.access.NamespaceGrantResolver;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.mapper.ErrandMapper;
import se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.access.NamespaceGrantResolver.adAccountOf;
import static se.sundsvall.supportmanagement.service.access.NamespaceGrantResolver.highestLevel;
import static se.sundsvall.supportmanagement.service.access.NamespaceGrantResolver.toFieldGrants;
import static se.sundsvall.supportmanagement.service.access.NamespaceGrantResolver.writableFields;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

@Component
public class AccessControlService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ENTITY_NOT_ACCESSIBLE = "Errand not accessible by user '%s'";
	private static final String KEY_NOT_ACCESSIBLE = "Key '%s' not accessible by user '%s'";
	private static final String FIELD_NOT_WRITABLE = "Field '%s' not writable by user '%s'";
	private static final String KEY_NOT_WRITABLE = "Key '%s' not writable by user '%s'";
	private static final String RESOURCE_NOT_ACCESSIBLE = "Resource '%s' not accessible by user '%s'";

	private final AccessMapperService accessMapperService;
	private final NamespaceConfigService namespaceConfigService;
	private final ErrandsRepository errandsRepository;

	public AccessControlService(final AccessMapperService accessMapperService, final NamespaceConfigService namespaceConfigService, final ErrandsRepository errandsRepository) {
		this.accessMapperService = accessMapperService;
		this.namespaceConfigService = namespaceConfigService;
		this.errandsRepository = errandsRepository;
	}

	/**
	 * Resolves which fields of an errand the requesting user may see.
	 * <p>
	 * An errand their labels only grant limited read for is trimmed to the limited read fields of the namespace, whatever
	 * roles the user holds, since a role says what they see of errands they properly have access to. That trimming does not
	 * depend on role
	 * based mapping, as limited read may never silently mean full read. Role field restrictions, on the other hand, only
	 * apply while the namespace maps errands per role.
	 * <p>
	 * Fields given to the reporter of an errand union on top of whatever restriction applies, so someone who both reported
	 * an errand and handles it keeps the fuller view. They never restrict a user nothing else restricts, since reporting an
	 * errand may not
	 * reduce what its reporter sees. A reporter no label of theirs reaches the errand through is held to the reporter
	 * fields alone, since limited read was never granted to them and so has nothing to add: the two grants are independent,
	 * and either may be the narrower one.
	 * <p>
	 * A null result means no restriction applies at all and the errand is mapped in full, which is what an unrestricted
	 * role yields. An empty result, in contrast, is a restriction resolving to no fields whatsoever. A limited read never
	 * resolves to
	 * nothing, since a namespace that has not said what limited read exposes falls back to a built in minimum.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @return                resolver of fields, and the keys to limit them to, per errand
	 */
	public Function<ErrandEntity, Map<ErrandField, Set<String>>> roleBasedFieldResolver(String namespace, String municipalityId, Identifier user) {
		return fieldAccessResolver(namespace, municipalityId, user).andThen(FieldAccessResolution::readable);
	}

	/**
	 * Resolves what the user may read of an errand and what of it they may write, in one pass.
	 * <p>
	 * The two answer different questions and a caller needing both should ask once: a field grant may hold a field to
	 * read while the errand itself is writable, so the keys a caller may see are not always the keys they may change.
	 * What may be written is by construction a subset of what may be read - a level on a grant only ever restricts it
	 * further, and a grant carrying no level simply follows the errand, which is what every grant did before levels
	 * existed.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @return                resolver of what the user may read and write, per errand
	 */
	public Function<ErrandEntity, FieldAccessResolution> fieldAccessResolver(String namespace, String municipalityId, Identifier user) {
		final var config = namespaceConfigService.get(namespace, municipalityId);

		// Nothing restricts anyone while the namespace has not opted in, so the access mapper is never asked for it.
		if (!config.isAccessControl()) {
			return _ -> FieldAccessResolution.unrestricted();
		}

		return NamespaceGrantResolver.fieldAccessResolver(config, accessMapperService.getAccessSnapshot(municipalityId, namespace, user), adAccountOf(user));
	}

	/**
	 * Reports what the user may do with one errand, so that a client can render only the controls their next request
	 * would actually be allowed to make.
	 * <p>
	 * Answered from the same grants the write paths enforce rather than from a second reading of the configuration:
	 * {@link #highestLevel} mirrors the specification guarding every endpoint, and the fields come from the very
	 * resolver {@code readErrand} maps its response with, so what is reported and what is served cannot drift apart.
	 * <p>
	 * The configuration and the access snapshot are resolved once and the fields once, since both underlying lookups are
	 * cached per request at best and resolving them twice is what quietly turns one read into several.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  errandEntity   errand to report on
	 * @return                what the user holds the errand, its fields and its resources at
	 */
	public ErrandAccessResolution resolveErrandAccess(String namespace, String municipalityId, Identifier user, ErrandEntity errandEntity) {
		final var config = namespaceConfigService.get(namespace, municipalityId);
		final var access = config.isAccessControl() ? accessMapperService.getAccessSnapshot(municipalityId, namespace, user) : AccessSnapshot.empty();
		final var adAccount = adAccountOf(user);

		final var errandLevel = highestLevel(config, access, errandEntity, adAccount, ProtectedResource.ERRAND);

		if (isNull(errandLevel)) {
			throw Problem.valueOf(FORBIDDEN, ENTITY_NOT_ACCESSIBLE.formatted(ofNullable(user)
				.map(Identifier::getValue)
				.orElse(null)));
		}

		// The errand itself is reported as the level of the whole answer, so it is left out here rather than repeated and
		// left free to disagree with it.
		final Map<ProtectedResource, Access.AccessLevelEnum> resources = new EnumMap<>(ProtectedResource.class);
		Arrays.stream(ProtectedResource.values())
			.filter(ProtectedResource::isErrandScoped)
			.filter(resource -> ProtectedResource.ERRAND != resource)
			.forEach(resource -> ofNullable(highestLevel(config, access, errandEntity, adAccount, resource))
				.ifPresent(level -> resources.put(resource, level)));

		// Built from the snapshot already resolved above, so that what is reported of the fields and what is reported of
		// the errand cannot come from two different answers of the access mapper.
		final var resolution = config.isAccessControl()
			? NamespaceGrantResolver.fieldAccessResolver(config, access, adAccount).apply(errandEntity)
			: FieldAccessResolution.unrestricted();

		final var fields = toFieldGrants(resolution, writableFields(errandLevel, resources));

		return new ErrandAccessResolution(errandLevel, resources, fields);
	}

	/**
	 * Tells which keys of a keyed field the user may read on sent in errand, so that the endpoints serving that data on its
	 * own honour the same grants as the errand payload does.
	 * <p>
	 * Every key is readable whenever the whole errand is returned, which is the case for a user nothing restricts. A field
	 * the applicable restriction does not expose at all yields no readable keys.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  errandEntity   errand the field belongs to
	 * @param  field          field to read
	 * @return                predicate accepting the keys the user may read
	 */
	public Predicate<String> readableKeyPredicate(String namespace, String municipalityId, Identifier user, ErrandEntity errandEntity, ErrandField field) {
		return fieldAccessResolver(namespace, municipalityId, user).apply(errandEntity).readableKey(field);
	}

	/**
	 * Tells which keys of a keyed field the user may change on sent in errand.
	 * <p>
	 * Narrower than {@link #readableKeyPredicate} exactly where the namespace holds a field or a key to read, and the
	 * same answer everywhere else.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  errandEntity   errand the field belongs to
	 * @param  field          field to write
	 * @return                predicate accepting the keys the user may change
	 */
	public Predicate<String> writableKeyPredicate(String namespace, String municipalityId, Identifier user, ErrandEntity errandEntity, ErrandField field) {
		return fieldAccessResolver(namespace, municipalityId, user).apply(errandEntity).writableKey(field);
	}

	/**
	 * The same answer as {@link #readableKeyPredicate}, for every keyed field of one errand at once. A request touching
	 * several fields resolves the grants once instead of once per field, which matters since resolving them queries the
	 * database and would
	 * otherwise flush a half updated errand mid transaction.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  errandEntity   errand the fields belong to
	 * @return                resolver of the predicate accepting the keys the user may read, per field
	 */
	public Function<ErrandField, Predicate<String>> readableKeyResolver(String namespace, String municipalityId, Identifier user, ErrandEntity errandEntity) {
		return fieldAccessResolver(namespace, municipalityId, user).apply(errandEntity)::readableKey;
	}

	/**
	 * Throws 401 unless the user may reach sent in key of sent in field. A key the user cannot read is also a key they
	 * cannot write, so that no one can overwrite or remove data they are not allowed to see. The converse does not hold:
	 * a key they may read is not necessarily one they may change, which {@link #verifyWritableKeys} answers.
	 */
	public void verifyAccessibleKey(String namespace, String municipalityId, ErrandEntity errandEntity, ErrandField field, String key) {
		verifyAccessibleKeys(namespace, municipalityId, errandEntity, field, List.of(key));
	}

	/**
	 * Throws 401 unless the user may reach every one of sent in keys. Resolves the grants once, so it stays a single pass
	 * regardless of how many keys a request carries.
	 */
	public void verifyAccessibleKeys(String namespace, String municipalityId, ErrandEntity errandEntity, ErrandField field, Collection<String> keys) {
		if (isNull(keys)) {
			return;
		}

		verifyAccessibleKeys(readableKeyPredicate(namespace, municipalityId, Identifier.get(), errandEntity, field), keys);
	}

	/**
	 * Throws 401 unless the user may reach every one of sent in keys, according to an already resolved predicate. Lets a
	 * caller needing the predicate itself resolve the grants once instead of once per use.
	 */
	public void verifyAccessibleKeys(Predicate<String> accessibleKey, Collection<String> keys) {
		if (isNull(keys)) {
			return;
		}

		keys.stream()
			.filter(key -> !accessibleKey.test(key))
			.findFirst()
			.ifPresent(key -> {
				throw Problem.valueOf(FORBIDDEN, KEY_NOT_ACCESSIBLE.formatted(key, getCallerIdentity()));
			});
	}

	/**
	 * Throws 401 unless the user may change every one of sent in keys, according to an already resolved predicate.
	 * <p>
	 * Sent in keys are the ones a request would actually change, not every key it carries: a namespace holding a key to
	 * read leaves it readable, so a caller patching back what they were served may name it as long as they leave it as
	 * it stands.
	 */
	public void verifyWritableKeys(Predicate<String> writableKey, Collection<String> keys) {
		if (isNull(keys)) {
			return;
		}

		keys.stream()
			.filter(key -> !writableKey.test(key))
			.findFirst()
			.ifPresent(key -> {
				throw Problem.valueOf(FORBIDDEN, KEY_NOT_WRITABLE.formatted(key, getCallerIdentity()));
			});
	}

	/**
	 * Throws 401 unless the user may change sent in key of sent in field, resolving the grants for it.
	 */
	public void verifyWritableKey(String namespace, String municipalityId, ErrandEntity errandEntity, ErrandField field, String key) {
		verifyWritableKeys(writableKeyPredicate(namespace, municipalityId, Identifier.get(), errandEntity, field), List.of(key));
	}

	/**
	 * Verifies every keyed field of sent in patch against what the caller may reach on the errand, and answers with what
	 * the merge and the response need.
	 * <p>
	 * Two questions are asked of each field. A key the caller cannot see at all is refused outright, whichever endpoint
	 * they write it through. A key they may see but not change is refused only when the patch would actually change it,
	 * since a caller patching back what they were served carries it unchanged.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  errandEntity   errand as it stands, before the patch is applied
	 * @param  patch          patch to apply
	 * @return                the keys the caller may change, and the resolver mapping the response
	 */
	public ErrandKeyAccess verifyKeyAccess(String namespace, String municipalityId, ErrandEntity errandEntity, Errand patch) {
		final var resolver = fieldAccessResolver(namespace, municipalityId, Identifier.get());
		final var access = resolver.apply(errandEntity);

		verifyKeys(access, ErrandField.PARAMETERS, keysOf(patch.getParameters(), Parameter::getKey),
			ErrandParameterMapper.changedKeys(errandEntity, patch.getParameters()));
		verifyKeys(access, ErrandField.JSON_PARAMETERS, keysOf(patch.getJsonParameters(), JsonParameter::getKey),
			ErrandMapper.changedJsonParameterKeys(errandEntity, patch.getJsonParameters()));
		verifyKeys(access, ErrandField.EXTERNAL_TAGS, keysOf(patch.getExternalTags(), ExternalTag::getKey),
			ErrandMapper.changedExternalTagKeys(errandEntity, patch.getExternalTags()));

		verifyWritableFields(access, patch);

		return new ErrandKeyAccess(access::writableKey, resolver.andThen(FieldAccessResolution::readable));
	}

	/**
	 * The same two questions for the parameters of one errand, asked by the endpoint replacing them wholesale.
	 */
	public KeyAccess verifyParameterAccess(String namespace, String municipalityId, ErrandEntity errandEntity, List<Parameter> parameters) {
		final var access = fieldAccessResolver(namespace, municipalityId, Identifier.get()).apply(errandEntity);

		verifyKeys(access, ErrandField.PARAMETERS, keysOf(parameters, Parameter::getKey),
			ErrandParameterMapper.changedKeys(errandEntity, parameters));

		return toKeyAccess(access, ErrandField.PARAMETERS);
	}

	/**
	 * The same two questions for a single parameter, which the endpoint writing one asks of its values alone.
	 */
	public KeyAccess verifyParameterAccess(String namespace, String municipalityId, ErrandEntity errandEntity, String key, List<String> values) {
		final var access = fieldAccessResolver(namespace, municipalityId, Identifier.get()).apply(errandEntity);

		verifyKeys(access, ErrandField.PARAMETERS, List.of(key), ErrandParameterMapper.changedValueKeys(errandEntity, key, values));

		return toKeyAccess(access, ErrandField.PARAMETERS);
	}

	/**
	 * The same two questions for a single json parameter.
	 */
	public KeyAccess verifyJsonParameterAccess(String namespace, String municipalityId, ErrandEntity errandEntity, String key, JsonParameter jsonParameter) {
		final var access = fieldAccessResolver(namespace, municipalityId, Identifier.get()).apply(errandEntity);

		verifyKeys(access, ErrandField.JSON_PARAMETERS, List.of(key), ErrandMapper.changedJsonParameterKeys(errandEntity, List.of(JsonParameter.create()
			.withKey(key)
			.withSchemaId(jsonParameter.getSchemaId())
			.withValue(jsonParameter.getValue()))));

		return toKeyAccess(access, ErrandField.JSON_PARAMETERS);
	}

	/**
	 * Refuses a patch naming a field its sender does not hold.
	 * <p>
	 * A field carries no level of its own unless it is keyed - a namespace may not hold a whole field to read, which
	 * {@code validateFields} refuses - so a field that is not keyed is theirs to read and to write, or not theirs at
	 * all. A value for one they do not hold is therefore a value they were never served, and is refused rather than
	 * quietly dropped: a patch that is half applied is worse to debug than one that is turned away.
	 * <p>
	 * The keyed fields are left to {@link #verifyKeys}, which weighs them key by key.
	 */
	private void verifyWritableFields(FieldAccessResolution access, Errand patch) {
		// A null map is a user nothing restricts, who holds every field.
		if (isNull(access.writable())) {
			return;
		}

		ErrandMapper.fieldReaders().forEach((field, read) -> {
			if (!field.isKeyed() && !access.writable().containsKey(field) && nonNull(read.apply(patch))) {
				throw Problem.valueOf(FORBIDDEN, FIELD_NOT_WRITABLE.formatted(field.getPropertyName(), getCallerIdentity()));
			}
		});
	}

	private void verifyKeys(FieldAccessResolution access, ErrandField field, Collection<String> present, Collection<String> changed) {
		verifyAccessibleKeys(access.readableKey(field), present);
		verifyWritableKeys(access.writableKey(field), changed);
	}

	private static KeyAccess toKeyAccess(FieldAccessResolution access, ErrandField field) {
		return new KeyAccess(access.readableKey(field), access.writableKey(field));
	}

	/**
	 * Keys of a keyed collection of a patch, or null when the patch leaves the collection alone.
	 */
	private static <T> List<String> keysOf(List<T> values, Function<T, String> keyExtractor) {
		return ofNullable(values)
			.map(list -> list.stream().map(keyExtractor).toList())
			.orElse(null);
	}

	/**
	 * Resolves which errands the requesting user reaches for sent in resource at the required level.
	 * <p>
	 * Labels say which errands the user reaches, the access mapper resources say which operations they may perform at all,
	 * and both must allow. Errands reported by the user are reached besides, which their labels may say nothing at all
	 * about.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  resource       resource being guarded
	 * @param  required       lowest access level accepted for the operation
	 * @return                the scope, unrestricted if access control is not enabled on the namespace
	 */
	public AccessScope accessScope(String namespace, String municipalityId, Identifier user, ProtectedResource resource, Access.AccessLevelEnum required) {
		final var config = namespaceConfigService.get(namespace, municipalityId);

		if (!config.isAccessControl()) {
			return AccessScope.UNRESTRICTED;
		}

		return NamespaceGrantResolver.accessScope(config, accessMapperService.getAccessSnapshot(municipalityId, namespace, user), user, resource, required);
	}

	/**
	 * Resolves what the user holds in the namespace at the required level, see {@link NamespaceGrant}. One configuration
	 * and one snapshot answer for the errand, for every errand scoped resource and for the fields, so that a caller
	 * rendering all of them cannot render them from different moments.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  required       lowest access level accepted for the operation
	 * @return                the grant, unrestricted if access control is not enabled on the namespace
	 */
	public NamespaceGrant namespaceGrant(String namespace, String municipalityId, Identifier user, Access.AccessLevelEnum required) {
		final var config = namespaceConfigService.get(namespace, municipalityId);
		final var access = config.isAccessControl() ? accessMapperService.getAccessSnapshot(municipalityId, namespace, user) : AccessSnapshot.empty();
		return NamespaceGrantResolver.namespaceGrant(config, access, user, required);
	}

	/**
	 * Creates specification filter ensuring user has access to sent in resource at the required level, see
	 * {@link #accessScope(String, String, Identifier, ProtectedResource, Access.AccessLevelEnum)}.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  resource       resource being guarded
	 * @param  required       lowest access level accepted for the operation
	 * @return                specification if access control is enabled on namespace, conjunction otherwise
	 */
	public Specification<ErrandEntity> withAccessControl(String namespace, String municipalityId, Identifier user, ProtectedResource resource, Access.AccessLevelEnum required) {
		return ErrandAccessSpecifications.withAccessControl(accessScope(namespace, municipalityId, user, resource, required));
	}

	/**
	 * Verifies that the requesting user may reach a resource belonging to the namespace itself rather than to any errand,
	 * such as its configuration or its metadata. Labels say nothing about these, so the access mapper resources decide on
	 * their own.
	 * <p>
	 * Enforced whenever access control is active for the namespace. A namespace without configuration enforces nothing,
	 * since access control cannot be active without it, which is also what lets a configuration be created in the first
	 * place. Because the
	 * check reads the persisted configuration, switching access control off is itself guarded.
	 *
	 * @param namespace      namespace
	 * @param municipalityId municipality id
	 * @param resource       resource being guarded
	 * @param required       lowest access level accepted for the operation
	 */
	public void verifyNamespaceAuthorization(final String namespace, final String municipalityId, final ProtectedResource resource, final Access.AccessLevelEnum required) {
		if (!namespaceConfigService.isAccessControlActive(namespace, municipalityId)) {
			return;
		}

		// A resource of the namespace is guarded on the grant alone, so the grant always applies here - unlike the
		// resources of an errand, which a namespace may leave to its labels by switching resource access control off.
		final var grant = new NamespaceGrantResolver.ResourceGrant(true, accessMapperService.getAccessSnapshot(municipalityId, namespace, Identifier.get()).resources().get(resource));

		if (!grant.permits(required)) {
			throw Problem.valueOf(FORBIDDEN, RESOURCE_NOT_ACCESSIBLE.formatted(resource, getCallerIdentity()));
		}
	}

	/**
	 * Fetches ErrandEntity and checks user access, if enabled in namespace.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  errandId       errand id
	 * @param  lock           db row locking enable if true
	 * @param  resource       resource being guarded
	 * @param  required       lowest access level accepted for the operation
	 * @return                errand entity
	 */
	public ErrandEntity getErrand(final String namespace, final String municipalityId, final String errandId, boolean lock, ProtectedResource resource, Access.AccessLevelEnum required) {
		verifyExistingErrand(errandId, namespace, municipalityId, lock);
		return errandsRepository
			.findOne(withId(errandId).and(withAccessControl(namespace, municipalityId, Identifier.get(), resource, required)))
			.orElseThrow(() -> Problem.valueOf(FORBIDDEN, ENTITY_NOT_ACCESSIBLE.formatted(getCallerIdentity())));
	}

	/**
	 * Verify existence of errand and that user has access to it if access control is enabled in namespace. Throws Problem
	 * 404 if errand does not exist. Throws 401 if user does not have access.
	 *
	 * @param namespace      namespace
	 * @param municipalityId municipality id
	 * @param id             errand id
	 * @param resource       resource being guarded
	 * @param required       lowest access level accepted for the operation
	 */
	public void verifyExistingErrandAndAuthorization(final String namespace, final String municipalityId, final String id, ProtectedResource resource, Access.AccessLevelEnum required) {
		verifyExistingErrand(id, namespace, municipalityId, false);
		final var authorized = errandsRepository.exists(withId(id).and(withAccessControl(namespace, municipalityId, Identifier.get(), resource, required)));

		if (!authorized) {
			throw Problem.valueOf(FORBIDDEN, ENTITY_NOT_ACCESSIBLE.formatted(getCallerIdentity()));
		}
	}

	private void verifyExistingErrand(final String id, final String namespace, final String municipalityId, final boolean lock) {

		final boolean exists;
		if (lock) {
			exists = errandsRepository.existsWithLockingByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		} else {
			exists = errandsRepository.existsByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId);
		}

		if (!exists) {
			throw Problem.valueOf(NOT_FOUND, ENTITY_NOT_FOUND.formatted(id, namespace, municipalityId));
		}
	}
}
