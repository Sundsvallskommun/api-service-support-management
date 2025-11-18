package se.sundsvall.supportmanagement.service;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.hasAllowedMetadataLabels;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.ErrandLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";

	@Mock
	private AccessMapperService accessMapperService;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@InjectMocks
	private AccessControlService accessControlService;

	@Test
	void limitedMappingPredicateByLabelShouldReturnTrueIfActive() {

		// Setup
		var user = Identifier.create();
		var label1ResourcePath = "label/1";
		var label2ResourcePath = "label/2";
		var label1 = MetadataLabelEntity.create().withResourcePath(label1ResourcePath);
		var label2 = MetadataLabelEntity.create().withResourcePath(label2ResourcePath);
		var errandLabel1 = ErrandLabelEmbeddable.create();
		var errandLabel2 = ErrandLabelEmbeddable.create();
		ReflectionTestUtils.setField(errandLabel1, "metadataLabel", label1);
		ReflectionTestUtils.setField(errandLabel2, "metadataLabel", label2);
		var errand = ErrandEntity.create().withLabels(List.of(errandLabel1, errandLabel2));
		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());

		// Act
		var result = accessControlService.limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user)
			.test(errand);

		// Verify
		assertThat(result).isTrue();
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(eq(MUNICIPALITY_ID), eq(NAMESPACE), same(user), eq(List.of(R, RW)));

	}

	@Test
	void limitedMappingPredicateByLabelShouldReturnFalseIfActive() {

		// Setup
		var user = Identifier.create();
		var label1ResourcePath = "label/1";
		var label2ResourcePath = "label/2";
		var label1 = MetadataLabelEntity.create().withResourcePath(label1ResourcePath);
		var label2 = MetadataLabelEntity.create().withResourcePath(label2ResourcePath);
		var errandLabel1 = ErrandLabelEmbeddable.create();
		var errandLabel2 = ErrandLabelEmbeddable.create();
		ReflectionTestUtils.setField(errandLabel1, "metadataLabel", label1);
		ReflectionTestUtils.setField(errandLabel2, "metadataLabel", label2);
		var errand = ErrandEntity.create().withLabels(List.of(errandLabel1, errandLabel2));

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of(label1, label2));

		// Act
		var result = accessControlService.limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user)
			.test(errand);

		// Verify
		assertThat(result).isFalse();
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(eq(MUNICIPALITY_ID), eq(NAMESPACE), same(user), eq(List.of(R, RW)));
	}

	@Test
	void limitedMappingPredicateByLabelShouldReturnFalseIfInactive() {
		// Setup
		var user = Identifier.create();
		var errand = ErrandEntity.create();

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(false));

		// Act
		var result = accessControlService.limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user)
			.test(errand);

		// Verify
		assertThat(result).isFalse();
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(accessMapperService);

	}

	@Test
	void withAccessControlOff() {
		// Setup
		var config = NamespaceConfig.create().withAccessControl(false);
		var user = Identifier.create();

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);

		// Act
		var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);

		// Verify
		assertThat(specification).usingRecursiveComparison().isEqualTo((Specification<ErrandEntity>) (root, query, criteriaBuilder) -> criteriaBuilder.conjunction());
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);

	}

	@Test
	void withAccessControlEnabled() {
		// Setup
		var config = NamespaceConfig.create().withAccessControl(true);
		var user = Identifier.create();
		var allowedLabels = Set.of(MetadataLabelEntity.create());

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		// Act
		var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);

		// Verify
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, user, List.of(LR, R, RW));
	}
}
