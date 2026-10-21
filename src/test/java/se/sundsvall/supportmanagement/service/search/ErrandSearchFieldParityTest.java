package se.sundsvall.supportmanagement.service.search;

import generated.se.sundsvall.accessmapper.Access;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
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
import se.sundsvall.supportmanagement.service.access.AccessSnapshot;
import se.sundsvall.supportmanagement.service.access.ErrandAccessSpecifications;
import se.sundsvall.supportmanagement.service.access.NamespaceGrantResolver;
import se.sundsvall.supportmanagement.service.search.index.ErrandIndexModel;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.LR;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.RW;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.data.jpa.domain.Specification.where;
import static se.sundsvall.supportmanagement.service.util.SpecificationBuilder.withId;

/**
 * Holds what a search may look in against what the answer will show.
 * <p>
 * The same decision is made twice over. The mapper settles per errand what the user may read of it
 * ({@link NamespaceGrantResolver#fieldAccessResolver}), and the search settles per route which errands are searched by
 * which fields ({@link ErrandSearchAccess}). They have to agree in one direction: a route may never search a field the
 * mapper would leave out of the answer for an errand that route reaches. A hit is an answer of its own - it says the
 * field holds what was searched for - so a field searched but not shown is the field disclosed.
 * <p>
 * Nothing compared the two before, and twice they had drifted: the reporter route reached errands the labels already
 * covered, so a query on a field only the reporter fallback exposes was answered from an errand whose answer redacted
 * it. Asserting either side on its own would have caught neither, which is why every combination is put to both.
 * <p>
 * Which errands a route reaches is answered by the specification the database is guarded with, rather than by a second
 * reading of the rule here: {@link AccessControlSpecificationParityTest} already holds that specification to the
 * in memory check, and {@link ErrandSearchPredicates} renders the same {@code AccessScope} for the index.
 */
@SpringBootTest
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql"
})
@Transactional
class ErrandSearchFieldParityTest {

	private static final String NAMESPACE = "NAMESPACE-1";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String AD_ACCOUNT = "joe01doe";
	private static final String ROLE = "CASE_OFFICER";

	private static final MetadataLabelEntity LABEL = MetadataLabelEntity.create().withId("label-id-1");
	private static final MetadataLabelEntity OTHER_LABEL = MetadataLabelEntity.create().withId("label-id-2");

	/** A cut of the text fields of the real index, enough for the routes to differ in what they open. */
	private static final List<String> TEXT_FIELDS = List.of("communications.subject", "description", "errandNumber", "title");

	@Autowired
	private ErrandsRepository errandsRepository;

	private ErrandSearchAccess searchAccess;

	@BeforeEach
	void setUp() {
		final var index = mock(ErrandIndexModel.class);
		when(index.textFields()).thenReturn(TEXT_FIELDS);
		searchAccess = new ErrandSearchAccess(index);
	}

	/**
	 * The shapes that make the two sides differ: whether the user reported the errand, which labels they hold and at
	 * which level, whether the namespace excepts reporters, whether a role narrows what is read, and whether a limited
	 * read is told what to expose.
	 * <p>
	 * Errands carrying a label, which is every errand there is: labels are what access control is made of, so an errand
	 * carrying none is not access controlled and the mapper shows it in full to anyone who reaches it. The search instead
	 * answers such an errand from whichever route reached it, and a limited route exposing a field a role withholds at
	 * full read would then search more than the answer shows. It takes an errand with no labels and a namespace whose
	 * limited read is wider than a role's full read at once, and the deviation is pinned by
	 * {@link #anErrandWithoutLabelsIsTheKnownDeviation} rather than left to be rediscovered.
	 */
	static Stream<Arguments> combinations() {
		final List<Set<MetadataLabelEntity>> grantedLabels = List.of(Set.of(), Set.of(LABEL), Set.of(LABEL, OTHER_LABEL));

		return Stream.of(true).flatMap(labelled -> Stream.of(true, false).flatMap(reporter -> grantedLabels.stream().flatMap(labels -> Stream.of(LR, R, RW).flatMap(grantedAt -> Stream.of(true, false).flatMap(reporterAccess -> Stream.of(true, false)
			.map(roleRestriction -> Arguments.of(labelled, reporter, labels, grantedAt, reporterAccess, roleRestriction)))))));
	}

	@ParameterizedTest(name = "labelled={0} reporter={1} labels={2} grantedAt={3} reporterAccess={4} roleRestriction={5}")
	@MethodSource("combinations")
	void noRouteSearchesAFieldTheAnswerWouldLeaveOut(final boolean labelled, final boolean reporter, final Set<MetadataLabelEntity> grantedLabels,
		final Access.AccessLevelEnum grantedAt, final boolean reporterAccess, final boolean roleRestriction) {

		final var errand = persistErrand(labelled, reporter);
		final var config = config(reporterAccess, roleRestriction);
		final var access = snapshot(grantedLabels, grantedAt);

		final var grant = NamespaceGrantResolver.namespaceGrant(config, access, adUser(), R);
		final var shown = NamespaceGrantResolver.fieldAccessResolver(config, access, AD_ACCOUNT).apply(errand).readable();

		for (final var clause : searchAccess.plan("", Sort.unsorted(), grant).clauses()) {
			if (!reaches(clause, errand)) {
				continue;
			}
			for (final var searched : clause.fields()) {
				fieldsBinding(searched).forEach(field -> assertThat(isShown(shown, field))
					.as("route searching '%s' on an errand whose answer leaves %s out", searched, field)
					.isTrue());
			}
		}
	}

	/**
	 * The one shape where the two sides part, kept here so that a change to either is noticed rather than silently
	 * widening or narrowing what a search of an unlabelled errand may look in.
	 * <p>
	 * The mapper shows such an errand in full - an errand with no labels is not access controlled - while the search
	 * answers it from the limited route when the user holds nothing at read. Accepted because no errand is ever written
	 * without a label, and because the search is then narrower than the answer in every configuration where a limited
	 * read exposes no more than a role allows at full read.
	 */
	@Test
	void anErrandWithoutLabelsIsTheKnownDeviation() {
		final var errand = persistErrand(false, false);
		final var config = config(false, true);
		final var access = snapshot(Set.of(LABEL), LR);

		final var grant = NamespaceGrantResolver.namespaceGrant(config, access, adUser(), R);
		final var shown = NamespaceGrantResolver.fieldAccessResolver(config, access, AD_ACCOUNT).apply(errand).readable();

		// The answer shows what the role allows, the errand being covered by labels the user does not hold at read at all
		assertThat(shown).containsOnlyKeys(ErrandField.STATUS, ErrandField.TITLE);

		// While the one route reaching it is the limited one, which looks in what a limited read exposes
		final var clauses = searchAccess.plan("", Sort.unsorted(), grant).clauses();
		assertThat(clauses).hasSize(1);
		assertThat(clauses.getFirst().fields()).contains("errandNumber", "description");
		assertThat(reaches(clauses.getFirst(), errand)).isTrue();
	}

	/** Whether the clause reaches the errand, asked of the specification the database is guarded with. */
	private boolean reaches(final ErrandSearchAccess.Clause clause, final ErrandEntity errand) {
		final var within = errandsRepository.exists(where(withId(errand.getId())).and(ErrandAccessSpecifications.withAccessControl(clause.scope())));
		final var left = clause.excluded() != null
			&& errandsRepository.exists(where(withId(errand.getId())).and(ErrandAccessSpecifications.withAccessControl(clause.excluded())));

		return within && !left;
	}

	/**
	 * The fields of the errand an index field belongs to. A name belonging only to a resource is not one of them: a
	 * resource is reached whole or not at all and the mapper never trims it, which is settled before a route is built.
	 */
	private static Stream<ErrandField> fieldsBinding(final String name) {
		return Stream.of(ErrandField.values()).filter(field -> field.getSearchFields().stream().anyMatch(bound -> bound.equals(name)));
	}

	/** A null map is an unrestricted user, which is not the same as one restricted to nothing. */
	private static boolean isShown(final Map<ErrandField, Set<String>> shown, final ErrandField field) {
		return shown == null || shown.containsKey(field);
	}

	private ErrandEntity persistErrand(final boolean labelled, final boolean reporter) {
		final var errand = ErrandEntity.create()
			.withNamespace(NAMESPACE)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withErrandNumber("PARITY-" + randomUUID())
			.withTitle("title")
			.withReporterUserId(reporter ? AD_ACCOUNT : "someone01else")
			.withAccessLabels(labelled ? List.of(AccessLabelEmbeddable.create().withMetadataLabelId(LABEL.getId())) : List.of());

		return errandsRepository.saveAndFlush(errand);
	}

	/**
	 * A role seeing the status and the title alone, a limited read told to expose the description, and reporters excepted
	 * with nothing said about their fields - the three ways the sides can come to differ, switched on together.
	 */
	private static NamespaceConfig config(final boolean reporterAccess, final boolean roleRestriction) {
		var config = NamespaceConfig.create()
			.withAccessControl(true)
			.withLimitedReadAccess(LimitedReadAccess.create()
				.withFields(List.of(FieldAccess.create().withField(ErrandField.ERRAND_NUMBER), FieldAccess.create().withField(ErrandField.DESCRIPTION))));

		if (roleRestriction) {
			config = config.withRoleBasedMapping(true)
				.withRoleFieldRestrictions(List.of(RoleFieldRestriction.create().withRole(ROLE)
					.withFields(List.of(FieldAccess.create().withField(ErrandField.STATUS), FieldAccess.create().withField(ErrandField.TITLE)))));
		}

		return reporterAccess ? config.withReporterAccess(ReporterAccess.create()
			.withResources(List.of(ResourceAccess.create().withResource(ProtectedResource.ERRAND).withLevel(AccessLevel.R)))) : config;
	}

	private static AccessSnapshot snapshot(final Set<MetadataLabelEntity> labels, final Access.AccessLevelEnum grantedAt) {
		return new AccessSnapshot(
			Map.of(
				LR, LR == grantedAt ? labels : Set.of(),
				R, R == grantedAt ? labels : Set.of(),
				RW, RW == grantedAt ? labels : Set.of()),
			Set.of(ROLE),
			Map.of());
	}

	private static Identifier adUser() {
		return Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue(AD_ACCOUNT);
	}
}
