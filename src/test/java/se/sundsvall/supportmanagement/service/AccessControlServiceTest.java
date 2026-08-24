package se.sundsvall.supportmanagement.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;
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
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.config.ResourceAccess;
import se.sundsvall.supportmanagement.api.model.config.RoleFieldRestriction;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.hasAllowedMetadataLabels;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.isReportedBy;
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

	private static final String AD_ACCOUNT = "joe01doe";

	private static Identifier adUser() {
		return Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(AD_ACCOUNT);
	}

	/**
	 * An errand carrying a label the user is not given, so their labels never cover it fully and it is limited for them.
	 */
	private static ErrandEntity limitedErrand() {
		return ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
	}

	private static NamespaceConfig configWithReporterAccess(final List<ResourceAccess> resources, final List<FieldAccess> fields) {
		return NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withReporterAccess(ReporterAccess.create()
				.withResources(resources)
				.withFields(fields));
	}

	@Test
	void roleBasedFieldResolverResolvesFieldsForReporter() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(
			FieldAccess.create().withField(ErrandField.TITLE),
			FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")))));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		// The namespace says nothing about limited read, so its minimum applies and the reporter fields widen it
		assertThat(result).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS, ErrandField.PARAMETERS);
		assertThat(result.get(ErrandField.TITLE)).isEmpty();
		assertThat(result.get(ErrandField.PARAMETERS)).containsExactly("key-1");
	}

	/**
	 * Reporting an errand may never show someone less of it than a limited read user who did not report it. The minimum
	 * is therefore resolved before the reporter fields are merged in, so it acts as a floor rather than as something the
	 * reporter's own field set replaces.
	 */
	@Test
	void roleBasedFieldResolverKeepsTheLimitedReadMinimumForAReporter() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(
			FieldAccess.create().withField(ErrandField.DESCRIPTION))));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS, ErrandField.DESCRIPTION);
	}

	@Test
	void roleBasedFieldResolverLeavesAnOtherwiseUnrestrictedReporterUnrestricted() {
		// Reporting an errand may not reduce access: reporter fields widen a restriction, they never introduce one.
		final var errand = ErrandEntity.create().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.TITLE))));

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
	}

	@Test
	void roleBasedFieldResolverResolvesNothingWhenUserIsNotReporter() {
		final var errand = ErrandEntity.create().withReporterUserId("someone-else");
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.TITLE))));

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
	}

	@Test
	void roleBasedFieldResolverResolvesNothingForNonAdIdentifier() {
		final var errand = ErrandEntity.create().withReporterUserId(AD_ACCOUNT);
		final var partyIdUser = Identifier.create().withType(Identifier.Type.PARTY_ID).withValue(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.TITLE))));

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, partyIdUser).apply(errand)).isNull();
	}

	@Test
	void roleBasedFieldResolverResolvesNothingWhenNamespaceHasNoRoleAccess() {
		final var errand = ErrandEntity.create().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withRoleBasedMapping(true));

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
	}

	@Test
	void roleBasedFieldResolverResolvesFieldsForLimitedLabelAccess() {
		final var errand = ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE)))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.TITLE);
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, adUser(), List.of(R, RW));
	}

	@Test
	void roleBasedFieldResolverResolvesNothingWhenLabelsCoverErrandFully() {
		final var errand = ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE)))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of(MetadataLabelEntity.create().withId("label-id-1")));

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
	}

	@Test
	void roleBasedFieldResolverResolvesFieldsForNamespaceRoleFromAccessMapper() {
		final var errand = ErrandEntity.create();
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("case_officer").withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE))))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());
		when(accessMapperService.getAccessibleRoles(any(), any(), any())).thenReturn(Set.of("CASE_OFFICER"));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.TITLE);
	}

	@Test
	void roleBasedFieldResolverUnionsReporterOnTopOfNamespaceRole() {
		final var errand = ErrandEntity.create().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withReporterAccess(ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")))))
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(
				FieldAccess.create().withField(ErrandField.TITLE),
				FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-2")))))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());
		when(accessMapperService.getAccessibleRoles(any(), any(), any())).thenReturn(Set.of("CASE_OFFICER"));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.PARAMETERS, ErrandField.TITLE);
		assertThat(result.get(ErrandField.PARAMETERS)).containsExactlyInAnyOrder("key-1", "key-2");
	}

	@Test
	void roleBasedFieldResolverPrefersLimitedReadOverRoleFields() {
		final var errand = ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.STATUS))))
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(ErrandField.DESCRIPTION))))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());
		when(accessMapperService.getAccessibleRoles(any(), any(), any())).thenReturn(Set.of("CASE_OFFICER"));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		// The errand is only limited read for this user, so the role fields do not apply to it.
		assertThat(result).containsOnlyKeys(ErrandField.STATUS);
	}

	@Test
	void roleBasedFieldResolverExposesWholeCollectionWhenOneScopeGrantsItUnkeyed() {
		final var errand = ErrandEntity.create().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withReporterAccess(ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")))))
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(ErrandField.PARAMETERS))))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());
		when(accessMapperService.getAccessibleRoles(any(), any(), any())).thenReturn(Set.of("CASE_OFFICER"));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result.get(ErrandField.PARAMETERS)).isEmpty();
	}

	@Test
	void withAccessControlIgnoresAccessMapperResourcesWhenGateIsOff() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(accessMapperService, never()).getAccessibleResources(any(), any(), any());
	}

	@Test
	void withAccessControlKeepsLabelClauseWhenResourceIsGranted() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.COMMUNICATION, RW));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void withAccessControlAcceptsAResourceGrantedAtLimitedReadForALimitedReadOperation() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.ERRAND, LR));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.ERRAND, LR);

		// An explicitly granted limited read resource may not behave the same as no grant at all.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, adUser(), List.of(LR, R, RW));
	}

	@Test
	void withAccessControlDeniesAResourceGrantedAtLimitedReadForAFullRead() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.COMMUNICATION, LR));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(matches(specification)).isFalse();
	}

	@Test
	void withAccessControlDeniesWhenResourceIsNotGranted() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.ERRAND, RW));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(matches(specification)).isFalse();
		verify(accessMapperService, never()).getAccessibleLabels(any(), any(), any(), any());
	}

	@Test
	void withAccessControlDeniesWhenGrantedResourceLevelIsTooLow() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.COMMUNICATION, R));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, RW);

		assertThat(matches(specification)).isFalse();
	}

	/**
	 * A denied specification is a bare disjunction, which is what the criteria builder produces for "matches nothing".
	 */
	private static boolean matches(final Specification<ErrandEntity> specification) {
		final var criteriaBuilder = mock(CriteriaBuilder.class);
		final var disjunction = mock(Predicate.class);
		when(criteriaBuilder.disjunction()).thenReturn(disjunction);

		return specification.toPredicate(null, null, criteriaBuilder) != disjunction;
	}

	@Test
	void withAccessControlAddsReporterClauseWhenResourceIsGranted() {
		final var user = adUser();
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(List.of(ResourceAccess.create().withResource(ProtectedResource.ERRAND).withLevel(AccessLevel.R)), null));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels).or(isReportedBy(AD_ACCOUNT)));
	}

	@Test
	void withAccessControlOmitsReporterClauseWhenResourceIsNotGranted() {
		final var user = adUser();
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(List.of(ResourceAccess.create().withResource(ProtectedResource.COMMUNICATION).withLevel(AccessLevel.R)), null));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void withAccessControlOmitsReporterClauseWhenGrantedLevelIsTooLow() {
		final var user = adUser();
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(List.of(ResourceAccess.create().withResource(ProtectedResource.ERRAND).withLevel(AccessLevel.R)), null));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, RW);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void withAccessControlAddsReporterClauseForWriteWhenGranted() {
		final var user = adUser();
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(List.of(ResourceAccess.create().withResource(ProtectedResource.CONVERSATION_MESSAGE).withLevel(AccessLevel.RW)), null));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.CONVERSATION_MESSAGE, RW);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels).or(isReportedBy(AD_ACCOUNT)));
	}

	@Test
	void withAccessControlOff() {
		// Setup
		final var config = NamespaceConfig.create().withAccessControl(false);
		final var user = Identifier.create();

		// Mock
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(config);

		// Act
		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, LR);

		// Verify
		assertThat(specification).usingRecursiveComparison().isEqualTo((Specification<ErrandEntity>) (_, _, criteriaBuilder) -> criteriaBuilder.conjunction());
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		// A namespace that has not opted in must cost no remote calls, so an access mapper outage cannot affect it.
		verifyNoInteractions(accessMapperService);
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
		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, LR);

		// Verify - one clause, at limited read since that is the lowest level reaching an errand.
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
		final var result = accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR);

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
		final var result = accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ERRAND, LR);

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
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR));

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
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR));

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
		accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ERRAND, LR);

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
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ERRAND, LR));

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
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ERRAND, LR));

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

	@Test
	void readableKeyPredicateAllowsEveryKeyWhenAccessControlIsInactive() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(false));

		final var predicate = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), ErrandEntity.create(), ErrandField.PARAMETERS);

		assertThat(predicate.test("any-key")).isTrue();
		verifyNoInteractions(accessMapperService);
	}

	@Test
	void readableKeyPredicateAllowsEveryKeyOfAnErrandNothingRestricts() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));

		final var predicate = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), ErrandEntity.create(), ErrandField.PARAMETERS);

		assertThat(predicate.test("any-key")).isTrue();
	}

	@Test
	void readableKeyPredicateAllowsEveryKeyWhenNoRoleMatches() {
		final var errand = ErrandEntity.create().withReporterUserId("someone-else");
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")))));

		final var predicate = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);

		assertThat(predicate.test("key-2")).isTrue();
	}

	@Test
	void readableKeyPredicateLimitsToConfiguredKeys() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")))));

		final var predicate = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);

		assertThat(predicate.test("key-1")).isTrue();
		assertThat(predicate.test("key-2")).isFalse();
	}

	@Test
	void readableKeyPredicateAllowsEveryKeyWhenFieldIsGrantedWithoutKeys() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.PARAMETERS))));

		final var predicate = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);

		assertThat(predicate.test("any-key")).isTrue();
	}

	@Test
	void readableKeyPredicateDeniesEveryKeyWhenFieldIsNotGranted() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.TITLE))));

		final var predicate = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);

		assertThat(predicate.test("any-key")).isFalse();
	}

	@Test
	void verifyAccessibleKeyThrowsForUngrantedKey() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		Identifier.set(adUser());
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")))));

		final var exception = assertThrows(ThrowableProblem.class,
			() -> accessControlService.verifyAccessibleKey(NAMESPACE, MUNICIPALITY_ID, errand, ErrandField.PARAMETERS, "key-2"));

		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Key 'key-2' not accessible by user '%s'".formatted(AD_ACCOUNT));
	}

	@Test
	void verifyAccessibleKeyPassesForGrantedKey() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		Identifier.set(adUser());
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")))));

		assertThatNoException().isThrownBy(() -> accessControlService.verifyAccessibleKey(NAMESPACE, MUNICIPALITY_ID, errand, ErrandField.PARAMETERS, "key-1"));
	}

	@Test
	void withAccessControlReachesTheErrandOnLimitedReadWithoutConfiguration() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.ERRAND, LR);

		// Limited read always reaches the errand, so the clause is resolved at that level.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, adUser(), List.of(LR, R, RW));
	}

	@Test
	void withAccessControlKeepsLimitedReadOffOtherResourcesWithoutConfiguration() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.NOTE, LR);

		// Only the full access clause, so labels giving limited read reach no note.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, adUser(), List.of(R, RW));
	}

	@Test
	void withAccessControlExtendsLimitedReadToAConfiguredResource() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withResources(List.of(ProtectedResource.NOTE))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.NOTE, LR);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, adUser(), List.of(LR, R, RW));
	}

	@Test
	void withAccessControlNeverExtendsLimitedReadToAWrite() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withResources(List.of(ProtectedResource.NOTE))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.NOTE, RW);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(accessMapperService).getAccessibleLabels(MUNICIPALITY_ID, NAMESPACE, adUser(), List.of(RW));
	}

	@Test
	void withAccessControlNeverExtendsLimitedReadToAnErrandWrite() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(allowedLabels);

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.ERRAND, RW);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void roleBasedFieldResolverFallsBackToAMinimumWhenLimitedReadIsNotConfigured() {
		final var errand = ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		// Never the full errand, since the namespace has not said what limited read exposes.
		assertThat(result).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS);
	}

	@Test
	void roleBasedFieldResolverKeepsTheMinimumUnderReporterFields() {
		final var errand = ErrandEntity.create()
			.withReporterUserId(AD_ACCOUNT)
			.withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withReporterAccess(ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE)))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		// The minimum is a floor for every limited read user, reporter or not, and the reporter fields add to it
		assertThat(result).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS);
	}

	@Test
	void roleBasedFieldResolverTrimsALimitedErrandWithoutRoleBasedMapping() {
		// Limited read may never silently mean full read, so the toggle governs role restrictions only.
		final var errand = limitedErrand();
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(false)
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE)))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.TITLE);
		verify(accessMapperService, never()).getAccessibleRoles(any(), any(), any());
	}

	@Test
	void roleBasedFieldResolverIgnoresRoleRestrictionsWithoutRoleBasedMapping() {
		final var errand = ErrandEntity.create();
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(false)
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE))))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
		verify(accessMapperService, never()).getAccessibleRoles(any(), any(), any());
	}

	@Test
	void roleBasedFieldResolverLeavesAnUnrestrictedRoleUnmapped() {
		final var errand = ErrandEntity.create();
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("OTHER_ROLE").withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE))))));
		when(accessMapperService.getAccessibleLabels(any(), any(), any(), any())).thenReturn(Set.of());
		when(accessMapperService.getAccessibleRoles(any(), any(), any())).thenReturn(Set.of("CASE_OFFICER"));

		// No restriction listed for the role held, and the errand is not limited, so the errand is mapped in full.
		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
	}

	@Test
	void verifyNamespaceAuthorizationPassesWhenAccessControlIsInactive() {
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		assertThatNoException().isThrownBy(() -> accessControlService.verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW));
		verifyNoInteractions(accessMapperService);
	}

	@Test
	void verifyNamespaceAuthorizationPassesWhenResourceIsGranted() {
		Identifier.set(adUser());
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.NAMESPACE_CONFIG, RW));

		assertThatNoException().isThrownBy(() -> accessControlService.verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW));
	}

	@Test
	void verifyNamespaceAuthorizationThrowsWhenResourceIsNotGranted() {
		Identifier.set(adUser());
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.METADATA_STATUS, RW));

		final var exception = assertThrows(ThrowableProblem.class,
			() -> accessControlService.verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW));

		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Resource 'NAMESPACE_CONFIG' not accessible by user '%s'".formatted(AD_ACCOUNT));
	}

	@Test
	void verifyNamespaceAuthorizationThrowsWhenGrantedLevelIsTooLow() {
		Identifier.set(adUser());
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessMapperService.getAccessibleResources(any(), any(), any())).thenReturn(Map.of(ProtectedResource.NAMESPACE_CONFIG, R));

		assertThrows(ThrowableProblem.class,
			() -> accessControlService.verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW));
	}

	@Test
	void roleBasedFieldResolverNeverCallsAccessMapperWhileAccessControlIsOff() {
		// A namespace that has not opted in must cost no remote calls at all: an access mapper outage may not turn
		// errand reads into 500s for namespaces that never enabled access control.
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(false));

		accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(ErrandEntity.create());

		verifyNoInteractions(accessMapperService);
	}
}
