package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.AccessGroup;
import generated.se.sundsvall.accessmapper.AccessType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.accessmapper.AccessMapperClient;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;

@ExtendWith(MockitoExtension.class)
class AccessMapperServiceTest {

	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String NAMESPACE = "namespace";
	private static final String AD_USER = "adUser";
	private static final Identifier IDENTIFIER = Identifier.create().withType(AD_ACCOUNT).withValue(AD_USER);
	private static final String ACCESS_PATTERN_R = "accessPatternR";
	private static final String ACCESS_PATTERN_RW = "accessPatternRW";
	private static final String ACCESS_PATTERN_LR = "accessPatternLR";
	private static final MetadataLabelEntity METADATA_LABEL_ENTITY = MetadataLabelEntity.create();

	@Mock
	private AccessMapperClient accessMapperClientMock;

	@Mock
	private MetadataService metadataServiceMock;

	@InjectMocks
	private AccessMapperService accessMapperService;

	@Test
	void getAccessibleLabelsSuccessful() {
		// Mock
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup())));
		when(metadataServiceMock.patternToLabels(any(), any(), any())).thenReturn(Set.of(METADATA_LABEL_ENTITY));

		// Act
		final var labels = accessMapperService.getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER, List.of(RW, R, LR));

		// Verify
		assertThat(labels).containsExactly(METADATA_LABEL_ENTITY);
		verify(accessMapperClientMock).getAccessDetails(MUNICIPALITY_ID, NAMESPACE, AD_USER, "label");
		verify(metadataServiceMock).patternToLabels(NAMESPACE, MUNICIPALITY_ID, List.of(ACCESS_PATTERN_R, ACCESS_PATTERN_RW, ACCESS_PATTERN_LR));
	}

	@Test
	void getAccessibleLabelsSuccessfulWithFilter() {
		// Mock
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup())));
		when(metadataServiceMock.patternToLabels(any(), any(), any())).thenReturn(Set.of(METADATA_LABEL_ENTITY));

		// Act
		final var labels = accessMapperService.getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER, List.of(R));

		// Verify
		assertThat(labels).containsExactly(METADATA_LABEL_ENTITY);
		verify(accessMapperClientMock).getAccessDetails(MUNICIPALITY_ID, NAMESPACE, AD_USER, "label");
		verify(metadataServiceMock).patternToLabels(NAMESPACE, MUNICIPALITY_ID, List.of(ACCESS_PATTERN_R));
	}

	@Test
	void getAccessibleLabelsFail() {
		// Mock
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.badRequest().build());

		// Act
		final var labels = accessMapperService.getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER, List.of(RW, R, LR));

		// Verify
		assertThat(labels).isEmpty();
		verify(accessMapperClientMock).getAccessDetails(MUNICIPALITY_ID, NAMESPACE, AD_USER, "label");
		verifyNoInteractions(metadataServiceMock);
	}

	private List<AccessGroup> createAccessGroup() {
		return createAccessGroup("label");
	}

	private List<AccessGroup> createAccessGroup(final String type) {
		final var accessRead = new Access().accessLevel(R).pattern(ACCESS_PATTERN_R);
		final var accessReadWrite = new Access().accessLevel(RW).pattern(ACCESS_PATTERN_RW);
		final var accessLimitedRead = new Access().accessLevel(LR).pattern(ACCESS_PATTERN_LR);
		final var accessType = new AccessType().type(type).access(List.of(accessRead, accessReadWrite, accessLimitedRead));
		return List.of(new AccessGroup().accessByType(List.of(accessType)));
	}

	@Test
	void getAccessibleLabelsIgnoresOtherTypes() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup("role"))));
		when(metadataServiceMock.patternToLabels(any(), any(), any())).thenReturn(Set.of());

		accessMapperService.getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER, List.of(RW, R, LR));

		verify(metadataServiceMock).patternToLabels(NAMESPACE, MUNICIPALITY_ID, List.of());
	}

	@Test
	void getAccessibleRolesSuccessful() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup("role"))));

		final var roles = accessMapperService.getAccessibleRoles(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		assertThat(roles).containsExactlyInAnyOrder(ACCESS_PATTERN_R.toUpperCase(), ACCESS_PATTERN_RW.toUpperCase(), ACCESS_PATTERN_LR.toUpperCase());
		verify(accessMapperClientMock).getAccessDetails(MUNICIPALITY_ID, NAMESPACE, AD_USER, "role");
		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void getAccessibleRolesIgnoresOtherTypes() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup("label"))));

		assertThat(accessMapperService.getAccessibleRoles(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER)).isEmpty();
	}

	@Test
	void getAccessibleResourcesMatchesPatternsAgainstResourcePaths() {
		final var accessType = new AccessType().type("resource").access(List.of(
			new Access().accessLevel(R).pattern("errand/**"),
			new Access().accessLevel(RW).pattern("errand/communication/**")));
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(List.of(new AccessGroup().accessByType(List.of(accessType))))));

		final var resources = accessMapperService.getAccessibleResources(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		// "errand/**" covers the errand itself along with everything below it, and the more specific communication
		// pattern raises those two to RW. Resources belonging to the namespace rather than to an errand are not covered.
		assertThat(resources).containsEntry(ProtectedResource.ERRAND, R)
			.containsEntry(ProtectedResource.NOTE, R)
			.containsEntry(ProtectedResource.COMMUNICATION, RW)
			.containsEntry(ProtectedResource.COMMUNICATION_ATTACHMENT, RW)
			.doesNotContainKeys(ProtectedResource.NAMESPACE_CONFIG, ProtectedResource.METADATA_LABEL);
		verify(accessMapperClientMock).getAccessDetails(MUNICIPALITY_ID, NAMESPACE, AD_USER, "resource");
	}

	@Test
	void getAccessibleResourcesKeepsTheMostPermissiveLevel() {
		final var accessType = new AccessType().type("resource").access(List.of(
			new Access().accessLevel(LR).pattern("errand/note"),
			new Access().accessLevel(RW).pattern("errand/note"),
			new Access().accessLevel(R).pattern("errand/note")));
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(List.of(new AccessGroup().accessByType(List.of(accessType))))));

		assertThat(accessMapperService.getAccessibleResources(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER)).containsEntry(ProtectedResource.NOTE, RW);
	}

	@Test
	void getAccessibleResourcesIgnoresOtherTypes() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup("label"))));

		assertThat(accessMapperService.getAccessibleResources(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER)).isEmpty();
	}

	@Test
	void getAccessibleResourcesFail() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.badRequest().build());

		assertThat(accessMapperService.getAccessibleResources(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER)).isEmpty();
	}

	@Test
	void getAccessibleResourcesForNonAdIdentifier() {
		assertThat(accessMapperService.getAccessibleResources(MUNICIPALITY_ID, NAMESPACE, Identifier.create().withType(Identifier.Type.PARTY_ID).withValue(AD_USER))).isEmpty();
		verifyNoInteractions(accessMapperClientMock);
	}

	@Test
	void getAccessibleRolesFail() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any(), any())).thenReturn(ResponseEntity.badRequest().build());

		assertThat(accessMapperService.getAccessibleRoles(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER)).isEmpty();
	}

	@Test
	void getAccessibleRolesForNonAdIdentifier() {
		assertThat(accessMapperService.getAccessibleRoles(MUNICIPALITY_ID, NAMESPACE, Identifier.create().withType(Identifier.Type.PARTY_ID).withValue(AD_USER))).isEmpty();
		verifyNoInteractions(accessMapperClientMock);
	}
}
