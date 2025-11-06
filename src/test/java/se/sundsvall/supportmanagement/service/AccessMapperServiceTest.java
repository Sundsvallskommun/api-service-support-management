package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;

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
		var labels = accessMapperService.getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER, List.of(RW, R, LR));

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
		var labels = accessMapperService.getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER, List.of(R));

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
		var labels = accessMapperService.getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, IDENTIFIER, List.of(RW, R, LR));

		// Verify
		assertThat(labels).isEmpty();
		verify(accessMapperClientMock).getAccessDetails(MUNICIPALITY_ID, NAMESPACE, AD_USER, "label");
		verifyNoInteractions(metadataServiceMock);
	}

	private List<AccessGroup> createAccessGroup() {
		var accessRead = new Access().accessLevel(R).pattern(ACCESS_PATTERN_R);
		var accessReadWrite = new Access().accessLevel(RW).pattern(ACCESS_PATTERN_RW);
		var accessLimitedRead = new Access().accessLevel(LR).pattern(ACCESS_PATTERN_LR);
		var accessType = new AccessType().access(List.of(accessRead, accessReadWrite, accessLimitedRead));
		return List.of(new AccessGroup().accessByType(List.of(accessType)));
	}
}
