package se.sundsvall.supportmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.accessmapper.Access;
import generated.se.sundsvall.accessmapper.AccessGroup;
import generated.se.sundsvall.accessmapper.AccessType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;

@ExtendWith(MockitoExtension.class)
class AccessControlTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";

	@Mock
	private MetadataService metadataServiceMock;

	@InjectMocks
	private AccessControl accessControl;

	@Captor
	private ArgumentCaptor<List<String>> listArgumentCaptor;

	@Test
	void limitedMappingPredicateByLabelShouldReturnTrue() {

		// Setup
		var label1ResourcePath = "label/1";
		var label2ResourcePath = "label/2";
		var pattern = "label/**";
		var label1 = MetadataLabelEntity.create().withResourcePath(label1ResourcePath);
		var label2 = MetadataLabelEntity.create().withResourcePath(label2ResourcePath);
		var errandLabel1 = ErrandLabelEmbeddable.create();
		var errandLabel2 = ErrandLabelEmbeddable.create();
		ReflectionTestUtils.setField(errandLabel1, "metadataLabel", label1);
		ReflectionTestUtils.setField(errandLabel2, "metadataLabel", label2);

		var access = new Access();
		access.setAccessLevel(Access.AccessLevelEnum.LR);
		var accessType = new AccessType();
		accessType.setType("label");
		accessType.setAccess(List.of(access));
		access.setPattern(pattern);
		var accessGroupLimitedRead = new AccessGroup();
		accessGroupLimitedRead.setAccessByType(List.of(accessType));

		var errand = ErrandEntity.create().withLabels(List.of(errandLabel1, errandLabel2));

		// Mock
		when(metadataServiceMock.patternToLabels(any(), any(), any())).thenReturn(Set.of());

		// Act
		var result = accessControl.limitedMappingPredicateByLabel(MUNICIPALITY_ID, NAMESPACE, List.of(accessGroupLimitedRead))
			.test(errand);

		// Verify
		assertThat(result).isTrue();
		verify(metadataServiceMock).patternToLabels(eq(NAMESPACE), eq(MUNICIPALITY_ID), listArgumentCaptor.capture());
		assertThat(listArgumentCaptor.getValue()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = Access.AccessLevelEnum.class, names = {
		"R", "RW"
	})
	void limitedMappingPredicateByLabelShouldReturnFalse(Access.AccessLevelEnum levelEnum) {

		// Setup
		var label1ResourcePath = "label/1";
		var label2ResourcePath = "label/2";
		var pattern = "label/**";
		var label1 = MetadataLabelEntity.create().withResourcePath(label1ResourcePath);
		var label2 = MetadataLabelEntity.create().withResourcePath(label2ResourcePath);
		var errandLabel1 = ErrandLabelEmbeddable.create();
		var errandLabel2 = ErrandLabelEmbeddable.create();
		ReflectionTestUtils.setField(errandLabel1, "metadataLabel", label1);
		ReflectionTestUtils.setField(errandLabel2, "metadataLabel", label2);

		var access = new Access();
		access.setAccessLevel(levelEnum);
		var accessType = new AccessType();
		accessType.setType("label");
		accessType.setAccess(List.of(access));
		access.setPattern(pattern);
		var accessGroupLimitedRead = new AccessGroup();
		accessGroupLimitedRead.setAccessByType(List.of(accessType));

		var errand = ErrandEntity.create().withLabels(List.of(errandLabel1, errandLabel2));

		// Mock
		when(metadataServiceMock.patternToLabels(any(), any(), any())).thenReturn(Set.of(label1, label2));

		// Act
		var result = accessControl.limitedMappingPredicateByLabel(MUNICIPALITY_ID, NAMESPACE, List.of(accessGroupLimitedRead))
			.test(errand);

		// Verify
		assertThat(result).isFalse();
		verify(metadataServiceMock).patternToLabels(eq(NAMESPACE), eq(MUNICIPALITY_ID), listArgumentCaptor.capture());
		assertThat(listArgumentCaptor.getValue()).isEqualTo(List.of(pattern));
	}
}
