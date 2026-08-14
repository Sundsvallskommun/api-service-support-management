package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
	 * Fields a limited read falls back to when the namespace has not said what limited read exposes. Keeps a namespace
	 * from widening what a limited read user sees simply by switching role based mapping on, and is overridden by
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
	 * Signals if errands of the namespace are mapped according to the fields configured for the roles held by the
	 * requesting user.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @return                true if role based mapping is active for the namespace
	 */
	public boolean hasRoleBasedMappingActive(String namespace, String municipalityId) {
		final var config = namespaceConfigService.get(namespace, municipalityId);
		return config.isAccessControl() && config.isRoleBasedMapping();
	}

	/**
	 * Resolves which fields of an errand the requesting user may see.
	 * <p>
	 * An errand their labels only grant limited read for uses the limited read fields of the namespace, instead of the
	 * fields of any role they hold, since a role says what they see of errands they properly have access to. Fields given
	 * to the reporter of an errand union on top, so someone who both reported an errand and handles it keeps the fuller
	 * view.
	 * <p>
	 * An empty result means no restriction applies and the errand is mapped in full, which is what an unrestricted role
	 * yields. A limited read never resolves to empty, so it can never be mapped in full.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @return                resolver of fields, and the keys to limit them to, per errand
	 */
	public Function<ErrandEntity, Map<ErrandField, Set<String>>> roleBasedFieldResolver(String namespace, String municipalityId, Identifier user) {
		final var config = namespaceConfigService.get(namespace, municipalityId);
		final var adAccount = adAccountOf(user);

		// R/RW has precedence over LR, so an errand fully covered by them is not limited for this user.
		final var fullReadLabelIds = accessMapperService.getAccessibleLabels(municipalityId, namespace, user, List.of(R, RW)).stream()
			.map(MetadataLabelEntity::getId)
			.collect(Collectors.toSet());

		final var namespaceRoles = accessMapperService.getAccessibleRoles(municipalityId, namespace, user);

		return errandEntity -> {
			final var applicable = new ArrayList<FieldAccess>();
			final var limited = isLimited(fullReadLabelIds, errandEntity);

			if (limited) {
				applicable.addAll(ofNullable(config.getLimitedReadAccess()).map(LimitedReadAccess::getFields).orElse(emptyList()));
			} else {
				ofNullable(config.getRoleFieldRestrictions()).orElse(emptyList()).stream()
					.filter(restriction -> nonNull(restriction.getRole()) && namespaceRoles.contains(restriction.getRole().toUpperCase()))
					.forEach(restriction -> applicable.addAll(ofNullable(restriction.getFields()).orElse(emptyList())));
			}

			if (isReporter(adAccount, errandEntity)) {
				applicable.addAll(ofNullable(config.getReporterAccess()).map(ReporterAccess::getFields).orElse(emptyList()));
			}

			// An errand that is limited for the user is never returned in full. Nothing resolving here means the namespace
			// has not said what limited read exposes, and the safe reading of limited is the minimum rather than everything.
			if (limited && applicable.isEmpty()) {
				applicable.addAll(DEFAULT_LIMITED_READ_FIELDS);
			}

			return applicable.stream().collect(Collectors.toMap(
				FieldAccess::getField,
				fieldAccess -> new LinkedHashSet<>(ofNullable(fieldAccess.getKeys()).orElse(emptyList())),
				AccessControlService::mergeKeys,
				LinkedHashMap::new));
		};
	}

	/**
	 * Signals if the labels of the user fail to cover the errand fully, meaning the access mapper granted them limited
	 * read for it.
	 */
	private static boolean isLimited(Set<String> fullReadLabelIds, ErrandEntity errandEntity) {
		return !fullReadLabelIds.containsAll(ofNullable(errandEntity.getAccessLabels()).orElse(emptyList()).stream()
			.map(AccessLabelEmbeddable::getMetadataLabelId)
			.collect(Collectors.toSet()));
	}

	private static boolean isReporter(String adAccount, ErrandEntity errandEntity) {
		return nonNull(adAccount) && adAccount.equalsIgnoreCase(errandEntity.getReporterUserId());
	}

	/**
	 * Tells which keys of a keyed field the user may read on sent in errand, so that the endpoints serving that data on
	 * its own honour the same grants as the errand payload does.
	 * <p>
	 * Every key is readable when the namespace does not map errands per role, and when the user holds no configured role,
	 * both of which are the cases where the whole errand is returned. A field the matched roles do not expose at all
	 * yields no readable keys.
	 *
	 * @param  namespace      namespace
	 * @param  municipalityId municipality id
	 * @param  user           user
	 * @param  errandEntity   errand the field belongs to
	 * @param  field          field to read
	 * @return                predicate accepting the keys the user may read
	 */
	public Predicate<String> readableKeyPredicate(String namespace, String municipalityId, Identifier user, ErrandEntity errandEntity, ErrandField field) {
		if (!hasRoleBasedMappingActive(namespace, municipalityId)) {
			return _ -> true;
		}

		final var fields = roleBasedFieldResolver(namespace, municipalityId, user).apply(errandEntity);
		if (fields.isEmpty()) {
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

		final var accessibleKey = readableKeyPredicate(namespace, municipalityId, Identifier.get(), errandEntity, field);

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
	 * not know about, never reduce what it already granted.
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

		// Labels say which errands the user reaches, the access mapper resources say which operations they may perform at
		// all, and both must allow.
		if (grantsResourceAccess(config, namespace, municipalityId, user, resource, required)) {
			// One clause at the lowest label level that reaches this resource. A separate clause for limited read would be
			// redundant, since the labels of a level are a subset of those of every level below it and the predicate is
			// monotonic, so the stricter clause can never match a row the looser one does not.
			final var lowestLevel = grantsLimitedReadAccess(config, resource, required) ? LR : fullAccessLevel(required);

			clauses.add(hasAllowedMetadataLabels(accessMapperService.getAccessibleLabels(municipalityId, namespace, user, levelsAtOrAbove(lowestLevel))));
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
	 * inert until the access mapper has been configured for the namespace.
	 */
	private boolean grantsResourceAccess(NamespaceConfig config, String namespace, String municipalityId, Identifier user, ProtectedResource resource, Access.AccessLevelEnum required) {
		if (!config.isResourceAccessControl()) {
			return true;
		}
		return ofNullable(accessMapperService.getAccessibleResources(municipalityId, namespace, user).get(resource))
			.filter(granted -> satisfies(granted, fullAccessLevel(required)))
			.isPresent();
	}

	/**
	 * Signals if limited read reaches sent in resource. Whether an errand is limited for the user is settled by their
	 * labels, so within limited read a resource is simply reachable or not and carries no level of its own. Operations
	 * asking for more than limited read are never satisfied by it.
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
			.anyMatch(resourceAccess -> resource == resourceAccess.getResource() && satisfies(resourceAccess.getLevel(), required));
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

	private boolean hasAccessControlActive(String namespace, String municipalityId) {
		return namespaceConfigService.get(namespace, municipalityId).isAccessControl();
	}

	/**
	 * Verifies that the requesting user may reach a resource belonging to the namespace itself rather than to any errand,
	 * such as its configuration or its metadata. Labels say nothing about these, so the access mapper resources decide on
	 * their own.
	 * <p>
	 * Enforced whenever access control is active for the namespace. A namespace without configuration enforces nothing,
	 * since access control cannot be active without it, which is also what lets a configuration be created in the first
	 * place. Because the check reads the persisted configuration, switching access control off is itself guarded.
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

		final var granted = accessMapperService.getAccessibleResources(municipalityId, namespace, Identifier.get()).get(resource);

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
	 * @param  id             errand id
	 * @param  lock           db row locking enable if true
	 * @param  resource       resource being guarded
	 * @param  required       lowest access level accepted for the operation
	 * @return                errand entity
	 */
	public ErrandEntity getErrand(final String namespace, final String municipalityId, final String id, boolean lock, ProtectedResource resource, Access.AccessLevelEnum required) {
		verifyExistingErrand(id, namespace, municipalityId, lock);
		return errandsRepository
			.findOne(withId(id).and(withAccessControl(namespace, municipalityId, Identifier.get(), resource, required)))
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
