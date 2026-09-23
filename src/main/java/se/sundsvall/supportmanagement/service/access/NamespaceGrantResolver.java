package se.sundsvall.supportmanagement.service.access;

import generated.se.sundsvall.accessmapper.Access;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Stream;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * Resolves what a user holds, from a namespace configuration and a snapshot of the access mapper and nothing else.
 * <p>
 * Every decision of access control is made here, as a function of the two: which errands the labels and the reporting
 * reach, what may be read and written of an errand held with a given coverage, what a resource grant permits. Nothing
 * here reads a database or answers a request; that is the business of
 * {@link se.sundsvall.supportmanagement.service.AccessControlService},
 * which fetches the configuration and the snapshot once, asks here, and enforces or renders the answer.
 */
public final class NamespaceGrantResolver {

	private NamespaceGrantResolver() {}

	/**
	 * The errands the labels of the user reach at limited read, with what a limited read exposes of them.
	 * <p>
	 * Only for an operation asking for read or less: nothing is written on the strength of a limited read, and an
	 * operation asking for write is answered by the labels carrying it. Resolved from the same snapshot as the rest of
	 * the grant, and null where limited read reaches no more than the level already did - an errand the labels cover at
	 * the level is held at the level, whatever a limited read would have exposed of it, so a route reaching nothing new
	 * would only widen what may be read of errands already reached.
	 */
	private static NamespaceGrant.LabelRoute limitedLabelRoute(NamespaceConfig config, AccessSnapshot access, Identifier user, Set<String> namespaceRoles, Access.AccessLevelEnum required,
		Set<MetadataLabelEntity> reachedAtLevel) {
		if (RW == required) {
			return null;
		}

		final var limited = accessScope(config, access, user, ProtectedResource.ERRAND, LR).allowedLabels();
		if (isNull(limited) || limited.isEmpty() || limited.equals(ofNullable(reachedAtLevel).orElse(Set.of()))) {
			return null;
		}

		return new NamespaceGrant.LabelRoute(limited, fieldAccess(config, Coverage.LIMITED, namespaceRoles, false).readable(), labelResources(config, access, user, LR, limited));
	}

	/**
	 * The errand scoped resources a route of labels reaches on every errand it reaches.
	 * <p>
	 * A resource is guarded on its own, and by labels of its own: reaching an errand says nothing about what hangs off
	 * it. The resource is reached on every errand of the route exactly when the labels reaching the resource cover
	 * everything the route covers, which is what including its labels says, since an errand is covered when all of its
	 * access labels are among them. That is what keeps a limited read from reaching a resource the namespace has not
	 * extended it to, whatever the labels of the user reach on the errands they hold at read.
	 */
	private static Set<ProtectedResource> labelResources(NamespaceConfig config, AccessSnapshot access, Identifier user, Access.AccessLevelEnum required, Set<MetadataLabelEntity> routeLabels) {
		return errandResources()
			.filter(resource -> ofNullable(accessScope(config, access, user, resource, required).allowedLabels())
				.map(allowed -> allowed.containsAll(routeLabels))
				.orElse(false))
			.collect(Collectors.toSet());
	}

	/**
	 * The errand scoped resources the namespace grants its reporters at the level, on the errands they reported.
	 */
	private static Set<ProtectedResource> reporterResources(NamespaceConfig config, AccessSnapshot access, Identifier user, Access.AccessLevelEnum required) {
		return errandResources()
			.filter(resource -> nonNull(accessScope(config, access, user, resource, required).reporterAdAccount()))
			.collect(Collectors.toSet());
	}

	/** The resources belonging to an errand, the errand itself excluded: it is what the routes reach to begin with. */
	private static Stream<ProtectedResource> errandResources() {
		return Stream.of(ProtectedResource.values())
			.filter(ProtectedResource::isErrandScoped)
			.filter(resource -> ProtectedResource.ERRAND != resource);
	}

	/**
	 * What the user holds in the namespace at the required level, see {@link NamespaceGrant}.
	 */
	public static NamespaceGrant namespaceGrant(NamespaceConfig config, AccessSnapshot access, Identifier user, Access.AccessLevelEnum required) {
		if (!config.isAccessControl()) {
			return NamespaceGrant.UNRESTRICTED;
		}

		final var namespaceRoles = config.isRoleBasedMapping() ? access.roles() : Set.<String>of();
		final var errand = accessScope(config, access, user, ProtectedResource.ERRAND, required);

		final var labels = isNull(errand.allowedLabels())
			? null
			: new NamespaceGrant.LabelRoute(errand.allowedLabels(), fieldAccess(config, Coverage.FULL, namespaceRoles, false).readable(),
				labelResources(config, access, user, required, errand.allowedLabels()));
		final var limitedLabels = limitedLabelRoute(config, access, user, namespaceRoles, required, errand.allowedLabels());
		final var reporter = isNull(errand.reporterAdAccount())
			? null
			: new NamespaceGrant.ReporterRoute(errand.reporterAdAccount(), fieldAccess(config, Coverage.REPORTER_ONLY, namespaceRoles, true).readable(),
				reporterResources(config, access, user, required));

		return new NamespaceGrant(true, labels, limitedLabels, reporter);
	}

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

	/**
	 * The same resolver, built from a configuration and a snapshot already in hand.
	 * <p>
	 * Lets a caller answering more than one question of the same user resolve both from a single snapshot. Read once per
	 * question instead, the two could be answered from different moments and disagree with each other - the reason a
	 * snapshot holds labels, roles and resources together in the first place.
	 */
	public static Function<ErrandEntity, FieldAccessResolution> fieldAccessResolver(NamespaceConfig config, AccessSnapshot access, String adAccount) {
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
			return fieldAccess(config, coverage, namespaceRoles, reporter);
		};
	}

	/**
	 * What the user may read and write of an errand they hold with sent in coverage.
	 */
	public static FieldAccessResolution fieldAccess(NamespaceConfig config, Coverage coverage, Set<String> namespaceRoles, boolean reporter) {
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
	}

	/**
	 * Signals which fields of the errand the user may change, given what they hold the errand and its resources at.
	 * <p>
	 * Everything of an errand is written through the errand itself and so follows it, except the keyed collections
	 * carrying a write endpoint of their own: those follow the resource serving them, since that is what the endpoint
	 * accepting the write is guarded on. Reporting them by the errand instead would hold a key to read that the write
	 * path accepts, which is the report contradicting the endpoint rather than merely understating it.
	 */
	public static Predicate<ErrandField> writableFields(Access.AccessLevelEnum errandLevel, Map<ProtectedResource, Access.AccessLevelEnum> resources) {
		return field -> RW == errandLevel || (nonNull(field.getWriteResource()) && RW == resources.get(field.getWriteResource()));
	}

	/**
	 * Renders a resolved field restriction as what the user may do with each field they reach.
	 * <p>
	 * Every level is capped by whether the field itself is writable. A namespace may not put a level on a field holding
	 * no keyed collection, so every such grant is levelless and would otherwise be reported writable to a user holding
	 * the errand at read - who would then be refused by the very endpoint the report invited them to call.
	 *
	 * @param  access        resolved fields of the errand
	 * @param  writableField if the user may change sent in field at all
	 * @return               what the user may do with each field they reach
	 */
	public static Map<ErrandField, FieldGrant> toFieldGrants(FieldAccessResolution access, Predicate<ErrandField> writableField) {
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
	 * only ones reachable, or it names none and every key of the collection simply follows the field. The field carries
	 * the level of whatever serves it, which a namespace cannot narrow further - it may hold an individual key to read,
	 * never a whole field.
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
	 * The grants of sent in ones that carry the right to write, which is every grant a namespace has not deliberately
	 * held to read.
	 */
	private static List<FieldAccess> writableOf(List<FieldAccess> applicable) {
		return applicable.stream()
			.filter(fieldAccess -> AccessLevel.R != fieldAccess.getLevel())
			.toList();
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
			NamespaceGrantResolver::mergeKeys,
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
	 * Merges the keys two roles grant for the same field. An empty set means the whole collection, so it wins over any set
	 * of individual keys.
	 */
	public static Set<String> mergeKeys(Set<String> left, Set<String> right) {
		if (left.isEmpty() || right.isEmpty()) {
			return new LinkedHashSet<>();
		}
		final var merged = new LinkedHashSet<>(left);
		merged.addAll(right);
		return merged;
	}

	public static AccessScope accessScope(NamespaceConfig config, AccessSnapshot access, Identifier user, ProtectedResource resource, Access.AccessLevelEnum required) {
		final var grant = resourceGrant(config, access, resource);

		final var allowedLabels = grant.permits(required) ? allowedLabels(config, access, grant, resource, required) : null;
		final var reporterAdAccount = grantsReporterAccess(config, resource, required) ? adAccountOf(user) : null;

		return new AccessScope(true, allowedLabels, reporterAdAccount);
	}

	/**
	 * The level the labels of the user must give the errand for sent in operation on sent in resource.
	 * <p>
	 * The single place that question is answered: the specification guarding every endpoint and the in memory mirror of
	 * it both read it here, so the two cannot come to differ. Limited read lowers the floor for the resources a
	 * namespace extends it to, and what the grant of the access mapper has already vouched for answers the rest.
	 */
	private static Access.AccessLevelEnum requiredLabelLevel(NamespaceConfig config, ResourceGrant grant, ProtectedResource resource, Access.AccessLevelEnum required) {
		return grantsLimitedReadAccess(config, resource, required) ? LR : grant.requiredLabelLevel(resource, required);
	}

	/**
	 * The labels that reach sent in resource at sent in level.
	 * <p>
	 * One set at the lowest label level reaching the resource. A separate set for limited read would be redundant, since
	 * the labels of a level are a subset of those of every level below it and the predicate is monotonic, so the stricter
	 * set can never match an errand the looser one does not.
	 */
	private static Set<MetadataLabelEntity> allowedLabels(NamespaceConfig config, AccessSnapshot access, ResourceGrant grant, ProtectedResource resource, Access.AccessLevelEnum required) {
		return access.labels(levelsAtOrAbove(requiredLabelLevel(config, grant, resource, required)));
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
	public static boolean reaches(NamespaceConfig config, AccessSnapshot access, ErrandEntity errandEntity, String adAccount, ProtectedResource resource, Access.AccessLevelEnum required) {
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
	 * Probed from the top down, which settles it in at most three passes: {@link #reaches} is monotonic in the level
	 * asked for, since the labels of a level are a subset of those of every level below it, {@link #satisfies} weighs a
	 * grant the same way, and limited read only ever widens the lowest level.
	 */
	public static Access.AccessLevelEnum highestLevel(NamespaceConfig config, AccessSnapshot access, ErrandEntity errandEntity, String adAccount, ProtectedResource resource) {
		return levelsAtOrAbove(LR).reversed().stream()
			.filter(level -> reaches(config, access, errandEntity, adAccount, resource, level))
			.findFirst()
			.orElse(null);
	}

	/**
	 * What the access mapper grants the user on one resource of an errand within the namespace.
	 * <p>
	 * A namespace that has not switched resource access control on applies no grants at all, and says so rather than
	 * granting everything: the two are the same answer to the operation, which is unrestricted either way, but not to
	 * the labels, which carry the write themselves where nothing else does. Keeping that distinction in the type is
	 * what keeps it from being rediscovered by each caller.
	 *
	 * @param applied if the namespace weighs the resource grants of the access mapper at all
	 * @param level   the level granted for the resource, null for one the access mapper does not grant
	 */
	public record ResourceGrant(boolean applied, Access.AccessLevelEnum level) {

		/**
		 * Signals if the user may perform an operation at sent in level. A grant the namespace does not apply never
		 * refuses, which is what keeps resource access control inert until the access mapper has been configured for the
		 * namespace.
		 * <p>
		 * The granted level is weighed against the level the operation actually asks for, so a resource granted at
		 * limited read satisfies a read but neither a full read nor a write. Weighing it against the full access level
		 * instead would make a limited read grant equal to no grant at all.
		 */
		public boolean permits(Access.AccessLevelEnum required) {
			return !applied || (nonNull(level) && satisfies(level, required));
		}

		/**
		 * The level the labels must reach the errand at, given what this grant has already vouched for.
		 * <p>
		 * A write the grant carries leaves the labels only having to reach the errand at read, since the grant has
		 * already said the user may perform the operation. Where no grant applies, the labels are the only axis there is
		 * and carry the write themselves, which is what every namespace holding its labels alone relies on. The errand
		 * itself is never vouched for by a grant - it is what the labels are held against.
		 */
		public Access.AccessLevelEnum requiredLabelLevel(ProtectedResource resource, Access.AccessLevelEnum required) {
			if (RW != required) {
				return R;
			}
			return applied && ProtectedResource.ERRAND != resource ? R : RW;
		}
	}

	/**
	 * The grant sent in namespace applies to sent in resource, read from the snapshot already in hand.
	 */
	public static ResourceGrant resourceGrant(NamespaceConfig config, AccessSnapshot access, ProtectedResource resource) {
		return config.isResourceAccessControl()
			? new ResourceGrant(true, access.resources().get(resource))
			: new ResourceGrant(false, null);
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
	private static boolean grantsLimitedReadAccess(NamespaceConfig config, ProtectedResource resource, Access.AccessLevelEnum required) {
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
	private static boolean grantsReporterAccess(NamespaceConfig config, ProtectedResource resource, Access.AccessLevelEnum required) {
		return ofNullable(config.getReporterAccess())
			.map(ReporterAccess::getResources)
			.orElse(emptyList()).stream()
			.anyMatch(resourceAccess -> resource == resourceAccess.getResource() && satisfies(toAccessLevelEnum(resourceAccess.getLevel()), required));
	}

	/**
	 * Extracts the ad account of sent in identifier. Labels are only resolved for ad accounts, and reporterUserId holds an
	 * ad account, so any other identifier type can never match.
	 */
	public static String adAccountOf(Identifier user) {
		return ofNullable(user)
			.filter(identifier -> Identifier.Type.AD_ACCOUNT.equals(identifier.getType()))
			.map(Identifier::getValue)
			.orElse(null);
	}

	/**
	 * Translates a level configured on this API into the client enum the service layer compares with. The two enums
	 * carry the same names, and are kept apart so that a change to the access mapper contract cannot alter this API.
	 */
	public static Access.AccessLevelEnum toAccessLevelEnum(final AccessLevel level) {
		return isNull(level) ? null : Access.AccessLevelEnum.valueOf(level.name());
	}

	/**
	 * Signals if a granted access level is enough for the required one, ordered LR before R before RW.
	 */
	public static boolean satisfies(Access.AccessLevelEnum granted, Access.AccessLevelEnum required) {
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
}
