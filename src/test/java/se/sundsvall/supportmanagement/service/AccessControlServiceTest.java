package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
import se.sundsvall.supportmanagement.service.model.AccessSnapshot;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

	/**
	 * The access mapper grants nothing unless a test says otherwise, which is what it answers for a user held in no
	 * access group at all.
	 */
	@BeforeEach
	void setUp() {
		lenient().when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(AccessSnapshot.empty());
	}

	private static Identifier adUser() {
		return Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(AD_ACCOUNT);
	}

	/**
	 * An errand carrying a label the user is not given, so their labels never cover it fully and it is limited for them.
	 */
	private static ErrandEntity limitedErrand() {
		return ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
	}

	private static final MetadataLabelEntity LIMITED_READ_LABEL = MetadataLabelEntity.create().withId("limited-read-label-id");
	private static final MetadataLabelEntity READ_LABEL = MetadataLabelEntity.create().withId("read-label-id");
	private static final MetadataLabelEntity WRITE_LABEL = MetadataLabelEntity.create().withId("write-label-id");

	/**
	 * What the access mapper says about a user granted sent in labels at every level.
	 */
	private static AccessSnapshot snapshotOf(final Set<MetadataLabelEntity> labels) {
		return snapshotOf(labels, Set.of(), Map.of());
	}

	private static AccessSnapshot snapshotOf(final Set<MetadataLabelEntity> labels, final Set<String> roles) {
		return snapshotOf(labels, roles, Map.of());
	}

	private static AccessSnapshot snapshotOfResources(final Map<ProtectedResource, Access.AccessLevelEnum> resources) {
		return snapshotOf(Set.of(), Set.of(), resources);
	}

	private static AccessSnapshot snapshotOf(final Set<MetadataLabelEntity> labels, final Set<String> roles, final Map<ProtectedResource, Access.AccessLevelEnum> resources) {
		return new AccessSnapshot(Map.of(LR, labels, R, labels, RW, labels), roles, resources);
	}

	/**
	 * A label granted at one level each, so that the clause built for an operation shows which levels it reaches.
	 */
	private static AccessSnapshot snapshotWithALabelPerLevel() {
		return new AccessSnapshot(Map.of(LR, Set.of(LIMITED_READ_LABEL), R, Set.of(READ_LABEL), RW, Set.of(WRITE_LABEL)), Set.of(), Map.of());
	}

	/**
	 * What the access mapper says about a user granted limited read, and nothing beyond it, for {@link #limitedErrand()}.
	 */
	private static AccessSnapshot limitedReadSnapshot() {
		return new AccessSnapshot(Map.of(LR, Set.of(MetadataLabelEntity.create().withId("label-id-1")), R, Set.of(), RW, Set.of()), Set.of(), Map.of());
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

		// No label of theirs reaches the errand, so they hold it as its reporter and see the reporter fields alone.
		assertThat(result).containsOnlyKeys(ErrandField.TITLE, ErrandField.PARAMETERS);
		assertThat(result.get(ErrandField.TITLE)).isEmpty();
		assertThat(result.get(ErrandField.PARAMETERS)).containsExactly("key-1");
	}

	/**
	 * The reporter of an errand no label of theirs reaches was never granted limited read for it, so limited read has
	 * nothing to add and the reporter fields may be narrower than it. The two are independent grants.
	 */
	@Test
	void roleBasedFieldResolverHoldsAReporterOutsideTheirLabelsToTheReporterFields() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.CHANNEL))))
			.withReporterAccess(ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.DESCRIPTION)))));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.DESCRIPTION);
	}

	/**
	 * A reporter their labels do reach the errand through keeps everything limited read shows them, since reporting an
	 * errand may never show someone less of it than a limited read user who did not report it.
	 */
	@Test
	void roleBasedFieldResolverUnionsReporterOnTopOfLimitedRead() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.CHANNEL))))
			.withReporterAccess(ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.DESCRIPTION)))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(limitedReadSnapshot());

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.CHANNEL, ErrandField.DESCRIPTION);
	}

	/**
	 * A namespace granting the reporter its errands without saying what of them they see falls back to the minimum,
	 * rather than to an errand carrying no fields whatsoever.
	 */
	@Test
	void roleBasedFieldResolverFallsBackToTheMinimumForAReporterWithoutConfiguredFields() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, null));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS);
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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of()));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result).containsOnlyKeys(ErrandField.TITLE);
		verify(accessMapperService).getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, adUser());
	}

	@Test
	void roleBasedFieldResolverResolvesNothingWhenLabelsCoverErrandFully() {
		final var errand = ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE)))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(MetadataLabelEntity.create().withId("label-id-1"))));

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
	}

	@Test
	void roleBasedFieldResolverResolvesFieldsForNamespaceRoleFromAccessMapper() {
		final var errand = ErrandEntity.create();
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("case_officer").withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE))))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		assertThat(result.get(ErrandField.PARAMETERS)).isEmpty();
	}

	@Test
	void withAccessControlIgnoresAccessMapperResourcesWhenGateIsOff() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void withAccessControlKeepsLabelClauseWhenResourceIsGranted() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels, Set.of(), Map.of(ProtectedResource.COMMUNICATION, RW)));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void withAccessControlAcceptsAResourceGrantedAtLimitedReadForALimitedReadOperation() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(new AccessSnapshot(
			Map.of(LR, Set.of(LIMITED_READ_LABEL), R, Set.of(READ_LABEL), RW, Set.of(WRITE_LABEL)), Set.of(), Map.of(ProtectedResource.ERRAND, LR)));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.ERRAND, LR);

		// An explicitly granted limited read resource may not behave the same as no grant at all.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(Set.of(LIMITED_READ_LABEL, READ_LABEL, WRITE_LABEL)));
	}

	@Test
	void withAccessControlDeniesAResourceGrantedAtLimitedReadForAFullRead() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOfResources(Map.of(ProtectedResource.COMMUNICATION, LR)));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(matches(specification)).isFalse();
	}

	@Test
	void withAccessControlDeniesWhenResourceIsNotGranted() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOfResources(Map.of(ProtectedResource.ERRAND, RW)));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.COMMUNICATION, R);

		assertThat(matches(specification)).isFalse();
	}

	@Test
	void withAccessControlDeniesWhenGrantedResourceLevelIsTooLow() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true).withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOfResources(Map.of(ProtectedResource.COMMUNICATION, R)));

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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels).or(isReportedBy(AD_ACCOUNT)));
	}

	@Test
	void withAccessControlOmitsReporterClauseWhenResourceIsNotGranted() {
		final var user = adUser();
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(List.of(ResourceAccess.create().withResource(ProtectedResource.COMMUNICATION).withLevel(AccessLevel.R)), null));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void withAccessControlOmitsReporterClauseWhenGrantedLevelIsTooLow() {
		final var user = adUser();
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(List.of(ResourceAccess.create().withResource(ProtectedResource.ERRAND).withLevel(AccessLevel.R)), null));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, RW);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void withAccessControlAddsReporterClauseForWriteWhenGranted() {
		final var user = adUser();
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(List.of(ResourceAccess.create().withResource(ProtectedResource.CONVERSATION_MESSAGE).withLevel(AccessLevel.RW)), null));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));

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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));

		// Act
		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, LR);

		// Verify - one clause, at limited read since that is the lowest level reaching an errand.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, user);
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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));

		// Act
		final var result = accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR);

		// Verify
		assertThat(result).isSameAs(entity);
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, user);
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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.of(entity));

		// Act
		final var result = accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.ERRAND, LR);

		// Verify
		assertThat(result).isSameAs(entity);
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, user);
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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.empty());

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, false, ProtectedResource.ERRAND, LR));

		// Verify
		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getTitle()).isEqualTo(UNAUTHORIZED.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Errand not accessible by user 'user'");
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, user);
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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.exists(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(true);

		// Act
		accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ERRAND, LR);

		// Verify
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, user);
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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));
		when(errandsRepositoryMock.existsByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.exists(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(false);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.verifyExistingErrandAndAuthorization(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, ProtectedResource.ERRAND, LR));

		// Verify
		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getTitle()).isEqualTo(UNAUTHORIZED.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Errand not accessible by user 'user'");
		verify(namespaceConfigServiceMock).get(NAMESPACE, MUNICIPALITY_ID);
		verify(accessMapperService).getAccessSnapshot(MUNICIPALITY_ID, NAMESPACE, user);
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
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotWithALabelPerLevel());

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.ERRAND, LR);

		// Limited read always reaches the errand, so the clause carries the labels granted at that level too.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(Set.of(LIMITED_READ_LABEL, READ_LABEL, WRITE_LABEL)));
	}

	@Test
	void withAccessControlKeepsLimitedReadOffOtherResourcesWithoutConfiguration() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotWithALabelPerLevel());

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.NOTE, LR);

		// Only the full access clause, so labels giving limited read reach no note.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(Set.of(READ_LABEL, WRITE_LABEL)));
	}

	@Test
	void withAccessControlExtendsLimitedReadToAConfiguredResource() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withResources(List.of(ProtectedResource.NOTE))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotWithALabelPerLevel());

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.NOTE, LR);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(Set.of(LIMITED_READ_LABEL, READ_LABEL, WRITE_LABEL)));
	}

	@Test
	void withAccessControlNeverExtendsLimitedReadToAWrite() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withLimitedReadAccess(LimitedReadAccess.create().withResources(List.of(ProtectedResource.NOTE))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotWithALabelPerLevel());

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.NOTE, RW);

		// A write is satisfied by read/write labels alone, so neither the read nor the limited read ones are in the clause.
		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(Set.of(WRITE_LABEL)));
	}

	@Test
	void withAccessControlNeverExtendsLimitedReadToAnErrandWrite() {
		final var allowedLabels = Set.of(MetadataLabelEntity.create().withId("label-id"));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(allowedLabels));

		final var specification = accessControlService.withAccessControl(NAMESPACE, MUNICIPALITY_ID, adUser(), ProtectedResource.ERRAND, RW);

		assertThat(specification).usingRecursiveComparison().isEqualTo(hasAllowedMetadataLabels(allowedLabels));
	}

	@Test
	void roleBasedFieldResolverFallsBackToAMinimumWhenLimitedReadIsNotConfigured() {
		final var errand = ErrandEntity.create().withAccessLabels(List.of(AccessLabelEmbeddable.create().withMetadataLabelId("label-id-1")));
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of()));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		// Never the full errand, since the namespace has not said what limited read exposes.
		assertThat(result).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS);
	}

	@Test
	void roleBasedFieldResolverKeepsTheMinimumUnderReporterFields() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withReporterAccess(ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE)))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(limitedReadSnapshot());

		// The namespace says nothing about limited read, and its minimum is a floor for every limited read user,
		// reporter or not, which the reporter fields add to.
		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

		final var result = accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand);

		// The role held is left out of it, since the namespace does not map errands per role.
		assertThat(result).containsOnlyKeys(ErrandField.TITLE);
	}

	@Test
	void roleBasedFieldResolverIgnoresRoleRestrictionsWithoutRoleBasedMapping() {
		final var errand = ErrandEntity.create();
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(false)
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE))))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

		assertThat(accessControlService.roleBasedFieldResolver(NAMESPACE, MUNICIPALITY_ID, adUser()).apply(errand)).isNull();
	}

	@Test
	void roleBasedFieldResolverLeavesAnUnrestrictedRoleUnmapped() {
		final var errand = ErrandEntity.create();
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("OTHER_ROLE").withFields(List.of(FieldAccess.create().withField(ErrandField.TITLE))))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

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
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOfResources(Map.of(ProtectedResource.NAMESPACE_CONFIG, RW)));

		assertThatNoException().isThrownBy(() -> accessControlService.verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW));
	}

	@Test
	void verifyNamespaceAuthorizationThrowsWhenResourceIsNotGranted() {
		Identifier.set(adUser());
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOfResources(Map.of(ProtectedResource.METADATA_STATUS, RW)));

		final var exception = assertThrows(ThrowableProblem.class,
			() -> accessControlService.verifyNamespaceAuthorization(NAMESPACE, MUNICIPALITY_ID, ProtectedResource.NAMESPACE_CONFIG, RW));

		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
		assertThat(exception.getMessage()).isEqualTo("Unauthorized: Resource 'NAMESPACE_CONFIG' not accessible by user '%s'".formatted(AD_ACCOUNT));
	}

	@Test
	void verifyNamespaceAuthorizationThrowsWhenGrantedLevelIsTooLow() {
		Identifier.set(adUser());
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOfResources(Map.of(ProtectedResource.NAMESPACE_CONFIG, R)));

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

	@Test
	void measureCreatorMustBeTheRequestingUserWithTheClaimedRole() {
		final var user = adUser();
		Identifier.set(user);
		try {
			when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
			when(accessMapperService.getAccessibleRoles(MUNICIPALITY_ID, NAMESPACE, user)).thenReturn(Set.of("MANAGER"));
			assertThatNoException().isThrownBy(() -> accessControlService.verifyMeasureCreator(NAMESPACE, MUNICIPALITY_ID, AD_ACCOUNT, "manager"));
			assertThat(assertThrows(ThrowableProblem.class, () -> accessControlService.verifyMeasureCreator(NAMESPACE, MUNICIPALITY_ID, "someone-else", "MANAGER")).getStatus()).isEqualTo(UNAUTHORIZED);
			assertThat(assertThrows(ThrowableProblem.class, () -> accessControlService.verifyMeasureCreator(NAMESPACE, MUNICIPALITY_ID, AD_ACCOUNT, "OTHER")).getStatus()).isEqualTo(UNAUTHORIZED);
		} finally {
			Identifier.remove();
		}
	}

	@Test
	void measureCreatorRequiresAnAdAccountWhenAccessControlIsActive() {
		Identifier.remove();
		when(namespaceConfigServiceMock.isAccessControlActive(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		assertThat(assertThrows(ThrowableProblem.class, () -> accessControlService.verifyMeasureCreator(NAMESPACE, MUNICIPALITY_ID, AD_ACCOUNT, "MANAGER")).getStatus()).isEqualTo(UNAUTHORIZED);
		verifyNoInteractions(accessMapperService);
	}
}
