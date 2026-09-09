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
import java.util.Optional;
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
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.hasAllowedMetadataLabels;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.isReportedBy;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

@Component
public class AccessControlService {

	private static final String ENTITY_NOT_FOUND = "An errand with id '%s' could not be found in namespace '%s' for municipality with id '%s'";
	private static final String ENTITY_NOT_ACCESSIBLE = "Errand not accessible by user '%s'";
	private static final String KEY_NOT_ACCESSIBLE = "Key '%s' not accessible by user '%s'";
	private static final String KEY_NOT_WRITABLE = "Key '%s' not writable by user '%s'";
	private static final String RESOURCE_NOT_ACCESSIBLE = "Resource '%s' not accessible by user '%s'";

	/**
	 * Fields a limited read falls back to when the namespace has not said what limited read exposes. Keeps a namespace from
	 * widening what a limited read user sees simply by switching role based mapping on, and is overridden by configuring
	 * limitedReadAccess.
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

		return fieldAccessResolver(config, accessMapperService.getAccessSnapshot(municipalityId, namespace, user), adAccountOf(user));
	}

	/**
	 * The same resolver, built from a configuration and a snapshot already in hand.
	 * <p>
	 * Lets a caller answering more than one question of the same user resolve both from a single snapshot. Read once per
	 * question instead, the two could be answered from different moments and disagree with each other - the reason a
	 * snapshot holds labels, roles and resources together in the first place.
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
			throw Problem.valueOf(UNAUTHORIZED, ENTITY_NOT_ACCESSIBLE.formatted(ofNullable(user)
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

		final var fields = toFieldGrants(resolution, RW == errandLevel);

		return new ErrandAccessResolution(errandLevel, resources, fields);
	}

	/**
	 * Renders a resolved field restriction as what the user may do with each field they reach.
	 * <p>
	 * Every level is capped by whether the errand itself is writable. A namespace may not put a level on a field holding
	 * no keyed collection, so every such grant is levelless and would otherwise be reported writable to a user holding
	 * the errand at read - who would then be refused by the very endpoint the report invited them to call.
	 *
	 * @param  access         resolved fields of the errand
	 * @param  errandWritable if the user may write the errand at all
	 * @return                what the user may do with each field they reach
	 */
	private static Map<ErrandField, FieldGrant> toFieldGrants(FieldAccessResolution access, boolean errandWritable) {
		final Map<ErrandField, FieldGrant> grants = new EnumMap<>(ErrandField.class);

		// A user nothing restricts reaches every field, and every key of the keyed ones.
		if (isNull(access.readable())) {
			Arrays.stream(ErrandField.values()).forEach(field -> grants.put(field, unrestrictedGrant(field)));
			return grants;
		}

		access.readable().forEach((field, readableKeys) -> grants.put(field, toFieldGrant(access, field, readableKeys, errandWritable)));
		return grants;
	}

	private static FieldGrant unrestrictedGrant(ErrandField field) {
		return field.isKeyed() ? new FieldGrant(true, new LinkedHashMap<>()) : new FieldGrant(null, null);
	}

	/**
	 * What the user may do with one field they reach, and with the individual keys of it when it holds a keyed
	 * collection.
	 * <p>
	 * A key restriction is all or nothing: either the namespace names the keys of the field, in which case those are the
	 * only ones reachable, or it names none and every key of the collection simply follows the errand. The field itself
	 * therefore carries no level of its own - a field is writable exactly when the errand is, since a namespace may only
	 * hold a key to read, never a whole field.
	 */
	private static FieldGrant toFieldGrant(FieldAccessResolution access, ErrandField field, Set<String> readableKeys, boolean errandWritable) {
		if (!field.isKeyed()) {
			return new FieldGrant(null, null);
		}

		// No key restriction, so the keys are not enumerated at all and each of them follows the errand.
		if (readableKeys.isEmpty()) {
			return new FieldGrant(true, new LinkedHashMap<>());
		}

		final var writableKey = access.writableKey(field);
		final Map<String, Access.AccessLevelEnum> keys = new LinkedHashMap<>();

		// Every key the user reaches, listed whether or not the errand carries it yet: a key granted here may be created
		// as well as changed, which is what lets a form be rendered before anything has been saved to it.
		readableKeys.forEach(key -> keys.put(key, errandWritable && writableKey.test(key) ? RW : R));

		return new FieldGrant(false, keys);
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
	 * What a user may do with one field of one errand. A field they do not reach at all is simply absent, and a field
	 * they reach follows the errand unless its keys say otherwise.
	 *
	 * @param allKeys if the field is reached without a key restriction, null for a field holding no keyed collection
	 * @param keys    every key of the collection they reach, empty when {@code allKeys}, null for a field holding no
	 *                keyed collection
	 */
	public record FieldGrant(
		Boolean allKeys,
		Map<String, Access.AccessLevelEnum> keys) {}

	/**
	 * The grants of sent in ones that carry the right to write, which is every grant a namespace has not deliberately
	 * held to read.
	 */
	private static List<FieldAccess> writableOf(List<FieldAccess> applicable) {
		return applicable.stream()
			.filter(fieldAccess -> AccessLevel.R != fieldAccess.getLevel())
			.toList();
	}

	/**
	 * The three ways a user may hold an errand, since each of them answers with a field set of its own.
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
	 * Settles how the user holds sent in errand.
	 * <p>
	 * Anyone but the reporter holding an errand their labels do not cover fully was granted limited read for it, since
	 * nothing else would have returned it to them at all. The reporter reaches their own errand either way, so only there
	 * do the labels have to be asked whether limited read is what actually applies.
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
	 * What an errand is trimmed to for a user holding limited read for it.
	 * <p>
	 * An errand that is limited for the user is never returned in full. Nothing configured means the namespace has not
	 * said what limited read exposes, and the safe reading of limited is the minimum rather than everything. Resolved
	 * before the reporter fields are merged in, so that the minimum is a floor the reporter widens rather than something
	 * their own field set replaces - otherwise a namespace granting the reporter a single field would show them less of
	 * their own errand than any other limited read user sees.
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
	 * Signals if sent in labels cover every label of the errand.
	 * <p>
	 * Nearly the question {@link se.sundsvall.supportmanagement.service.util.SpecificationBuilder#hasAllowedMetadataLabels}
	 * asks of the labels at a given level, with one long standing difference: an errand carrying no labels at all is
	 * covered by any set here, while the specification reaches no errand at all for a user holding no labels. A user the
	 * access mapper grants nothing is therefore fully covered for an unlabelled errand rather than held to the reporter
	 * fields.
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
				throw Problem.valueOf(UNAUTHORIZED, KEY_NOT_ACCESSIBLE.formatted(key, Optional.ofNullable(Identifier.get())
					.map(Identifier::getValue)
					.orElse(null)));
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
				throw Problem.valueOf(UNAUTHORIZED, KEY_NOT_WRITABLE.formatted(key, Optional.ofNullable(Identifier.get())
					.map(Identifier::getValue)
					.orElse(null)));
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
		if (grantsResourceAccess(config, access, resource, required)) {
			clauses.add(hasAllowedMetadataLabels(allowedLabels(config, access, resource, required)));
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
	 * The level labels must give for an errand to count as fully accessible, as opposed to limited read. A write needs
	 * read/write, everything else needs read.
	 */
	private static Access.AccessLevelEnum fullAccessLevel(Access.AccessLevelEnum required) {
		return RW == required ? RW : R;
	}

	/**
	 * The labels that reach sent in resource at sent in level.
	 * <p>
	 * One set at the lowest label level reaching the resource. A separate set for limited read would be redundant, since
	 * the labels of a level are a subset of those of every level below it and the predicate is monotonic, so the stricter
	 * set can never match an errand the looser one does not.
	 */
	private Set<MetadataLabelEntity> allowedLabels(NamespaceConfig config, AccessSnapshot access, ProtectedResource resource, Access.AccessLevelEnum required) {
		return access.labels(levelsAtOrAbove(grantsLimitedReadAccess(config, resource, required) ? LR : fullAccessLevel(required)));
	}

	/**
	 * Answers, for one errand already in hand, the question {@link #withAccessControl} asks of the database.
	 * <p>
	 * The two must agree, since this is what {@link #resolveErrandAccess} reports and the specification is what actually
	 * guards every endpoint: an answer here that the specification would refuse is a caller told they may do something
	 * that then fails with 401. {@code AccessControlSpecificationParityTest} holds the two to each other.
	 * <p>
	 * Kept in memory rather than asked of the database, since reporting the level of every resource of an errand would
	 * otherwise be one query per resource and level.
	 */
	private boolean reaches(NamespaceConfig config, AccessSnapshot access, ErrandEntity errandEntity, String adAccount, ProtectedResource resource, Access.AccessLevelEnum required) {
		// Nothing restricts anyone while the namespace has not opted in, which is the conjunction of the specification.
		if (!config.isAccessControl()) {
			return true;
		}

		if (grantsResourceAccess(config, access, resource, required)) {
			final var allowed = allowedLabels(config, access, resource, required);

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
	 * Probed from the top down, which settles it in at most three passes: {@link #reaches} is monotonic in the level
	 * asked for, since the labels of a level are a subset of those of every level below it, {@link #satisfies} weighs a
	 * grant the same way, and limited read only ever widens the lowest level.
	 */
	private Access.AccessLevelEnum highestLevel(NamespaceConfig config, AccessSnapshot access, ErrandEntity errandEntity, String adAccount, ProtectedResource resource) {
		return levelsAtOrAbove(LR).reversed().stream()
			.filter(level -> reaches(config, access, errandEntity, adAccount, resource, level))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Signals if the access mapper lets the user reach sent in resource at sent in level. Namespaces that have not switched
	 * on resource access control are unrestricted here and rely on their labels alone, which is what keeps the feature
	 * inert until the
	 * access mapper has been configured for the namespace.
	 * <p>
	 * The granted level is weighed against the level the operation actually asks for, so a resource granted at limited read
	 * satisfies a read but neither a full read nor a write. Weighing it against the full access level instead would make a
	 * limited read
	 * grant equal to no grant at all.
	 */
	private static boolean grantsResourceAccess(NamespaceConfig config, AccessSnapshot access, ProtectedResource resource, Access.AccessLevelEnum required) {
		if (!config.isResourceAccessControl()) {
			return true;
		}
		return ofNullable(access.resources().get(resource))
			.filter(granted -> satisfies(granted, required))
			.isPresent();
	}

	/**
	 * Signals if limited read reaches sent in resource. Whether an errand is limited for the user is settled by their
	 * labels, so within limited read a resource is simply reachable or not and carries no level of its own. Operations
	 * asking for more than
	 * limited read are never satisfied by it.
	 * <p>
	 * The errand itself is always reachable, that is what limited read means, and a namespace extends it beyond the errand
	 * by listing further resources.
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
	 * Extracts the ad account of sent in identifier. Labels are only resolved for ad accounts, and reporterUserId holds an
	 * ad account, so any other identifier type can never match.
	 */
	private static String adAccountOf(Identifier user) {
		return ofNullable(user)
			.filter(identifier -> Identifier.Type.AD_ACCOUNT.equals(identifier.getType()))
			.map(Identifier::getValue)
			.orElse(null);
	}

	/**
	 * Translates a level configured on this API into the client enum the service layer compares with. The two enums
	 * carry the same names, and are kept apart so that a change to the access mapper contract cannot alter this API.
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

		final var granted = accessMapperService.getAccessSnapshot(municipalityId, namespace, Identifier.get()).resources().get(resource);

		if (isNull(granted) || !satisfies(granted, required)) {
			throw Problem.valueOf(UNAUTHORIZED, RESOURCE_NOT_ACCESSIBLE.formatted(resource, Optional.ofNullable(Identifier.get())
				.map(Identifier::getValue)
				.orElse(null)));
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
			.orElseThrow(() -> Problem.valueOf(UNAUTHORIZED, ENTITY_NOT_ACCESSIBLE.formatted(Optional.ofNullable(Identifier.get())
				.map(Identifier::getValue)
				.orElse(null))));
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
			throw Problem.valueOf(UNAUTHORIZED, ENTITY_NOT_ACCESSIBLE.formatted(Optional.ofNullable(Identifier.get())
				.map(Identifier::getValue)
				.orElse(null)));
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
