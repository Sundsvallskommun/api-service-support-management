package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.AccessGroup;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.accessmapper.AccessMapperClient;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.model.AccessSnapshot;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Component
public class AccessMapperService {

	private static final String LABEL_TYPE = "label";
	private static final String ROLE_TYPE = "role";
	private static final String RESOURCE_TYPE = "resource";

	/**
	 * Access levels from least to most permissive.
	 */
	private static final List<Access.AccessLevelEnum> LEVEL_ORDER = List.of(Access.AccessLevelEnum.LR, Access.AccessLevelEnum.R, Access.AccessLevelEnum.RW);

	private static final String ACCESSIBLE_LABELS_CACHE_NAME = "accessibleLabelsCache";
	private static final Logger LOG = LoggerFactory.getLogger(AccessMapperService.class);

	private final AccessMapperClient accessMapperClient;
	private final MetadataService metadataService;
	private final AntPathMatcher pathMatcher;

	public AccessMapperService(final AccessMapperClient accessMapperClient, final MetadataService metadataService) {
		this.accessMapperClient = accessMapperClient;
		this.metadataService = metadataService;
		this.pathMatcher = new AntPathMatcher();
		this.pathMatcher.setCaseSensitive(false);
	}

	/**
	 * Resolves everything the access mapper says about the user within the namespace: the labels saying which errands
	 * they reach, the roles selecting what they see of an errand, and the resources saying which operations they may
	 * perform at all.
	 * <p>
	 * All three come out of one answer of the access mapper, since the endpoint filters by type but is no cheaper for it,
	 * and one answer is a single moment in time rather than three that may disagree. Held in one cache entry per user and
	 * namespace, so that a request needing all three costs one call rather than one per type.
	 *
	 * @param  municipalityId municipality id
	 * @param  namespace      namespace
	 * @param  user           user
	 * @return                what the access mapper grants the user, empty for anyone it grants nothing
	 */
	@Cacheable(value = ACCESSIBLE_LABELS_CACHE_NAME, key = "{#municipalityId, #namespace, #user}")
	public AccessSnapshot getAccessSnapshot(String municipalityId, String namespace, Identifier user) {
		final var logNamespace = sanitizeForLogging(namespace);
		final var logMunicipalityId = sanitizeForLogging(municipalityId);
		LOG.info("Renewing access of user {} within namespace {} and municipality {}", user, logNamespace, logMunicipalityId);

		return ofNullable(user)
			.filter(identifier -> Identifier.Type.AD_ACCOUNT.equals(identifier.getType()))
			.map(ad -> accessMapperClient.getAccessDetails(municipalityId, namespace, ad.getValue()))
			.filter(response -> response.getStatusCode().is2xxSuccessful())
			.map(ResponseEntity::getBody)
			.map(accessGroups -> new AccessSnapshot(
				toLabelsByLevel(namespace, municipalityId, accessGroups),
				toRoles(accessGroups),
				toResourceLevels(accessGroups)))
			.orElseGet(AccessSnapshot::empty);
	}

	/**
	 * Resolves the labels the user reaches at each level. Kept apart per level rather than unioned, since which level a
	 * label is granted at is what separates an errand the user reads fully from one they only have limited read for.
	 */
	private Map<Access.AccessLevelEnum, Set<MetadataLabelEntity>> toLabelsByLevel(String namespace, String municipalityId, List<AccessGroup> accessGroups) {
		final Map<Access.AccessLevelEnum, Set<MetadataLabelEntity>> labelsByLevel = new EnumMap<>(Access.AccessLevelEnum.class);

		LEVEL_ORDER.forEach(level -> labelsByLevel.put(level, metadataService.patternToLabels(namespace, municipalityId, toPatterns(accessGroups, LABEL_TYPE, List.of(level)))));

		return labelsByLevel;
	}

	/**
	 * Resolves the roles the user holds within the namespace. These say nothing about which errands the user may reach,
	 * that is decided by their labels, they only select which fields of an errand are returned when the namespace maps
	 * errands per role.
	 */
	private Set<String> toRoles(List<AccessGroup> accessGroups) {
		return toPatterns(accessGroups, ROLE_TYPE, Arrays.asList(Access.AccessLevelEnum.values())).stream()
			.map(String::toUpperCase)
			.collect(toSet());
	}

	/**
	 * Resolves what the user may do with each resource. Patterns are matched against the paths of
	 * {@link ProtectedResource}, so a single pattern may cover a whole subtree, and the most permissive level wins when
	 * several patterns match the same resource.
	 */
	private Map<ProtectedResource, Access.AccessLevelEnum> toResourceLevels(List<AccessGroup> accessGroups) {
		final Map<ProtectedResource, Access.AccessLevelEnum> levels = new EnumMap<>(ProtectedResource.class);

		ofNullable(accessGroups).orElse(emptyList()).stream()
			.flatMap(accessGroup -> ofNullable(accessGroup.getAccessByType()).orElse(emptyList()).stream())
			.filter(accessType -> RESOURCE_TYPE.equalsIgnoreCase(accessType.getType()))
			.flatMap(accessType -> ofNullable(accessType.getAccess()).orElse(emptyList()).stream())
			.filter(access -> nonNull(access.getPattern()) && nonNull(access.getAccessLevel()))
			.forEach(access -> Arrays.stream(ProtectedResource.values())
				.filter(errandResource -> pathMatcher.match(access.getPattern(), errandResource.getPath()))
				.forEach(errandResource -> levels.merge(errandResource, access.getAccessLevel(), AccessMapperService::mostPermissive)));

		return levels;
	}

	/**
	 * Picks the most permissive of two access levels, ordered LR before R before RW.
	 */
	private static Access.AccessLevelEnum mostPermissive(Access.AccessLevelEnum left, Access.AccessLevelEnum right) {
		return LEVEL_ORDER.indexOf(left) >= LEVEL_ORDER.indexOf(right) ? left : right;
	}

	private List<String> toPatterns(List<AccessGroup> accessGroups, String type, List<Access.AccessLevelEnum> filter) {
		return ofNullable(accessGroups).orElse(emptyList()).stream()
			.flatMap(accessGroup -> ofNullable(accessGroup.getAccessByType()).orElse(emptyList()).stream())
			.filter(accessType -> type.equalsIgnoreCase(accessType.getType()))
			.flatMap(accessType -> ofNullable(accessType.getAccess()).orElse(emptyList()).stream())
			.filter(access -> filter.contains(access.getAccessLevel()))
			.map(Access::getPattern)
			.toList();
	}
}
