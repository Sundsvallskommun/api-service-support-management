package se.sundsvall.supportmanagement.service;

import generated.se.sundsvall.accessmapper.Access;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.Arrays;
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
import static org.assertj.core.api.Assertions.entry;
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
	void writableKeyPredicateHoldsAKeyGrantedToRead() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(
			FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("read-only")).withLevel(AccessLevel.R),
			FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("writable")))));

		final var readable = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);
		final var writable = accessControlService.writableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);

		// A level only ever narrows: the key is served, it simply may not be changed.
		assertThat(readable.test("read-only")).isTrue();
		assertThat(writable.test("read-only")).isFalse();

		// A grant carrying no level follows the errand, which is what every grant did before levels existed.
		assertThat(readable.test("writable")).isTrue();
		assertThat(writable.test("writable")).isTrue();
	}

	@Test
	void writableKeyPredicateAcceptsAKeyGrantedToReadAndWrite() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(
			FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")).withLevel(AccessLevel.RW))));

		assertThat(accessControlService.writableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS).test("key-1")).isTrue();
	}

	@Test
	void writableKeyPredicateTakesTheMostPermissiveOfTwoScopes() {
		final var errand = ErrandEntity.create().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create()
			.withAccessControl(true)
			.withRoleBasedMapping(true)
			.withReporterAccess(ReporterAccess.create().withFields(List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")).withLevel(AccessLevel.RW))))
			.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(List.of(
				FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("key-1")).withLevel(AccessLevel.R))))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(), Set.of("CASE_OFFICER")));

		// One scope holding the key to read does not take the write away from a scope granting it.
		assertThat(accessControlService.writableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS).test("key-1")).isTrue();
	}

	@Test
	void writableKeyPredicateHoldsAWholeCollectionGrantedToRead() {
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(null, List.of(
			FieldAccess.create().withField(ErrandField.PARAMETERS).withLevel(AccessLevel.R))));

		final var readable = accessControlService.readableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);
		final var writable = accessControlService.writableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), errand, ErrandField.PARAMETERS);

		// The whole collection is served and none of it may be changed.
		assertThat(readable.test("any-key")).isTrue();
		assertThat(writable.test("any-key")).isFalse();
	}

	@Test
	void writableKeyPredicateAllowsEveryKeyOfAnErrandNothingRestricts() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(true));

		assertThat(accessControlService.writableKeyPredicate(NAMESPACE, MUNICIPALITY_ID, adUser(), ErrandEntity.create(), ErrandField.PARAMETERS).test("any-key")).isTrue();
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

	// -------------------------------------------------------------------------------------------------------------
	// resolveErrandAccess
	// -------------------------------------------------------------------------------------------------------------

	private static final MetadataLabelEntity ERRAND_LABEL = MetadataLabelEntity.create().withId("label-id-1");

	/**
	 * An errand covered by {@link #ERRAND_LABEL}, so a user granted that label holds it fully.
	 */
	private static ErrandEntity coveredErrand() {
		return limitedErrand();
	}

	private static NamespaceConfig controlledConfig() {
		return NamespaceConfig.create().withAccessControl(true).withRoleBasedMapping(true);
	}

	@Test
	void resolveErrandAccessWithoutAccessControlGrantsEverything() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(false));

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), coveredErrand());

		assertThat(resolution.errandLevel()).isEqualTo(RW);
		assertThat(resolution.fields()).containsOnlyKeys(ErrandField.values());
		assertThat(resolution.resources()).containsOnlyKeys(Arrays.stream(ProtectedResource.values())
			.filter(ProtectedResource::isErrandScoped)
			.filter(resource -> ProtectedResource.ERRAND != resource)
			.toArray(ProtectedResource[]::new));
		assertThat(resolution.resources().values()).containsOnly(RW);
		verifyNoInteractions(accessMapperService);
	}

	@Test
	void resolveErrandAccessWithoutAccessControlMarksKeyedFieldsAsHoldingEveryKey() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(NamespaceConfig.create().withAccessControl(false));

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), coveredErrand());

		assertThat(resolution.fields().get(ErrandField.PARAMETERS).allKeys()).isTrue();
		assertThat(resolution.fields().get(ErrandField.PARAMETERS).keys()).isEmpty();
		assertThat(resolution.fields().get(ErrandField.TITLE).allKeys()).isNull();
		assertThat(resolution.fields().get(ErrandField.TITLE).keys()).isNull();
	}

	@Test
	void resolveErrandAccessGrantsEverythingToUnrestrictedUserOfControlledNamespace() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(controlledConfig());
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(ERRAND_LABEL)));

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), coveredErrand());

		assertThat(resolution.errandLevel()).isEqualTo(RW);
		assertThat(resolution.fields()).containsOnlyKeys(ErrandField.values());
	}

	/**
	 * A field carries no level of its own, so what may be written is read off the errand. Keys are held against it, since
	 * a key of an errand reached at read is never writable.
	 */
	@Test
	void resolveErrandAccessHoldsKeysAgainstTheLevelOfTheErrand() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithRoleFields(List.of(
			FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("granted-key")))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(
			new AccessSnapshot(Map.of(LR, Set.of(ERRAND_LABEL), R, Set.of(ERRAND_LABEL), RW, Set.of()), Set.of("CASE_OFFICER"), Map.of()));

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), coveredErrand());

		assertThat(resolution.errandLevel()).isEqualTo(R);
		assertThat(resolution.fields().get(ErrandField.PARAMETERS).keys()).containsExactly(entry("granted-key", R));
	}

	@Test
	void resolveErrandAccessReportsLimitedReadAsTrimmedAndReadOnly() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(controlledConfig()
			.withLimitedReadAccess(LimitedReadAccess.create()
				.withResources(List.of(ProtectedResource.COMMUNICATION))
				.withFields(List.of(
					FieldAccess.create().withField(ErrandField.ID),
					FieldAccess.create().withField(ErrandField.TITLE)))));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(limitedReadSnapshot());

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), limitedErrand());

		assertThat(resolution.errandLevel()).isEqualTo(LR);
		assertThat(resolution.fields()).containsOnlyKeys(ErrandField.ID, ErrandField.TITLE);
		assertThat(resolution.resources()).containsExactly(entry(ProtectedResource.COMMUNICATION, LR));
	}

	@Test
	void resolveErrandAccessReportsWhatTheNamespaceGrantsAReporter() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(
			List.of(
				ResourceAccess.create().withResource(ProtectedResource.ERRAND).withLevel(AccessLevel.R),
				ResourceAccess.create().withResource(ProtectedResource.COMMUNICATION).withLevel(AccessLevel.RW)),
			List.of(
				FieldAccess.create().withField(ErrandField.ID),
				FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("granted-key")))));

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), limitedErrand().withReporterUserId(AD_ACCOUNT));

		assertThat(resolution.errandLevel()).isEqualTo(R);
		assertThat(resolution.fields()).containsOnlyKeys(ErrandField.ID, ErrandField.PARAMETERS);
		assertThat(resolution.fields().get(ErrandField.PARAMETERS).allKeys()).isFalse();
		assertThat(resolution.fields().get(ErrandField.PARAMETERS).keys()).containsExactly(entry("granted-key", R));
		assertThat(resolution.resources()).containsExactly(entry(ProtectedResource.COMMUNICATION, RW));
	}

	/**
	 * The reporter clause reaches no errand for a caller carrying no ad account, exactly as the specification does not.
	 */
	@Test
	void resolveErrandAccessRefusesAReporterIdentifiedByPartyId() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithReporterAccess(
			List.of(ResourceAccess.create().withResource(ProtectedResource.ERRAND).withLevel(AccessLevel.R)),
			List.of(FieldAccess.create().withField(ErrandField.ID))));
		final var partyUser = Identifier.create().withType(Identifier.Type.PARTY_ID).withValue(AD_ACCOUNT);
		final var errand = limitedErrand().withReporterUserId(AD_ACCOUNT);

		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, partyUser, errand));

		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
	}

	/**
	 * hasAllowedMetadataLabels reaches no errand at all for a user holding no labels, where covers would call an
	 * unlabelled errand covered by the empty set. Reporting the looser of the two would promise access the endpoints
	 * refuse.
	 */
	@Test
	void resolveErrandAccessRefusesAUserHoldingNoLabelsOnAnUnlabelledErrand() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(controlledConfig());
		final var user = adUser();
		final var errand = ErrandEntity.create();

		final var exception = assertThrows(ThrowableProblem.class, () -> accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, user, errand));

		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
	}

	@Test
	void resolveErrandAccessOmitsResourcesTheAccessMapperDoesNotGrant() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(controlledConfig().withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(ERRAND_LABEL), Set.of(), Map.of(
			ProtectedResource.ERRAND, RW,
			ProtectedResource.COMMUNICATION, R)));

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), coveredErrand());

		assertThat(resolution.errandLevel()).isEqualTo(RW);
		assertThat(resolution.resources()).containsExactly(entry(ProtectedResource.COMMUNICATION, R));
	}

	// The four shapes a keyed field can resolve to, each reachable from a real configuration.

	private static NamespaceConfig configWithRoleFields(final List<FieldAccess> fields) {
		return controlledConfig().withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole("CASE_OFFICER").withFields(fields)));
	}

	private AccessControlService.ErrandAccessResolution resolveWithRoleFields(final List<FieldAccess> fields) {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(configWithRoleFields(fields));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(snapshotOf(Set.of(ERRAND_LABEL), Set.of("CASE_OFFICER")));
		return accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), coveredErrand());
	}

	@Test
	void resolveErrandAccessReportsAKeyedCollectionCarryingNoKeyRestriction() {
		final var grant = resolveWithRoleFields(List.of(FieldAccess.create().withField(ErrandField.PARAMETERS)))
			.fields().get(ErrandField.PARAMETERS);

		assertThat(grant.allKeys()).isTrue();
		assertThat(grant.keys()).isEmpty();
	}

	/**
	 * A key restriction is all or nothing, so a namespace naming keys makes those the only reachable ones and each is
	 * listed with what may be done to it. A key held to read sits next to one that may be written.
	 */
	@Test
	void resolveErrandAccessListsEveryKeyOfAKeyedCollectionGrantedByKey() {
		final var grant = resolveWithRoleFields(List.of(
			FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("granted-key")),
			FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("readonly-key")).withLevel(AccessLevel.R)))
			.fields().get(ErrandField.PARAMETERS);

		assertThat(grant.allKeys()).isFalse();
		assertThat(grant.keys()).containsExactly(entry("granted-key", RW), entry("readonly-key", R));
	}

	/**
	 * A key the errand does not carry yet is listed exactly as a stored one is, since the grant says what may be written
	 * and not what happens to be there. This is what lets a form be rendered editable before anything is saved to it.
	 */
	@Test
	void resolveErrandAccessListsAGrantedKeyTheErrandDoesNotCarry() {
		final var grant = resolveWithRoleFields(List.of(
			FieldAccess.create().withField(ErrandField.JSON_PARAMETERS).withKeys(List.of("never-stored"))))
			.fields().get(ErrandField.JSON_PARAMETERS);

		assertThat(grant.allKeys()).isFalse();
		assertThat(grant.keys()).containsExactly(entry("never-stored", RW));
	}

	/**
	 * The one grant this response cannot express: a namespace may hand out a whole keyed collection and still hold it to
	 * read. There are no keys to carry the restriction and a field carries no level of its own, so such a caller is
	 * reported as reaching every key at the level of the errand - which overstates what the write paths accept. No
	 * namespace configures this, and expressing it would mean giving every field a level back.
	 */
	@Test
	void resolveErrandAccessReportsAWholeKeyedCollectionHeldToReadAsUnrestricted() {
		final var grant = resolveWithRoleFields(List.of(FieldAccess.create().withField(ErrandField.PARAMETERS).withLevel(AccessLevel.R)))
			.fields().get(ErrandField.PARAMETERS);

		assertThat(grant.allKeys()).isTrue();
		assertThat(grant.keys()).isEmpty();
	}

	/**
	 * A resource grant never stands in for the labels. Holding an errand at read while the access mapper grants the
	 * parameter resource read/write does not open the endpoint writing a parameter of that errand: the specification
	 * asks the labels for read/write as well, and both have to allow.
	 */
	@Test
	void getErrandRefusesAWriteResourceGrantOnAnErrandTheLabelsOnlyReach() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(controlledConfig().withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(new AccessSnapshot(
			Map.of(LR, Set.of(ERRAND_LABEL), R, Set.of(ERRAND_LABEL), RW, Set.of()),
			Set.of(),
			Map.of(ProtectedResource.ERRAND, RW, ProtectedResource.PARAMETER, RW)));
		when(errandsRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(any(), any(), any())).thenReturn(true);
		when(errandsRepositoryMock.findOne(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(Optional.empty());

		final var exception = assertThrows(ThrowableProblem.class,
			() -> accessControlService.getErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, true, ProtectedResource.PARAMETER, RW));

		assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
	}

	/**
	 * And the report says as much rather than passing the resource grant on unqualified, so a client is never invited to
	 * call an endpoint the labels would refuse.
	 */
	@Test
	void resolveErrandAccessHoldsAResourceGrantAgainstTheLabels() {
		when(namespaceConfigServiceMock.get(any(), any())).thenReturn(controlledConfig().withResourceAccessControl(true));
		when(accessMapperService.getAccessSnapshot(any(), any(), any())).thenReturn(new AccessSnapshot(
			Map.of(LR, Set.of(ERRAND_LABEL), R, Set.of(ERRAND_LABEL), RW, Set.of()),
			Set.of(),
			Map.of(ProtectedResource.ERRAND, RW, ProtectedResource.PARAMETER, RW)));

		final var resolution = accessControlService.resolveErrandAccess(NAMESPACE, MUNICIPALITY_ID, adUser(), coveredErrand());

		assertThat(resolution.errandLevel()).isEqualTo(R);
		assertThat(resolution.resources()).containsExactly(entry(ProtectedResource.PARAMETER, R));
	}
}
