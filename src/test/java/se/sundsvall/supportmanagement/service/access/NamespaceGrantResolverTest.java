package se.sundsvall.supportmanagement.service.access;

import generated.se.sundsvall.accessmapper.Access;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.api.model.config.AccessLevel;
import se.sundsvall.supportmanagement.api.model.config.FieldAccess;
import se.sundsvall.supportmanagement.api.model.config.LimitedReadAccess;
import se.sundsvall.supportmanagement.api.model.config.NamespaceConfig;
import se.sundsvall.supportmanagement.api.model.config.ReporterAccess;
import se.sundsvall.supportmanagement.api.model.config.ResourceAccess;
import se.sundsvall.supportmanagement.api.model.config.RoleFieldRestriction;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a user holds, decided from a configuration and a snapshot and nothing else.
 * <p>
 * Every rendering of access control reads this: the specification guarding the database, the check made in memory, the
 * trimming of an errand and the search of the index. Asked here directly rather than through one of them, so that a
 * rule that changes says so once, in the language of the rule.
 */
class NamespaceGrantResolverTest {

	private static final String AD_ACCOUNT = "joe01doe";
	private static final MetadataLabelEntity READ_LABEL = MetadataLabelEntity.create().withId("read-label");
	private static final MetadataLabelEntity LIMITED_LABEL = MetadataLabelEntity.create().withId("limited-label");
	private static final String ROLE = "FIRST_LINE";

	// ==================================================================================
	// Whether the namespace enforces anything at all
	// ==================================================================================

	@Test
	void aNamespaceNotEnforcingAccessControlGrantsEverything() {
		final var grant = NamespaceGrantResolver.namespaceGrant(NamespaceConfig.create(), snapshot(Set.of(READ_LABEL)), adUser(), R);

		assertThat(grant).isEqualTo(NamespaceGrant.UNRESTRICTED);
		assertThat(grant.enforced()).isFalse();
		assertThat(grant.scope().enforced()).isFalse();
		assertThat(grant.reaches(ProtectedResource.COMMUNICATION)).isTrue();
	}

	// ==================================================================================
	// The label route
	// ==================================================================================

	@Test
	void theLabelsOfTheUserAreTheReadRoute() {
		final var grant = NamespaceGrantResolver.namespaceGrant(enforcing(), snapshot(Set.of(READ_LABEL)), adUser(), R);

		assertThat(grant.enforced()).isTrue();
		assertThat(grant.labels().labels()).containsExactly(READ_LABEL);
		assertThat(grant.labels().reachesAnything()).isTrue();
		// Nothing restricts the fields while the namespace maps no errands per role
		assertThat(grant.labels().readable()).isNull();
	}

	@Test
	void aUserTheAccessMapperGrantsNoLabelReachesNothingThroughThem() {
		final var grant = NamespaceGrantResolver.namespaceGrant(enforcing(), snapshot(Set.of()), adUser(), R);

		assertThat(grant.labels().labels()).isEmpty();
		assertThat(grant.labels().reachesAnything()).isFalse();
	}

	// ==================================================================================
	// The limited read route
	// ==================================================================================

	/**
	 * An errand the labels cover at the level is held at the level, whatever a limited read would expose of it, so a
	 * route reaching nothing new is not built at all.
	 */
	@Test
	void noLimitedRouteWhereLimitedReadReachesNoMoreThanReadDoes() {
		final var grant = NamespaceGrantResolver.namespaceGrant(enforcing(), snapshot(Set.of(READ_LABEL)), adUser(), R);

		assertThat(grant.limitedLabels()).isNull();
	}

	@Test
	void labelsReachingMoreAtLimitedReadAreARouteOfTheirOwn() {
		final var grant = NamespaceGrantResolver.namespaceGrant(enforcing(), snapshotPerLevel(), adUser(), R);

		assertThat(grant.limitedLabels().labels()).containsExactlyInAnyOrder(READ_LABEL, LIMITED_LABEL);
		// Nothing configured, so a limited read shows the minimum rather than everything
		assertThat(grant.limitedLabels().readable()).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS);
	}

	@Test
	void theLimitedRouteShowsWhatTheNamespaceExposesForALimitedRead() {
		final var config = enforcing().withLimitedReadAccess(LimitedReadAccess.create()
			.withFields(List.of(FieldAccess.create().withField(ErrandField.CHANNEL))));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshotPerLevel(), adUser(), R);

		assertThat(grant.limitedLabels().readable()).containsOnlyKeys(ErrandField.CHANNEL);
	}

	/**
	 * Nothing is written on the strength of a limited read, so an operation asking for a write is answered by the labels
	 * carrying it and by nothing else.
	 */
	@Test
	void noLimitedRouteForAWrite() {
		final var grant = NamespaceGrantResolver.namespaceGrant(enforcing(), snapshotPerLevel(), adUser(), RW);

		assertThat(grant.limitedLabels()).isNull();
	}

	// ==================================================================================
	// The reporter route
	// ==================================================================================

	@Test
	void noReporterRouteWhereTheNamespaceGrantsReportersNothing() {
		final var grant = NamespaceGrantResolver.namespaceGrant(enforcing(), snapshot(Set.of(READ_LABEL)), adUser(), R);

		assertThat(grant.reporter()).isNull();
		assertThat(grant.reporterScope().reporterAdAccount()).isNull();
	}

	@Test
	void theReporterRouteCarriesTheAdAccountAndWhatTheNamespaceShowsAReporter() {
		final var config = reporterGranting(ProtectedResource.ERRAND).withReporterAccess(ReporterAccess.create()
			.withResources(List.of(resource(ProtectedResource.ERRAND), resource(ProtectedResource.COMMUNICATION)))
			.withFields(List.of(FieldAccess.create().withField(ErrandField.DESCRIPTION))));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of()), adUser(), R);

		assertThat(grant.reporter().adAccount()).isEqualTo(AD_ACCOUNT);
		assertThat(grant.reporter().readable()).containsOnlyKeys(ErrandField.DESCRIPTION);
		assertThat(grant.reporter().resources()).containsExactly(ProtectedResource.COMMUNICATION);
	}

	/**
	 * A namespace saying its reporters reach an errand without saying what of it falls back to the minimum, rather than
	 * to an errand carrying no fields at all.
	 */
	@Test
	void aReporterWithoutConfiguredFieldsSeesTheMinimum() {
		final var grant = NamespaceGrantResolver.namespaceGrant(reporterGranting(ProtectedResource.ERRAND), snapshot(Set.of()), adUser(), R);

		assertThat(grant.reporter().readable()).containsOnlyKeys(ErrandField.ID, ErrandField.ERRAND_NUMBER, ErrandField.TITLE, ErrandField.STATUS);
	}

	/**
	 * Labels are resolved for ad accounts alone, and reporterUserId holds one, so no other kind of caller can be the
	 * reporter of an errand.
	 */
	@Test
	void aCallerThatIsNoAdAccountIsNobodysReporter() {
		final var user = Identifier.create().withType(Identifier.Type.PARTY_ID).withValue("81471222-5798-11e9-ae24-57fa13b361e1");

		final var grant = NamespaceGrantResolver.namespaceGrant(reporterGranting(ProtectedResource.ERRAND), snapshot(Set.of()), user, R);

		assertThat(grant.reporter()).isNull();
	}

	// ==================================================================================
	// What the roles of the user leave readable
	// ==================================================================================

	@Test
	void aRoleOfTheUserSelectsWhatMayBeReadOnTheReadRoute() {
		final var config = roleBased(restriction(ROLE, FieldAccess.create().withField(ErrandField.TITLE)));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of(READ_LABEL), Set.of(ROLE)), adUser(), R);

		assertThat(grant.labels().readable()).containsOnlyKeys(ErrandField.TITLE);
	}

	@Test
	void aRestrictionForARoleTheUserDoesNotHoldRestrictsNothing() {
		final var config = roleBased(restriction("SOMEONE_ELSE", FieldAccess.create().withField(ErrandField.TITLE)));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of(READ_LABEL), Set.of(ROLE)), adUser(), R);

		assertThat(grant.labels().readable()).isNull();
	}

	@Test
	void rolesAreNotConsultedWhereTheNamespaceMapsNoErrandsPerRole() {
		final var config = roleBased(restriction(ROLE, FieldAccess.create().withField(ErrandField.TITLE))).withRoleBasedMapping(false);

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of(READ_LABEL), Set.of(ROLE)), adUser(), R);

		assertThat(grant.labels().readable()).isNull();
	}

	/**
	 * Several roles are the union of what each of them allows: holding one role may never show a user less than they
	 * would see without it.
	 */
	@Test
	void severalRolesAreMerged() {
		final var config = roleBased(
			restriction(ROLE, FieldAccess.create().withField(ErrandField.TITLE)),
			restriction("SECOND_LINE", FieldAccess.create().withField(ErrandField.DESCRIPTION)));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of(READ_LABEL), Set.of(ROLE, "SECOND_LINE")), adUser(), R);

		assertThat(grant.labels().readable()).containsOnlyKeys(ErrandField.TITLE, ErrandField.DESCRIPTION);
	}

	@Test
	void theKeysOfAFieldGrantedBySeveralRolesAreMerged() {
		final var config = roleBased(
			restriction(ROLE, FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("first"))),
			restriction("SECOND_LINE", FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("second"))));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of(READ_LABEL), Set.of(ROLE, "SECOND_LINE")), adUser(), R);

		assertThat(grant.labels().readable().get(ErrandField.PARAMETERS)).containsExactlyInAnyOrder("first", "second");
	}

	/**
	 * A grant carrying no keys at all is the whole collection, so it wins over one naming keys one by one.
	 */
	@Test
	void aRoleGrantedAWholeCollectionWinsOverOneGrantedItsKeys() {
		final var config = roleBased(
			restriction(ROLE, FieldAccess.create().withField(ErrandField.PARAMETERS)),
			restriction("SECOND_LINE", FieldAccess.create().withField(ErrandField.PARAMETERS).withKeys(List.of("second"))));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of(READ_LABEL), Set.of(ROLE, "SECOND_LINE")), adUser(), R);

		assertThat(grant.labels().readable().get(ErrandField.PARAMETERS)).isEmpty();
	}

	/**
	 * A restriction naming no field the service knows leaves the user reading nothing of the errand, which is what an
	 * empty map says, and is told apart from the null that restricts nothing at all.
	 */
	@Test
	void aRestrictionNamingNoFieldLeavesNothingReadable() {
		final var config = roleBased(RoleFieldRestriction.create().withRole(ROLE).withFields(List.of()));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot(Set.of(READ_LABEL), Set.of(ROLE)), adUser(), R);

		assertThat(grant.labels().readable()).isNotNull().isEmpty();
	}

	// ==================================================================================
	// The resources a route reaches
	// ==================================================================================

	/**
	 * A namespace that has not switched resource access control on applies no grants, so every resource of an errand is
	 * reached wherever the errand is.
	 */
	@Test
	void everyResourceIsReachedWhileResourceAccessControlIsOff() {
		final var grant = NamespaceGrantResolver.namespaceGrant(enforcing(), snapshot(Set.of(READ_LABEL)), adUser(), R);

		assertThat(grant.labels().resources()).contains(ProtectedResource.COMMUNICATION, ProtectedResource.DECISION);
		assertThat(grant.labels().resources()).doesNotContain(ProtectedResource.ERRAND);
		assertThat(grant.reaches(ProtectedResource.COMMUNICATION)).isTrue();
	}

	@Test
	void withResourceAccessControlOnARouteReachesOnlyTheResourcesGranted() {
		final var config = enforcing().withResourceAccessControl(true);
		final var snapshot = new AccessSnapshot(Map.of(R, Set.of(READ_LABEL)), Set.of(),
			Map.of(ProtectedResource.ERRAND, R, ProtectedResource.COMMUNICATION, R));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot, adUser(), R);

		assertThat(grant.labels().resources()).containsExactly(ProtectedResource.COMMUNICATION);
		assertThat(grant.reaches(ProtectedResource.DECISION)).isFalse();
	}

	/**
	 * The state a search runs into when a role leaves nothing readable and no resource is reached: every field of the
	 * errand is closed, which the search answers with nothing rather than an error.
	 */
	@Test
	void aRestrictedRoleReachingNoResourceHoldsNothingAtAll() {
		final var config = roleBased(RoleFieldRestriction.create().withRole(ROLE).withFields(List.of()))
			.withResourceAccessControl(true);
		final var snapshot = new AccessSnapshot(Map.of(R, Set.of(READ_LABEL)), Set.of(ROLE), Map.of(ProtectedResource.ERRAND, R));

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshot, adUser(), R);

		assertThat(grant.labels().readable()).isEmpty();
		assertThat(grant.labels().resources()).isEmpty();
	}

	// ==================================================================================
	// The projection the renderings filter on
	// ==================================================================================

	@Test
	void theScopeOfTheGrantReachesTheErrandsOfEveryRoute() {
		final var config = reporterGranting(ProtectedResource.ERRAND);

		final var grant = NamespaceGrantResolver.namespaceGrant(config, snapshotPerLevel(), adUser(), R);

		// The limited labels are a superset of those at read, so they are what the whole grant reaches
		assertThat(grant.scope().allowedLabels()).containsExactlyInAnyOrder(READ_LABEL, LIMITED_LABEL);
		assertThat(grant.scope().reporterAdAccount()).isEqualTo(AD_ACCOUNT);
		assertThat(NamespaceGrant.scopeOf(grant.labels()).allowedLabels()).containsExactly(READ_LABEL);
		assertThat(NamespaceGrant.scopeOf(grant.labels()).reporterAdAccount()).isNull();
	}

	// ==================================================================================

	private static Identifier adUser() {
		return Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(AD_ACCOUNT);
	}

	private static NamespaceConfig enforcing() {
		return NamespaceConfig.create().withAccessControl(true);
	}

	private static NamespaceConfig roleBased(final RoleFieldRestriction... restrictions) {
		return enforcing().withRoleBasedMapping(true).withRoleFieldRestrictions(List.of(restrictions));
	}

	private static NamespaceConfig reporterGranting(final ProtectedResource resource) {
		return enforcing().withReporterAccess(ReporterAccess.create().withResources(List.of(resource(resource))));
	}

	private static RoleFieldRestriction restriction(final String role, final FieldAccess... fields) {
		return RoleFieldRestriction.create().withRole(role).withFields(List.of(fields));
	}

	private static ResourceAccess resource(final ProtectedResource resource) {
		return ResourceAccess.create().withResource(resource).withLevel(AccessLevel.R);
	}

	private static AccessSnapshot snapshot(final Set<MetadataLabelEntity> labels) {
		return snapshot(labels, Set.of());
	}

	private static AccessSnapshot snapshot(final Set<MetadataLabelEntity> labels, final Set<String> roles) {
		return new AccessSnapshot(Map.of(LR, labels, R, labels, RW, labels), roles, Map.of());
	}

	/**
	 * Labels granted one level each, which is what tells the read route from the limited one: the label at limited read
	 * reaches errands the one at read does not.
	 */
	private static AccessSnapshot snapshotPerLevel() {
		return new AccessSnapshot(Map.of(LR, Set.of(LIMITED_LABEL), R, Set.of(READ_LABEL), RW, Set.<MetadataLabelEntity>of()), Set.of(), Map.<ProtectedResource, Access.AccessLevelEnum>of());
	}
}
