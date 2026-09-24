package se.sundsvall.supportmanagement.service.search;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.access.AccessScope;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant.LabelRoute;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant.ReporterRoute;
import se.sundsvall.supportmanagement.service.search.index.ErrandIndexModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * The search side of the grant: a {@link NamespaceGrant} in, what may be searched out. Nothing here asks the access
 * service, since the grant is the one answer it gives.
 */
class ErrandSearchAccessTest {

	private static final Set<MetadataLabelEntity> LABELS = Set.of(MetadataLabelEntity.create().withId("label"));
	private static final Sort UNSORTED = Sort.unsorted();
	private static final Set<ProtectedResource> EVERY_RESOURCE = Set.of(ProtectedResource.COMMUNICATION, ProtectedResource.DECISION, ProtectedResource.STATEMENT,
		ProtectedResource.INVESTIGATION, ProtectedResource.MEASURE, ProtectedResource.PARAMETER, ProtectedResource.JSON_PARAMETER, ProtectedResource.ATTACHMENT);

	/** A cut of the text fields of the real index, enough to tell the closures apart. */
	private static final List<String> TEXT_FIELDS = List.of("attachments.fileName", "communications.subject", "communications.messageBody", "contactReasonDescription",
		"decisions.title", "decisions.justification", "description", "errandNumber", "externalTags.value", "jsonParametersText", "measures.title", "parameters.values",
		"stakeholders.lastName", "title");

	private final ErrandSearchAccess access = new ErrandSearchAccess(indexModel());

	private static ErrandIndexModel indexModel() {
		final var model = mock(ErrandIndexModel.class);
		when(model.textFields()).thenReturn(TEXT_FIELDS);
		return model;
	}

	private ErrandSearchAccess.Plan plan(final String query, final Sort sort, final NamespaceGrant grant) {
		return access.plan(query, sort, grant);
	}

	/** The fields of the one clause a single route search runs with. */
	private List<String> fieldsOf(final String query, final Sort sort, final NamespaceGrant grant) {
		final var clauses = plan(query, sort, grant).clauses();
		assertThat(clauses).hasSize(1);
		return clauses.getFirst().fields();
	}

	/** The scopes of the clauses, in the order the plan put them. */
	private List<AccessScope> scopesOf(final String query, final Sort sort, final NamespaceGrant grant) {
		return plan(query, sort, grant).clauses().stream().map(ErrandSearchAccess.Clause::scope).toList();
	}

	/** What each clause leaves out of its scope, in the same order. */
	private List<AccessScope> exclusionsOf(final String query, final Sort sort, final NamespaceGrant grant) {
		return plan(query, sort, grant).clauses().stream().map(ErrandSearchAccess.Clause::excluded).toList();
	}

	@BeforeEach
	void setUp() {
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	private static NamespaceGrant grant(final LabelRoute labels, final ReporterRoute reporter, final Set<ProtectedResource> resources) {
		return new NamespaceGrant(true, withResources(labels, resources), null, withResources(reporter, resources));
	}

	private static NamespaceGrant grant(final LabelRoute labels, final LabelRoute limited, final ReporterRoute reporter, final Set<ProtectedResource> resources) {
		return new NamespaceGrant(true, withResources(labels, resources), withResources(limited, resources), withResources(reporter, resources));
	}

	/** The grant as the routes were built, each with resources of its own. */
	private static NamespaceGrant grantOf(final LabelRoute labels, final LabelRoute limited, final ReporterRoute reporter) {
		return new NamespaceGrant(true, labels, limited, reporter);
	}

	private static LabelRoute withResources(final LabelRoute route, final Set<ProtectedResource> resources) {
		return route == null ? null : new LabelRoute(route.labels(), route.readable(), resources);
	}

	private static ReporterRoute withResources(final ReporterRoute route, final Set<ProtectedResource> resources) {
		return route == null ? null : new ReporterRoute(route.adAccount(), route.readable(), resources);
	}

	private static Set<ProtectedResource> allBut(final ProtectedResource... closed) {
		final var reached = new java.util.HashSet<>(EVERY_RESOURCE);
		reached.removeAll(Set.of(closed));
		return reached;
	}

	private ThrowableProblem refused(final NamespaceGrant grant, final String query, final Sort sort) {
		return assertThrows(ThrowableProblem.class, () -> access.plan(query, sort, grant));
	}

	@Test
	void everythingIsOpenWithoutAccessControl() {
		final var plan = plan("communications.subject:x AND \\*.probability:3", Sort.by("created"), NamespaceGrant.UNRESTRICTED);

		assertThat(plan.clauses()).hasSize(1);
		assertThat(plan.clauses().getFirst().fields()).isEqualTo(TEXT_FIELDS);
		assertThat(plan.clauses().getFirst().scope().enforced()).isFalse();
	}

	@Test
	void resourcesTheLabelsDoNotReachAreClosed() {
		final var fields = fieldsOf("title:vatten", UNSORTED, grant(new LabelRoute(LABELS, null, EVERY_RESOURCE), null, allBut(ProtectedResource.COMMUNICATION, ProtectedResource.DECISION)));

		assertThat(fields)
			.doesNotContain("communications.subject", "communications.messageBody", "decisions.title", "decisions.justification")
			.contains("title", "measures.title", "jsonParametersText");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"communications.subject:läcka",
		"title:vatten AND (status:new OR communications.messageBody:läcka)",
		"communications.sent:[2025-01-01 TO 2025-12-31]",
		"_exists_:communications.subject"
	})
	void queryNamingAClosedResourceIsRefused(final String query) {
		final var e = refused(grant(new LabelRoute(LABELS, null, EVERY_RESOURCE), null, allBut(ProtectedResource.COMMUNICATION)), query, UNSORTED);

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(e.getDetail()).isEqualTo("Resource 'errand/communication' not searchable by user 'joe01doe'");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"jsonParameters.\\*.regNo:abc",
		"\\*.subject:x",
		"comm?nications.subject:x"
	})
	void wildcardFieldNamesAreRefusedWhileSomethingIsClosed(final String query) {
		final var e = refused(grant(new LabelRoute(LABELS, null, EVERY_RESOURCE), null, allBut(ProtectedResource.COMMUNICATION)), query, UNSORTED);

		assertThat(e.getStatus()).isEqualTo(FORBIDDEN);
		assertThat(e.getDetail()).isEqualTo("A wildcard in a field name is not available to user 'joe01doe', who may not search every field of the errand");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"",
		"vatten läcka",
		"title:vatten AND measures.title:armatur",
		"\"communications.subject:inside a phrase is a phrase\"",
		"description:communications.subject",
		"stakeholders.lastName:berg* created:>=2025-01-01",
		"communications\\:literal"
	})
	void queriesThatStayWithinWhatIsOpenPass(final String query) {
		final var grant = grant(new LabelRoute(LABELS, null, EVERY_RESOURCE), null, allBut(ProtectedResource.COMMUNICATION));

		assertThatCode(() -> plan(query, Sort.by("created"), grant)).doesNotThrowAnyException();
	}

	@Test
	void fieldsTheRolesKeepFromTheUserAreClosed() {
		// A role seeing the title, the status and one key of each keyed field
		final var role = Map.of(
			ErrandField.TITLE, Set.<String>of(),
			ErrandField.STATUS, Set.<String>of(),
			ErrandField.PARAMETERS, Set.of("granted-key"),
			ErrandField.JSON_PARAMETERS, Set.of("granted-json"),
			ErrandField.EXTERNAL_TAGS, Set.of("caseId"));
		final var grant = grant(new LabelRoute(LABELS, role, EVERY_RESOURCE), null, EVERY_RESOURCE);

		// Free text keeps to the title and to the resources guarded on their own, which the roles do not govern; the
		// values of parameters and JSON parameters are shared by every key
		assertThat(fieldsOf("", UNSORTED, grant))
			.contains("title", "communications.subject", "decisions.title", "attachments.fileName")
			.doesNotContain("description", "stakeholders.lastName", "jsonParametersText", "parameters.values", "externalTags.value", "contactReasonDescription");

		// Open: the fields, and the JSON parameter key, the role sees
		assertThatCode(() -> plan("title:x AND status:new AND jsonParameters.granted-json.visible:true AND jsonParameters.granted-json.deep.path.raw:x", UNSORTED, grant))
			.doesNotThrowAnyException();
		assertThatCode(() -> plan("", Sort.by("title"), grant)).doesNotThrowAnyException();

		assertThat(refused(grant, "description:x", UNSORTED).getDetail()).isEqualTo("Field 'description' not searchable by user 'joe01doe'");
		assertThat(refused(grant, "stakeholders.lastName:berg", UNSORTED).getDetail()).isEqualTo("Field 'stakeholders' not searchable by user 'joe01doe'");
		assertThat(refused(grant, "jsonParameters.hidden-json.secret:x", UNSORTED).getDetail()).isEqualTo("Key 'hidden-json' of Field 'jsonParameters' not searchable by user 'joe01doe'");
		assertThat(refused(grant, "parameters.values:x", UNSORTED).getDetail()).isEqualTo("Field 'parameters' beyond its keys not searchable by user 'joe01doe'");
		assertThat(refused(grant, "externalTags.value:x", UNSORTED).getDetail()).isEqualTo("Field 'externalTags' beyond its keys not searchable by user 'joe01doe'");
		assertThat(refused(grant, "", Sort.by("created")).getDetail()).isEqualTo("Field 'created' not sortable by user 'joe01doe'");
	}

	@Test
	void reporterAloneIsHeldToTheReporterFields() {
		final var reporterFields = Map.of(ErrandField.ERRAND_NUMBER, Set.<String>of(), ErrandField.TITLE, Set.<String>of(), ErrandField.STATUS, Set.<String>of());
		final var grant = grantOf(null, null, new ReporterRoute("joe01doe", reporterFields, Set.of()));

		assertThat(fieldsOf("title:x", UNSORTED, grant))
			.contains("errandNumber", "title")
			.doesNotContain("description", "stakeholders.lastName", "jsonParametersText", "communications.subject");
		assertThat(scopesOf("title:x", UNSORTED, grant)).containsExactly(grant.reporterScope());
		assertThat(refused(grant, "description:x", UNSORTED).getDetail()).isEqualTo("Field 'description' not searchable by user 'joe01doe'");
	}

	@Test
	void ownErrandsAreSearchedByWhatAReporterMayRead() {
		final var reporterFields = Map.of(ErrandField.ERRAND_NUMBER, Set.<String>of(), ErrandField.TITLE, Set.<String>of());
		final var grant = grantOf(new LabelRoute(LABELS, null, EVERY_RESOURCE), null, new ReporterRoute("joe01doe", reporterFields, Set.of()));

		// Within the reporter fields: both routes, the label covered errands first
		assertThat(scopesOf("title:x", Sort.by("title"), grant)).containsExactly(NamespaceGrant.scopeOf(grant.labels()), grant.reporterScope());
		// Beyond them, whether by a field or a sort: the label covered errands alone, and no refusal
		assertThat(scopesOf("description:x", UNSORTED, grant)).containsExactly(NamespaceGrant.scopeOf(grant.labels()));
		assertThat(scopesOf("title:x", Sort.by("created"), grant)).containsExactly(NamespaceGrant.scopeOf(grant.labels()));
		// A word without a field is looked for in what each route leaves open, so both answer it
		assertThat(scopesOf("vatten", UNSORTED, grant)).containsExactly(NamespaceGrant.scopeOf(grant.labels()), grant.reporterScope());
		assertThat(plan("vatten", UNSORTED, grant).clauses().getFirst().fields()).isEqualTo(TEXT_FIELDS);
		assertThat(plan("vatten", UNSORTED, grant).clauses().getLast().fields()).containsExactly("errandNumber", "title");
	}

	/**
	 * Errands the labels cover at limited read are searched by what a limited read exposes: found by their title, and
	 * simply not searched by a field, or a resource, a limited read does not carry.
	 */
	@Test
	void limitedReadIsSearchedByWhatALimitedReadExposes() {
		final var limitedFields = Map.of(ErrandField.ID, Set.<String>of(), ErrandField.ERRAND_NUMBER, Set.<String>of(), ErrandField.TITLE, Set.<String>of(), ErrandField.STATUS, Set.<String>of());
		// The namespace extends its limited read to the communications and to nothing else
		final var limited = new LabelRoute(LABELS, limitedFields, Set.of(ProtectedResource.COMMUNICATION));
		final var grant = grantOf(null, limited, null);

		assertThat(scopesOf("title:x", UNSORTED, grant)).containsExactly(NamespaceGrant.scopeOf(limited));
		assertThat(exclusionsOf("title:x", UNSORTED, grant)).containsOnlyNulls();
		assertThat(fieldsOf("vatten", UNSORTED, grant)).containsExactly("communications.subject", "communications.messageBody", "errandNumber", "title");
		assertThat(refused(grant, "description:x", UNSORTED).getDetail()).isEqualTo("Field 'description' not searchable by user 'joe01doe'");
		assertThat(refused(grant, "decisions.title:x", UNSORTED).getDetail()).isEqualTo("Resource 'errand/decision' not searchable by user 'joe01doe'");
	}

	/**
	 * The mixed case: the labels cover one errand at read and another at limited read only. A query the limited fields
	 * cannot answer is answered from the covered errands alone, without a refusal, and one they can is answered from
	 * both, each by its own fields.
	 */
	@Test
	void readAndLimitedReadAreSearchedSideBySide() {
		final var covered = new LabelRoute(Set.of(MetadataLabelEntity.create().withId("covered")), null, EVERY_RESOURCE);
		final var limitedFields = Map.of(ErrandField.ERRAND_NUMBER, Set.<String>of(), ErrandField.TITLE, Set.<String>of());
		final var limited = new LabelRoute(Set.of(MetadataLabelEntity.create().withId("covered"), MetadataLabelEntity.create().withId("limited")), limitedFields, Set.of());
		final var grant = grantOf(covered, limited, null);

		// The body is readable on the covered errands only
		assertThat(scopesOf("description:hemligt", UNSORTED, grant)).containsExactly(NamespaceGrant.scopeOf(covered));
		// The title is readable on both, so both answer, each with its own fields
		final var both = plan("title:vatten", UNSORTED, grant).clauses();
		assertThat(both).extracting(ErrandSearchAccess.Clause::scope).containsExactly(NamespaceGrant.scopeOf(covered), NamespaceGrant.scopeOf(limited));
		assertThat(both.getFirst().fields()).isEqualTo(TEXT_FIELDS);
		assertThat(both.getLast().fields()).containsExactly("errandNumber", "title");

		// The covered errands are held at read, so the limited clause leaves them out rather than searching them by what
		// a limited read exposes
		assertThat(both.getFirst().excluded()).isNull();
		assertThat(both.getLast().excluded()).isEqualTo(NamespaceGrant.scopeOf(covered));
	}

	/**
	 * A query no route can answer is refused, worded by the widest route.
	 */
	@Test
	void aQueryNoRouteCanAnswerIsRefused() {
		final var role = Map.of(ErrandField.TITLE, Set.<String>of());
		final var limitedFields = Map.of(ErrandField.TITLE, Set.<String>of());
		final var grant = grantOf(new LabelRoute(LABELS, role, EVERY_RESOURCE), new LabelRoute(LABELS, limitedFields, Set.of()), new ReporterRoute("joe01doe", limitedFields, Set.of()));

		assertThat(refused(grant, "description:x", UNSORTED).getDetail()).isEqualTo("Field 'description' not searchable by user 'joe01doe'");
	}
}
