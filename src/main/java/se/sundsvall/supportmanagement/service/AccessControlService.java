package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import java.util.ArrayList;
import java.util.Collection;
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
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;
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
		final var config = namespaceConfigService.get(namespace, municipalityId);

		// Nothing restricts anyone while the namespace has not opted in, so the access mapper is never asked for it.
		if (!config.isAccessControl()) {
			return _ -> null;
		}

		final var adAccount = adAccountOf(user);
		final var access = accessMapperService.getAccessSnapshot(municipalityId, namespace, user);

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
				return null;
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

			return toFields(applicable);
		};
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
		return toKeyPredicate(roleBasedFieldResolver(namespace, municipalityId, user).apply(errandEntity), field);
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
		final var fields = roleBasedFieldResolver(namespace, municipalityId, user).apply(errandEntity);
		return field -> toKeyPredicate(fields, field);
	}

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

	/**
	 * Throws 401 unless the user may reach sent in key of sent in field. A key the user cannot read is also a key they
	 * cannot write, so that no one can overwrite or remove data they are not allowed to see.
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
			// One clause at the lowest label level that reaches this resource. A separate clause for limited read would be
			// redundant, since the labels of a level are a subset of those of every level below it and the predicate is
			// monotonic, so the stricter clause can never match a row the looser one does not.
			final var lowestLevel = grantsLimitedReadAccess(config, resource, required) ? LR : fullAccessLevel(required);

			clauses.add(hasAllowedMetadataLabels(access.labels(levelsAtOrAbove(lowestLevel))));
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
