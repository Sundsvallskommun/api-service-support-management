package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.AccessGroup;
import generated.se.sundsvall.accessmapper.AccessType;
import java.util.List;
import java.util.Map;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
	void getAccessSnapshotResolvesLabelsPerLevelInOneRead() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup())));
		when(metadataServiceMock.patternToLabels(any(), any(), anyMap())).thenReturn(Map.of(LR, Set.of(METADATA_LABEL_ENTITY), R, Set.of(), RW, Set.of()));

		final var snapshot = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		// Kept apart per level, so that a label granted at limited read is not mistaken for one granted at full read,
		// but resolved together so that the levels cannot be answered from different states of the label table.
		assertThat(snapshot.labels(List.of(LR))).containsExactly(METADATA_LABEL_ENTITY);
		assertThat(snapshot.labels(List.of(R, RW))).isEmpty();
		verify(metadataServiceMock).patternToLabels(NAMESPACE, MUNICIPALITY_ID, Map.of(
			LR, List.of(ACCESS_PATTERN_LR),
			R, List.of(ACCESS_PATTERN_R),
			RW, List.of(ACCESS_PATTERN_RW)));
		verifyNoMoreInteractions(metadataServiceMock);
	}

	@Test
	void getAccessSnapshotDropsAccessCarryingNoLevelOrNoPattern() {
		final var accessGroups = List.of(new AccessGroup().accessByType(List.of(
			new AccessType().type("label").access(List.of(new Access().pattern("no-level"), new Access().accessLevel(R).pattern(ACCESS_PATTERN_R))),
			new AccessType().type("role").access(List.of(new Access().accessLevel(R), new Access().accessLevel(R).pattern("case_officer"))),
			new AccessType().type("resource").access(List.of(new Access().pattern("errand"))))));
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(accessGroups)));
		when(metadataServiceMock.patternToLabels(any(), any(), anyMap())).thenReturn(Map.of(LR, Set.of(), R, Set.of(METADATA_LABEL_ENTITY), RW, Set.of()));

		final var snapshot = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		// Neither can be matched against anything, and one of them would take the whole snapshot down with it.
		assertThat(snapshot.roles()).containsExactly("CASE_OFFICER");
		assertThat(snapshot.resources()).isEmpty();
		verify(metadataServiceMock).patternToLabels(NAMESPACE, MUNICIPALITY_ID, Map.of(
			LR, List.of(),
			R, List.of(ACCESS_PATTERN_R),
			RW, List.of()));
	}

	@Test
	void getAccessSnapshotAsksTheAccessMapperOnceForEveryType() {
		final var accessGroups = List.of(new AccessGroup().accessByType(List.of(
			new AccessType().type("label").access(List.of(new Access().accessLevel(R).pattern(ACCESS_PATTERN_R))),
			new AccessType().type("role").access(List.of(new Access().accessLevel(R).pattern("case_officer"))),
			new AccessType().type("resource").access(List.of(new Access().accessLevel(R).pattern("errand"))))));
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(accessGroups)));
		when(metadataServiceMock.patternToLabels(any(), any(), anyMap())).thenReturn(Map.of(LR, Set.of(), R, Set.of(METADATA_LABEL_ENTITY), RW, Set.of()));

		final var snapshot = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		// The type filter of the endpoint is deliberately not sent, so labels, roles and resources come out of one answer.
		assertThat(snapshot.labels(List.of(R))).containsExactly(METADATA_LABEL_ENTITY);
		assertThat(snapshot.roles()).containsExactly("CASE_OFFICER");
		assertThat(snapshot.resources()).containsEntry(ProtectedResource.ERRAND, R);
		verify(accessMapperClientMock).getAccessDetails(MUNICIPALITY_ID, NAMESPACE, AD_USER);
	}

	@Test
	void getAccessSnapshotSeparatesLabelsFromRoles() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup("role"))));
		when(metadataServiceMock.patternToLabels(any(), any(), anyMap())).thenReturn(Map.of(LR, Set.of(), R, Set.of(), RW, Set.of()));

		final var snapshot = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		assertThat(snapshot.labels(List.of(LR, R, RW))).isEmpty();
		assertThat(snapshot.roles()).containsExactlyInAnyOrder(ACCESS_PATTERN_R.toUpperCase(), ACCESS_PATTERN_RW.toUpperCase(), ACCESS_PATTERN_LR.toUpperCase());
		assertThat(snapshot.resources()).isEmpty();
		verify(metadataServiceMock).patternToLabels(NAMESPACE, MUNICIPALITY_ID, Map.of(LR, List.of(), R, List.of(), RW, List.of()));
	}

	@Test
	void getAccessSnapshotIgnoresOtherTypesForResources() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(createAccessGroup("label"))));

		assertThat(accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER).resources()).isEmpty();
	}

	@Test
	void getAccessSnapshotMatchesResourcePatternsAgainstResourcePaths() {
		final var accessType = new AccessType().type("resource").access(List.of(
			new Access().accessLevel(R).pattern("errand/**"),
			new Access().accessLevel(RW).pattern("errand/communication/**")));
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(List.of(new AccessGroup().accessByType(List.of(accessType))))));

		final var resources = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER).resources();

		// "errand/**" covers the errand itself along with everything below it, and the more specific communication
		// pattern raises those two to RW. Resources belonging to the namespace rather than to an errand are not covered.
		assertThat(resources).containsEntry(ProtectedResource.ERRAND, R)
			.containsEntry(ProtectedResource.NOTE, R)
			.containsEntry(ProtectedResource.COMMUNICATION, RW)
			.containsEntry(ProtectedResource.COMMUNICATION_ATTACHMENT, RW)
			.doesNotContainKeys(ProtectedResource.NAMESPACE_CONFIG, ProtectedResource.METADATA_LABEL);
	}

	@Test
	void getAccessSnapshotKeepsTheMostPermissiveResourceLevel() {
		final var accessType = new AccessType().type("resource").access(List.of(
			new Access().accessLevel(LR).pattern("errand/note"),
			new Access().accessLevel(RW).pattern("errand/note"),
			new Access().accessLevel(R).pattern("errand/note")));
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.of(Optional.of(List.of(new AccessGroup().accessByType(List.of(accessType))))));

		assertThat(accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER).resources()).containsEntry(ProtectedResource.NOTE, RW);
	}

	@Test
	void getAccessSnapshotFail() {
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.badRequest().build());

		final var snapshot = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		assertThat(snapshot.labels(List.of(LR, R, RW))).isEmpty();
		assertThat(snapshot.roles()).isEmpty();
		assertThat(snapshot.resources()).isEmpty();
		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void getAccessSnapshotForNonAdIdentifier() {
		final var snapshot = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, Identifier.create().withType(Identifier.Type.PARTY_ID).withValue(AD_USER));

		assertThat(snapshot.labels(List.of(LR, R, RW))).isEmpty();
		assertThat(snapshot.roles()).isEmpty();
		assertThat(snapshot.resources()).isEmpty();
		verifyNoInteractions(accessMapperClientMock);
	}

	@Test
	void notFoundFromAccessMapperYieldsNoAccessRatherThanFailure() {
		// The client bypasses 404, so an unknown user arrives here as a non-2xx response and must resolve to "no grants".
		when(accessMapperClientMock.getAccessDetails(any(), any(), any())).thenReturn(ResponseEntity.notFound().build());

		final var snapshot = accessMapperService.getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER);

		assertThat(snapshot.labels(List.of(R))).isEmpty();
		assertThat(snapshot.roles()).isEmpty();
		assertThat(snapshot.resources()).isEmpty();
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
}
