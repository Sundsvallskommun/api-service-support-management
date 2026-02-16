package se.sundsvall.supportmanagement.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.integration.db.ErrandsRepository;
import se.sundsvall.supportmanagement.integration.db.model.AccessLabelEmbeddable;
import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.service.config.NamespaceConfigService;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;
import static org.zalando.problem.Status.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.hasAllowedMetadataLabels;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String ERRAND_ID = "errandId";

	@Mock
	private AccessMapperService accessMapperService;

	@Mock
	private NamespaceConfigService namespaceConfigServiceMock;

	@Mock
	private ErrandsRepository errandsRepositoryMock;

	@Captor
	private ArgumentCaptor<Specification<ErrandEntity>> specificationCaptor;

	@InjectMocks
	private AccessControlService accessControlService;

	@Test
	void limitedMappingPredicateByLabelShouldReturnTrueIfActive() {

		// Setup
		final var user = Identifier.create();
		final var accessLabel1 = AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1");
		final var accessLabel2 = AccessLabelEmbeddable.create().withMetadataLabelId("label-id-2");
		final var errand = ErrandEntity.create().withAccessLabels(List.of(accessLabel1, accessLabel2));
		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());

		// Act
		final var result = accessControlService.limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user)
			.test(errand);

		// Verify
		assertThat(result).isTrue();
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(eq(MUNICIPALITY_ID), eq(NAMESPACE), same(user), eq(List.of(R, RW)));

	}

	@Test
	void limitedMappingPredicateByLabelShouldReturnFalseIfActive() {

		// Setup
		final var user = Identifier.create();
		final var label1 = MetadataLabelEntity.create().withId("label-id-1");
		final var label2 = MetadataLabelEntity.create().withId("label-id-2");
		final var accessLabel1 = AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1");
		final var accessLabel2 = AccessLabelEmbeddable.create().withMetadataLabelId("label-id-2");
		final var errand = ErrandEntity.create().withAccessLabels(List.of(accessLabel1, accessLabel2));

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of(label1, label2));

		// Act
		final var result = accessControlService.limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user)
			.test(errand);

		// Verify
		assertThat(result).isFalse();
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(eq(MUNICIPALITY_ID), eq(NAMESPACE), same(user), eq(List.of(R, RW)));
	}

	@Test
	void limitedMappingPredicateByLabelShouldReturnFalseIfInactive() {
		// Setup
		final var user = Identifier.create();
		final var errand = ErrandEntity.create();

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(false));

		// Act
		final var result = accessControlService.limitedMappingPredicateByLabel(NAMESPACE, MUNICIPALITY_ID, user)
			.test(errand);

		// Verify
		assertThat(result).isFalse();
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(accessMapperService);

	}

	@Test
	void withAccessControlOff() {
		// Setup
		final var config = NamespaceConfig.create().withAccessControl(false);
		final var user = Identifier.create();

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);

		// Act
		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);

		// Verify
		assertThat(specification).usingRecursiveComparison().isEqualTo((Specification<ErrandEntity>) (root, query, criteriaBuilder) -> criteriaBuilder.conjunction());
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);

	}

	@Test
	void withAccessControlEnabled() {
		// Setup
		final var config = NamespaceConfig.create().withAccessControl(true);
		final var user = Identifier.create();
		final var allowedLabels = Set.of(MetadataLabelEntity.create());

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		// Act
		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user);

		// Verify
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, user, List.of(LR, R, RW));
	}

	@Test
	void getErrand() {
		// Setup
		final var entity = ErrandEntity.create();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);
		final var config = NamespaceConfig.create().withAccessControl(true);
		final var allowedLabels = Set.of(MetadataLabelEntity.create());

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));

		// Act
		final var result = accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false);

		// Verify
		assertThat(result).isSameAs(entity);
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, user, List.of(LR, R, RW));
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(hasAllowedMetadataLabels(allowedLabels)));
	}

	@Test
	void getErrandWithLock() {
		// Setup
		final var entity = ErrandEntity.create();
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);
		final var config = NamespaceConfig.create().withAccessControl(true);
		final var allowedLabels = Set.of(MetadataLabelEntity.create());

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));

		// Act
		final var result = accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true);

		// Verify
		assertThat(result).isSameAs(entity);
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, user, List.of(LR, R, RW));
		verify(errandsRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(hasAllowedMetadataLabels(allowedLabels)));
	}

	@Test
	void getErrandNotFound() {
		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(false);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false));

		// Verify
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(namespaceConfigServiceMock, accessMapperService);
	}

	@Test
	void getErrandUnauthorized() {
		// Setup
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);
		final var config = NamespaceConfig.create().withAccessControl(true);
		final var allowedLabels = Set.of(MetadataLabelEntity.create());

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.empty());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false));

		// Verify
		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getTitle()).isEqualTo(UNAUTHORIZED.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Errand not accessible by user 'user'");
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, user, List.of(LR, R, RW));
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).findOne(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(hasAllowedMetadataLabels(allowedLabels)));
	}

	@Test
	void verifyExistingErrandAndAuthorization() {
		// Setup
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);
		final var config = NamespaceConfig.create().withAccessControl(true);
		final var allowedLabels = Set.of(MetadataLabelEntity.create());

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.exists(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(true);

		// Act
		accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID);

		// Verify
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, user, List.of(LR, R, RW));
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).exists(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(hasAllowedMetadataLabels(allowedLabels)));
	}

	@Test
	void verifyExistingErrandAndAuthorizationNotFound() {
		// Mock
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(false);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Verify
		assertThat(exception.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(exception.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Not Found: An errand with id 'errandId' could not be found in namespace 'namespace' for municipality with id 'municipalityId'");

		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(namespaceConfigServiceMock, accessMapperService);
	}

	@Test
	void verifyExistingErrandAndAuthorizationNotAuthorized() {
		// Setup
		final var user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("user");
		Identifier.set(user);
		final var config = NamespaceConfig.create().withAccessControl(true);
		final var allowedLabels = Set.of(MetadataLabelEntity.create());

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.exists(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(false);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID));

		// Verify
		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getTitle()).isEqualTo(UNAUTHORIZED.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Errand not accessible by user 'user'");
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, user, List.of(LR, R, RW));
		verify(errandsRepositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(errandsRepositoryMock).exists(specificationCaptor.capture());
		assertThat(specificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(withId(ERRAND_ID).and(hasAllowedMetadataLabels(allowedLabels)));
	}
}
