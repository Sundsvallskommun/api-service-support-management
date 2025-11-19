package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.AccessGroup;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.accessmapper.AccessMapperClient;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

@Component
public class AccessMapperService {

	private static final String LABEL_TYPE = "label";
	private final AccessMapperClient accessMapperClient;
	private final MetadataService metadataService;

	public AccessMapperService(final AccessMapperClient accessMapperClient, final MetadataService metadataService) {
		this.accessMapperClient = accessMapperClient;
		this.metadataService = metadataService;
	}

	// Add cache
	public Set<MetadataLabelEntity> getAccessibleLabels(String municipalityId, String namespace, Identifier user, List<Access.AccessLevelEnum> filter) {
		return Optional.ofNullable(user)
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
