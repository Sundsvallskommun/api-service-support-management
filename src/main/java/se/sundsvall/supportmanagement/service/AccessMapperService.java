package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.AccessGroup;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.accessmapper.AccessMapperClient;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Component
public class AccessMapperService {

	private static final String LABEL_TYPE = "label";
	private static final String ACCESSIBLE_LABELS_CACHE_NAME = "accessibleLabelsCache";
	private static final Logger LOG = LoggerFactory.getLogger(AccessMapperService.class);

	private final AccessMapperClient accessMapperClient;
	private final MetadataService metadataService;

	public AccessMapperService(final AccessMapperClient accessMapperClient, final MetadataService metadataService) {
		this.accessMapperClient = accessMapperClient;
		this.metadataService = metadataService;
	}

	@Cacheable(value = ACCESSIBLE_LABELS_CACHE_NAME,
		key = "{#root.methodName, #municipalityId, #namespace, #user, T(se.sundsvall.supportmanagement.service.util.ServiceUtil).createCacheKey(#filter)}")
	public Set<MetadataLabelEntity> getAccessibleLabels(String municipalityId, String namespace, Identifier user, List<Access.AccessLevelEnum> filter) {
		final var logNamespace = sanitizeForLogging(namespace);
		final var logMunicipalityId = sanitizeForLogging(municipalityId);
		LOG.info("Renewing accessible labels of requested access to {} for user {} within namespace {} and municipality {}", filter, user, logNamespace, logMunicipalityId);

		return ofNullable(user)
			.filter(identifier -> Identifier.Type.AD_ACCOUNT.equals(identifier.getType()))
			.map(ad -> accessMapperClient.getAccessDetails(municipalityId, namespace, ad.getValue(), LABEL_TYPE))
			.filter(response -> response.getStatusCode().is2xxSuccessful())
			.map(ResponseEntity::getBody)
			.map(accessGroups -> metadataService.patternToLabels(namespace, municipalityId, toPatterns(accessGroups, filter)))
			.orElse(Collections.emptySet());
	}

	private List<String> toPatterns(List<AccessGroup> accessGroups, List<Access.AccessLevelEnum> filter) {
		return accessGroups.stream().flatMap(accessGroup -> accessGroup.getAccessByType().stream())
			.flatMap(accessType -> accessType.getAccess().stream())
			.filter(access -> filter.contains(access.getAccessLevel()))
			.map(Access::getPattern)
			.toList();
	}
}
