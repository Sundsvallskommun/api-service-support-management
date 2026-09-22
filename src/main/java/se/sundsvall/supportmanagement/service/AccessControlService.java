package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.errand.Errand;
import se.sundsvall.supportmanagement.api.model.errand.ExternalTag;
import se.sundsvall.supportmanagement.api.model.errand.JsonParameter;
import se.sundsvall.supportmanagement.api.model.errand.Parameter;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
import se.sundsvall.supportmanagement.service.mapper.ErrandMapper;
import se.sundsvall.supportmanagement.service.mapper.ErrandParameterMapper;
import se.sundsvall.supportmanagement.service.model.AccessSnapshot;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.supportmanagement.service.util.ServiceUtil.getCallerIdentity;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.hasAllowedMetadataLabels;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.isReportedBy;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

@Component
public class AccessControlService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ENTITY_NOT_ACCESSIBLE = "Errand not accessible by user '%s'";
	private static final String KEY_NOT_ACCESSIBLE = "Key '%s' not accessible by user '%s'";
	private static final String FIELD_NOT_WRITABLE = "Field '%s' not writable by user '%s'";
	private static final String KEY_NOT_WRITABLE = "Key '%s' not writable by user '%s'";
	private static final String RESOURCE_NOT_ACCESSIBLE = "Resource '%s' not accessible by user '%s'";

	/**
	 * Fields a limited read falls back to when the namespace has not said what limited read exposes. Overridden by
	 * configuring limitedReadAccess.
	 */
	private static final List<FieldAccess> DEFAULT_LIMITED_READ_FIELDS = List.of(
		FieldAccess.create().withField(ErrandField.ID),
		FieldAccess.create().withField(ErrandField.ERRAND_NUMBER),
		FieldAccess.create().withField(ErrandField.TITLE),
		FieldAccess.create().withField(ErrandField.STATUS));

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
	 * roles the user holds and whether or not the namespace maps errands per role. Role field restrictions apply to the
	 * errands their labels cover fully, and only while the namespace maps errands per role.
	 * <p>
	 * Fields given to the reporter of an errand are added on top of whatever restriction applies, and never restrict a
	 * user nothing else restricts. A reporter no label of theirs reaches the errand through is held to the reporter fields
	 * alone.
	 * <p>
	 * A null result means no restriction applies at all and the errand is mapped in full, which is what an unrestricted
	 * role yields. An empty result, in contrast, is a restriction resolving to no fields whatsoever. A limited read never
	 * resolves to nothing: a namespace that has not said what limited read exposes falls back to a built in minimum.
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
	 * A field grant may hold a field to read while the errand itself is writable, so the keys a caller may see are not
	 * always the keys they may change. What may be written is always a subset of what may be read: a level on a grant
	 * only ever restricts it further, and a grant carrying no level follows the errand.
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

		return fieldAccessResolver(config, accessMapperService.getAccessSnapshot(municipalityId, namespace, user), adAccountOf(user));
	}

	/**
	 * The same resolver, built from a configuration and a snapshot already in hand. Lets a caller answering more than one
	 * question of the same user answer all of them from a single snapshot.
	 */
	private Function<ErrandEntity, FieldAccessResolution> fieldAccessResolver(NamespaceConfig config, AccessSnapshot access, String adAccount) {
		// R/RW has precedence over LR, so an errand fully covered by them is not limited for this user.
		final var fullReadLabelIds = labelIds(access, levelsAtOrAbove(R));

		// The labels reaching an errand at any level, which is what tells a limited read apart from no label access at
		// all. Only the reporter of an errand can be either, and only an ad account can be a reporter, so nobody else
		// pays for resolving it.
		final var readableLabelIds = nonNull(adAccount) ? labelIds(access, levelsAtOrAbove(LR)) : Set.<String>of();

		// Roles only select fields, so they are consulted solely for a namespace mapping errands per role.
		final var namespaceRoles = config.isRoleBasedMapping() ? access.roles() : Set.<String>of();

		return errandEntity -> {
			final var reporter = isReporter(adAccount, errandEntity);
			final var coverage = coverageOf(fullReadLabelIds, readableLabelIds, errandEntity, reporter);
			final var applicable = restrictedFields(config, coverage, namespaceRoles);

			// Reporter fields widen a restriction, they never introduce one.
			if (isNull(applicable)) {
				return FieldAccessResolution.unrestricted();
			}

			if (reporter) {
				applicable.addAll(ofNullable(config.getReporterAccess()).map(ReporterAccess::getFields).orElse(emptyList()));
			}

			// Here the reporter fields stand on their own rather than on top of a limited read, so the minimum is applied
			// after them instead of before: a namespace saying what its reporters see may keep that narrower than limited
			// read, while one saying nothing at all falls back to the minimum rather than to an errand carrying no fields.
			if (Coverage.REPORTER_ONLY == coverage && applicable.isEmpty()) {
				applicable.addAll(DEFAULT_LIMITED_READ_FIELDS);
			}

			return new FieldAccessResolution(toFields(applicable), toFields(writableOf(applicable)));
		};
	}

	/**
	 * What the user may read of one errand and what of it they may write, resolved together.
	 * <p>
	 * A null map means nothing restricts the user and the errand is reached in full, which is what an unrestricted role
	 * yields. An empty map, in contrast, is a restriction resolving to no fields whatsoever.
	 *
	 * @param readable the fields, and the keys of them, the user may see
	 * @param writable the fields, and the keys of them, the user may change
	 */
	public record FieldAccessResolution(Map<ErrandField, Set<String>> readable, Map<ErrandField, Set<String>> writable) {

		private static FieldAccessResolution unrestricted() {
			return new FieldAccessResolution(null, null);
		}

		/**
		 * The keys of sent in field the user may see.
		 */
		public Predicate<String> readableKey(ErrandField field) {
			return toKeyPredicate(readable, field);
		}

		/**
		 * The keys of sent in field the user may change. Never wider than {@link #readableKey}.
		 */
		public Predicate<String> writableKey(ErrandField field) {
			return toKeyPredicate(writable, field);
		}

		/**
		 * Reads one field out of a resolved map. A null map is a user nothing restricts, so every key of every field is
		 * theirs; a field the map does not carry is one they do not reach at all; and a field carrying no keys is the
		 * whole collection.
		 */
		private static Predicate<String> toKeyPredicate(Map<ErrandField, Set<String>> fields, ErrandField field) {
			if (isNull(fields)) {
				return _ -> true;
			}

			final var keys = fields.get(field);
			if (isNull(keys)) {
				return _ -> false;
			}

			return keys.isEmpty() ? _ -> true : keys::contains;
		}
	}

	/**
	 * Reports what the user may do with one errand, so that a client can render only the controls their next request
	 * would actually be allowed to make.
	 * <p>
	 * The levels are resolved by {@link #highestLevel}, which mirrors the specification guarding every endpoint, and the
	 * fields by the same resolver {@code readErrand} maps its response with. The configuration and the access snapshot
	 * are each resolved once per call.
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
			? fieldAccessResolver(config, access, adAccount).apply(errandEntity)
			: FieldAccessResolution.unrestricted();

		final var fields = toFieldGrants(resolution, writableFields(errandLevel, resources));

		return new ErrandAccessResolution(errandLevel, resources, fields);
	}

	/**
	 * Signals which fields of the errand the user may change, given what they hold the errand and its resources at.
	 * <p>
	 * A field is writable when the user holds the errand at read/write. A keyed collection carrying a write endpoint of
	 * its own is also writable when the user holds the resource guarding that endpoint at read/write.
	 */
	private static Predicate<ErrandField> writableFields(Access.AccessLevelEnum errandLevel, Map<ProtectedResource, Access.AccessLevelEnum> resources) {
		return field -> RW == errandLevel || (nonNull(field.getWriteResource()) && RW == resources.get(field.getWriteResource()));
	}

	/**
	 * Renders a resolved field restriction as what the user may do with each field they reach.
	 * <p>
	 * Every level is capped by whether the field itself is writable.
	 *
	 * @param  access        resolved fields of the errand
	 * @param  writableField if the user may change sent in field at all
	 * @return               what the user may do with each field they reach
	 */
	private static Map<ErrandField, FieldGrant> toFieldGrants(FieldAccessResolution access, Predicate<ErrandField> writableField) {
		final Map<ErrandField, FieldGrant> grants = new EnumMap<>(ErrandField.class);

		// A user nothing restricts reaches every field, and every key of the keyed ones.
		if (isNull(access.readable())) {
			Arrays.stream(ErrandField.values()).forEach(field -> grants.put(field, unrestrictedGrant(field, writableField.test(field))));
			return grants;
		}

		access.readable().forEach((field, readableKeys) -> grants.put(field, toFieldGrant(access, field, readableKeys, writableField.test(field))));
		return grants;
	}

	private static FieldGrant unrestrictedGrant(ErrandField field, boolean fieldWritable) {
		final var level = fieldWritable ? RW : R;
		return field.isKeyed() ? new FieldGrant(level, true, new LinkedHashMap<>()) : new FieldGrant(level, null, null);
	}

	/**
	 * What the user may do with one field they reach, and with the individual keys of it when it holds a keyed
	 * collection.
	 * <p>
	 * A key restriction is all or nothing: either the namespace names the keys of the field, in which case those are the
	 * only ones reachable, or it names none and every key of the collection simply follows the field. The field itself
	 * carries the level of whatever serves it, and only its individual keys can be held to read.
	 */
	private static FieldGrant toFieldGrant(FieldAccessResolution access, ErrandField field, Set<String> readableKeys, boolean fieldWritable) {
		final var level = fieldWritable ? RW : R;

		if (!field.isKeyed()) {
			return new FieldGrant(level, null, null);
		}

		// No key restriction, so the keys are not enumerated at all and each of them follows the field.
		if (readableKeys.isEmpty()) {
			return new FieldGrant(level, true, new LinkedHashMap<>());
		}

		final var writableKey = access.writableKey(field);
		final Map<String, Access.AccessLevelEnum> keys = new LinkedHashMap<>();

		// Every key the user reaches, listed whether or not the errand carries it yet: a key granted here may be created
		// as well as changed, which is what lets a form be rendered before anything has been saved to it.
		readableKeys.forEach(key -> keys.put(key, fieldWritable && writableKey.test(key) ? RW : R));

		return new FieldGrant(level, false, keys);
	}

	/**
	 * What a user may do with one errand, resolved in one pass.
	 *
	 * @param errandLevel the level they hold the errand itself at, never null
	 * @param resources   the level they hold each errand scoped resource they reach at, the errand itself excluded
	 * @param fields      what they may do with each field they reach
	 */
	public record ErrandAccessResolution(
		Access.AccessLevelEnum errandLevel,
		Map<ProtectedResource, Access.AccessLevelEnum> resources,
		Map<ErrandField, FieldGrant> fields) {}

	/**
	 * What a user may do with one field of one errand. A field they do not reach at all is simply absent.
	 *
	 * @param level   what they may do with the field itself, which the errand answers for every field written through
	 *                it and the resource serving it answers for the rest
	 * @param allKeys if the field is reached without a key restriction, null for a field holding no keyed collection
	 * @param keys    every key of the collection they reach, empty when {@code allKeys}, null for a field holding no
	 *                keyed collection
	 */
	public record FieldGrant(
		Access.AccessLevelEnum level,
		Boolean allKeys,
		Map<String, Access.AccessLevelEnum> keys) {}

	/**
	 * The grants of sent in ones that carry the right to write, which is every grant not held to read.
	 */
	private static List<FieldAccess> writableOf(List<FieldAccess> applicable) {
		return applicable.stream()
			.filter(fieldAccess -> AccessLevel.R != fieldAccess.getLevel())
			.toList();
	}

	/**
	 * The three ways a user may hold an errand, each answering with a field set of its own.
	 */
	private enum Coverage {
		/** The labels of the user cover the errand at read or read/write. */
		FULL,
		/** Their labels reach the errand, but only at limited read. */
		LIMITED,
		/** No label of theirs reaches the errand, which leaves its reporter holding it as its reporter alone. */
		REPORTER_ONLY
	}

	/**
	 * Settles how the user holds sent in errand, which is expected to have passed access control already.
	 * <p>
	 * An errand their labels do not cover fully is held at limited read, except by its reporter when no label of theirs
	 * reaches it, who then holds it as its reporter alone.
	 */
	private static Coverage coverageOf(Set<String> fullReadLabelIds, Set<String> readableLabelIds, ErrandEntity errandEntity, boolean reporter) {
		if (covers(fullReadLabelIds, errandEntity)) {
			return Coverage.FULL;
		}

		return !reporter || covers(readableLabelIds, errandEntity) ? Coverage.LIMITED : Coverage.REPORTER_ONLY;
	}

	/**
	 * The fields sent in coverage restricts the errand to, before any reporter fields widen them. Null means nothing
	 * restricts the user, and an empty list that the restriction is carried by the reporter fields alone.
	 *
	 * @param  config         namespace configuration
	 * @param  coverage       how the user holds the errand
	 * @param  namespaceRoles roles the user holds, empty unless the namespace maps errands per role
	 * @return                fields to restrict to, or null when the errand is not restricted at all
	 */
	private static List<FieldAccess> restrictedFields(NamespaceConfig config, Coverage coverage, Set<String> namespaceRoles) {
		return switch (coverage) {
			// Limited read was never granted here, so it has nothing to add and the reporter fields stand alone.
			case REPORTER_ONLY -> new ArrayList<>();
			case LIMITED -> limitedReadFields(config);
			case FULL -> roleRestrictedFields(config, namespaceRoles);
		};
	}

	/**
	 * What an errand is trimmed to for a user holding limited read for it: the limited read fields of the namespace, or
	 * {@link #DEFAULT_LIMITED_READ_FIELDS} when it has configured none. Never null, so an errand that is limited for the
	 * user is never returned in full. The reporter fields are added on top of the result, which makes the minimum a
	 * floor the reporter fields widen.
	 */
	private static List<FieldAccess> limitedReadFields(NamespaceConfig config) {
		final var applicable = new ArrayList<>(ofNullable(config.getLimitedReadAccess()).map(LimitedReadAccess::getFields).orElse(emptyList()));

		if (applicable.isEmpty()) {
			applicable.addAll(DEFAULT_LIMITED_READ_FIELDS);
		}

		return applicable;
	}

	/**
	 * What the roles the user holds trim an errand they have full access to, or null when no role of theirs is
	 * restricted at all.
	 */
	private static List<FieldAccess> roleRestrictedFields(NamespaceConfig config, Set<String> namespaceRoles) {
		final var matchedRestrictions = ofNullable(config.getRoleFieldRestrictions()).orElse(emptyList()).stream()
			.filter(restriction -> nonNull(restriction.getRole()) && namespaceRoles.contains(restriction.getRole().toUpperCase(Locale.ROOT)))
			.toList();

		if (matchedRestrictions.isEmpty()) {
			return null;
		}

		final var applicable = new ArrayList<FieldAccess>();
		matchedRestrictions.forEach(restriction -> applicable.addAll(ofNullable(restriction.getFields()).orElse(emptyList())));

		return applicable;
	}

	/**
	 * Collects resolved field grants into the keys readable per field, merging the keys granted for a field more than
	 * once.
	 */
	private static Map<ErrandField, Set<String>> toFields(List<FieldAccess> applicable) {
		return applicable.stream().collect(Collectors.toMap(
			FieldAccess::getField,
			fieldAccess -> new LinkedHashSet<>(ofNullable(fieldAccess.getKeys()).orElse(emptyList())),
			AccessControlService::mergeKeys,
			LinkedHashMap::new));
	}

	/**
	 * Signals if sent in labels cover every label of the errand. An errand carrying no labels at all is covered by any
	 * set, the empty one included.
	 */
	private static boolean covers(Set<String> labelIds, ErrandEntity errandEntity) {
		return labelIds.containsAll(ofNullable(errandEntity.getAccessLabels()).orElse(emptyList()).stream()
			.map(AccessLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet()));
	}

	/**
	 * Ids of the labels sent in access reaches at any of sent in levels.
	 */
	private static Set<String> labelIds(AccessSnapshot access, List<Access.AccessLevelEnum> levels) {
		return access.labels(levels).stream()
			.map(MetadataLabelEntity::getId)
			.collect(Collectors.toSet());
	}

	private static boolean isReporter(String adAccount, ErrandEntity errandEntity) {
		return nonNull(adAccount) && adAccount.equalsIgnoreCase(errandEntity.getReporterUserId());
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
	 * The same answer as {@link #readableKeyPredicate}, for every keyed field of one errand at once. The grants are
	 * resolved once, when this is called, and asking the returned resolver for a field queries nothing.
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
	 * Throws 403 unless the user may reach sent in key of sent in field. A key the user cannot read is also a key they
	 * cannot write. A key they may read is not necessarily one they may change, which {@link #verifyWritableKeys}
	 * answers.
	 */
	public void verifyAccessibleKey(String namespace, String municipalityId, ErrandEntity errandEntity, ErrandField field, String key) {
		verifyAccessibleKeys(namespace, municipalityId, errandEntity, field, List.of(key));
	}

	/**
	 * Throws 403 unless the user may reach every one of sent in keys. Resolves the grants once for all keys.
	 */
	public void verifyAccessibleKeys(String namespace, String municipalityId, ErrandEntity errandEntity, ErrandField field, Collection<String> keys) {
		if (isNull(keys)) {
			return;
		}

		verifyAccessibleKeys(readableKeyPredicate(namespace, municipalityId, Identifier.get(), errandEntity, field), keys);
	}

	/**
	 * Throws 403 unless the user may reach every one of sent in keys, according to an already resolved predicate. Lets a
	 * caller that also needs the predicate itself resolve the grants only once.
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
	 * Throws 403 unless the user may change every one of sent in keys, according to an already resolved predicate.
	 * <p>
	 * Sent in keys are expected to be the ones a request would actually change, not every key it carries, so a key held
	 * to read may be sent back unchanged.
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
	 * Throws 403 unless the user may change sent in key of sent in field, resolving the grants for it.
	 */
	public void verifyWritableKey(String namespace, String municipalityId, ErrandEntity errandEntity, ErrandField field, String key) {
		verifyWritableKeys(writableKeyPredicate(namespace, municipalityId, Identifier.get(), errandEntity, field), List.of(key));
	}

	/**
	 * Verifies every keyed field of sent in patch against what the caller may reach on the errand, and answers with what
	 * the merge and the response need.
	 * <p>
	 * Two questions are asked of each field. A key the caller cannot see at all is refused outright, whichever endpoint
	 * they write it through. A key they may see but not change is refused only when the patch would actually change it.
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
	 * Refuses a patch carrying a value for a field that is not keyed and that its sender does not hold.
	 * <p>
	 * A field that is not keyed carries no level of its own, so it is theirs to read and to write, or not theirs at all.
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
	 * What a caller may do with the keys of one field of one errand.
	 *
	 * @param readableKey the keys they may see
	 * @param writableKey the keys they may change, never wider than the ones they may see
	 */
	public record KeyAccess(Predicate<String> readableKey, Predicate<String> writableKey) {}

	/**
	 * What a caller may do with the keyed fields of one errand they are patching.
	 *
	 * @param writableKey the keys they may change, per field, for the merge to leave the rest as it stands
	 * @param readable    the fields they may see, for the response to be mapped as a plain read would be
	 */
	public record ErrandKeyAccess(Function<ErrandField, Predicate<String>> writableKey, Function<ErrandEntity, Map<ErrandField, Set<String>>> readable) {}

	/**
	 * Merges the keys two roles grant for the same field. An empty set means the whole collection, so it wins over any set
	 * of individual keys.
	 */
	private static Set<String> mergeKeys(Set<String> left, Set<String> right) {
		if (left.isEmpty() || right.isEmpty()) {
			return new LinkedHashSet<>();
		}
		final var merged = new LinkedHashSet<>(left);
		merged.addAll(right);
		return merged;
	}

	/**
	 * Creates specification filter ensuring user has access to sent in resource at the required level.
	 * <p>
	 * Access granted through the labels of the access mapper is combined with access granted through a role the user holds
	 * on the errand itself. The two are unioned, so a role can only ever add access for principals the access mapper does
	 * not know about,
	 * never reduce what it already granted.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  resource       resource being guarded
	 * @param  required       lowest access level accepted for the operation
	 * @return                specification if access control is enabled on namespace, conjunction otherwise
	 */
	public Specification<ErrandEntity> withAccessControl(String namespace, String municipalityId, Identifier user, ProtectedResource resource, Access.AccessLevelEnum required) {
		final var config = namespaceConfigService.get(namespace, municipalityId);

		if (!config.isAccessControl()) {
			return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
		}

		final var clauses = new ArrayList<Specification<ErrandEntity>>();
		final var access = accessMapperService.getAccessSnapshot(municipalityId, namespace, user);

		// Labels say which errands the user reaches, the access mapper resources say which operations they may perform at
		// all, and both must allow.
		final var grant = resourceGrant(config, access, resource);

		if (grant.permits(required)) {
			clauses.add(hasAllowedMetadataLabels(allowedLabels(config, access, grant, resource, required)));
		}

		// Errands reported by the user, which their labels may say nothing at all about.
		if (grantsReporterAccess(config, resource, required)) {
			clauses.add(isReportedBy(adAccountOf(user)));
		}

		return clauses.stream()
			.reduce(Specification::or)
			.orElse((_, _, criteriaBuilder) -> criteriaBuilder.disjunction());
	}

	/**
	 * The level the labels of the user must give the errand for sent in operation on sent in resource, used by both
	 * {@link #withAccessControl} and {@link #reaches}.
	 * <p>
	 * Limited read lowers the floor for the resources a namespace extends it to, and what the grant of the access mapper
	 * has already vouched for answers the rest.
	 */
	private Access.AccessLevelEnum requiredLabelLevel(NamespaceConfig config, ResourceGrant grant, ProtectedResource resource, Access.AccessLevelEnum required) {
		return grantsLimitedReadAccess(config, resource, required) ? LR : grant.requiredLabelLevel(resource, required);
	}

	/**
	 * The labels that reach sent in resource at sent in level: every label the user holds at or above the level
	 * {@link #requiredLabelLevel} asks for.
	 */
	private Set<MetadataLabelEntity> allowedLabels(NamespaceConfig config, AccessSnapshot access, ResourceGrant grant, ProtectedResource resource, Access.AccessLevelEnum required) {
		return access.labels(levelsAtOrAbove(requiredLabelLevel(config, grant, resource, required)));
	}

	/**
	 * Answers in memory, for one errand already in hand, the question {@link #withAccessControl} asks of the database,
	 * and gives the same answer as the specification does.
	 */
	private boolean reaches(NamespaceConfig config, AccessSnapshot access, ErrandEntity errandEntity, String adAccount, ProtectedResource resource, Access.AccessLevelEnum required) {
		// Nothing restricts anyone while the namespace has not opted in, which is the conjunction of the specification.
		if (!config.isAccessControl()) {
			return true;
		}

		final var grant = resourceGrant(config, access, resource);

		if (grant.permits(required)) {
			final var allowed = allowedLabels(config, access, grant, resource, required);

			// hasAllowedMetadataLabels reaches no errand at all for a user holding no labels, where covers would call an
			// unlabelled errand covered by the empty set. The specification is the enforcement, so emptiness is asked first.
			if (!allowed.isEmpty() && covers(allowed.stream().map(MetadataLabelEntity::getId).collect(Collectors.toSet()), errandEntity)) {
				return true;
			}
		}

		// isReportedBy reaches no errand for a caller carrying no ad account, so neither does this.
		return grantsReporterAccess(config, resource, required) && isReporter(adAccount, errandEntity);
	}

	/**
	 * The most a user may do with sent in resource of one errand, or null for a resource they do not reach at all.
	 * <p>
	 * Probed from RW down to LR, answering with the first level {@link #reaches} accepts.
	 */
	private Access.AccessLevelEnum highestLevel(NamespaceConfig config, AccessSnapshot access, ErrandEntity errandEntity, String adAccount, ProtectedResource resource) {
		return levelsAtOrAbove(LR).reversed().stream()
			.filter(level -> reaches(config, access, errandEntity, adAccount, resource, level))
			.findFirst()
			.orElse(null);
	}

	/**
	 * What the access mapper grants the user on one resource of an errand within the namespace.
	 * <p>
	 * A namespace that has not switched resource access control on applies no grant at all. Such a grant permits every
	 * operation, but leaves the labels to carry a write themselves.
	 *
	 * @param applied if the namespace weighs the resource grants of the access mapper at all
	 * @param level   the level granted for the resource, null for one the access mapper does not grant
	 */
	private record ResourceGrant(boolean applied, Access.AccessLevelEnum level) {

		/**
		 * Signals if the user may perform an operation at sent in level. A grant the namespace does not apply never
		 * refuses.
		 * <p>
		 * The granted level is weighed against the level the operation actually asks for, so a resource granted at
		 * limited read satisfies a read but neither a full read nor a write.
		 */
		boolean permits(Access.AccessLevelEnum required) {
			return !applied || (nonNull(level) && satisfies(level, required));
		}

		/**
		 * The level the labels must reach the errand at, given what this grant has already vouched for.
		 * <p>
		 * Anything short of a write asks the labels for read. A write asks them for read only when the grant applies and
		 * the resource is not the errand itself; otherwise the labels carry the write themselves and must reach the
		 * errand at read/write.
		 */
		Access.AccessLevelEnum requiredLabelLevel(ProtectedResource resource, Access.AccessLevelEnum required) {
			if (RW != required) {
				return R;
			}
			return applied && ProtectedResource.ERRAND != resource ? R : RW;
		}
	}

	/**
	 * The grant sent in namespace applies to sent in resource, read from the snapshot already in hand.
	 */
	private static ResourceGrant resourceGrant(NamespaceConfig config, AccessSnapshot access, ProtectedResource resource) {
		return config.isResourceAccessControl()
			? new ResourceGrant(true, access.resources().get(resource))
			: new ResourceGrant(false, null);
	}

	/**
	 * Signals if limited read reaches sent in resource. Within limited read a resource is simply reachable or not and
	 * carries no level of its own. Operations asking for more than limited read are never satisfied by it.
	 * <p>
	 * The errand itself is always reachable, and a namespace extends limited read beyond the errand by listing further
	 * resources.
	 */
	private boolean grantsLimitedReadAccess(NamespaceConfig config, ProtectedResource resource, Access.AccessLevelEnum required) {
		if (LR != required) {
			return false;
		}
		return ProtectedResource.ERRAND == resource || ofNullable(config.getLimitedReadAccess())
			.map(LimitedReadAccess::getResources)
			.orElse(emptyList())
			.contains(resource);
	}

	/**
	 * Signals if the namespace grants the reporter of an errand sent in resource at sent in level. A namespace without
	 * configured reporter access grants nothing, which leaves the exception switched off.
	 */
	private boolean grantsReporterAccess(NamespaceConfig config, ProtectedResource resource, Access.AccessLevelEnum required) {
		return ofNullable(config.getReporterAccess())
			.map(ReporterAccess::getResources)
			.orElse(emptyList()).stream()
			.anyMatch(resourceAccess -> resource == resourceAccess.getResource() && satisfies(toAccessLevelEnum(resourceAccess.getLevel()), required));
	}

	/**
	 * Extracts the ad account of sent in identifier, or null for any other identifier type.
	 */
	private static String adAccountOf(Identifier user) {
		return ofNullable(user)
			.filter(identifier -> Identifier.Type.AD_ACCOUNT.equals(identifier.getType()))
			.map(Identifier::getValue)
			.orElse(null);
	}

	/**
	 * Translates a level configured on this API into the client enum the service layer compares with, by name.
	 */
	private static Access.AccessLevelEnum toAccessLevelEnum(final AccessLevel level) {
		return isNull(level) ? null : Access.AccessLevelEnum.valueOf(level.name());
	}

	/**
	 * Signals if a granted access level is enough for the required one, ordered LR before R before RW.
	 */
	private static boolean satisfies(Access.AccessLevelEnum granted, Access.AccessLevelEnum required) {
		return levelsAtOrAbove(required).contains(granted);
	}

	/**
	 * Translates a required access level into the levels that satisfy it. LR is the lowest level and is satisfied by any
	 * level, RW is the highest and is only satisfied by itself.
	 *
	 * @param  required lowest access level accepted for the operation
	 * @return          all access levels satisfying the requirement
	 */
	private static List<Access.AccessLevelEnum> levelsAtOrAbove(Access.AccessLevelEnum required) {
		return switch (required) {
			case LR -> List.of(LR, R, RW);
			case R -> List.of(R, RW);
			case RW -> List.of(RW);
		};
	}

	/**
	 * Verifies that the requesting user may reach a resource belonging to the namespace itself rather than to any errand,
	 * such as its configuration or its metadata. The resource grants of the access mapper decide on their own, labels
	 * are not consulted.
	 * <p>
	 * Enforced whenever access control is active for the namespace, as read from its persisted configuration. A
	 * namespace without configuration enforces nothing, and switching access control off is itself guarded.
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
		final var grant = new ResourceGrant(true, accessMapperService.getAccessSnapshot(municipalityId, namespace, Identifier.get()).resources().get(resource));

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
	 * 404 if errand does not exist. Throws 403 if user does not have access.
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
