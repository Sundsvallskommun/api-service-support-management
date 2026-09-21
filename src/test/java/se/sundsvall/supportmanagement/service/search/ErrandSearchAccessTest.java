package se.sundsvall.supportmanagement.service.search;

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
import se.sundsvall.supportmanagement.service.access.NamespaceGrant;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant.LabelRoute;
import se.sundsvall.supportmanagement.service.access.NamespaceGrant.ReporterRoute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

	private final ErrandSearchAccess access = new ErrandSearchAccess();

	@BeforeEach
	void setUp() {
		Identifier.set(Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe"));
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	private static NamespaceGrant grant(final LabelRoute labels, final ReporterRoute reporter, final Set<ProtectedResource> resources) {
		return new NamespaceGrant(true, labels, reporter, resources);
	}

	private static Set<ProtectedResource> allBut(final ProtectedResource... closed) {
		final var reached = new java.util.HashSet<>(EVERY_RESOURCE);
		reached.removeAll(Set.of(closed));
		return reached;
	}

	private ThrowableProblem refused(final NamespaceGrant grant, final String query, final Sort sort) {
		final var resolved = access.resolve(grant);
		return assertThrows(ThrowableProblem.class, () -> access.plan(query, sort, resolved));
	}

	@Test
	void everythingIsOpenWithoutAccessControl() {
		final var resolved = access.resolve(NamespaceGrant.UNRESTRICTED);
		final var plan = access.plan("communications.subject:x AND \\*.probability:3", Sort.by("created"), resolved);

		assertThat(resolved.closed()).isEmpty();
		assertThat(resolved.reported()).isNull();
		assertThat(plan.fields()).isEqualTo(ErrandSearchPredicates.DEFAULT_FIELDS);
		assertThat(plan.scope().enforced()).isFalse();
	}

	@Test
	void resourcesTheLabelsDoNotReachAreClosed() {
		final var resolved = access.resolve(grant(new LabelRoute(LABELS, null), null, allBut(ProtectedResource.COMMUNICATION, ProtectedResource.DECISION)));
		final var plan = access.plan("title:vatten", UNSORTED, resolved);

		assertThat(resolved.closed()).extracting(ErrandSearchAccess.Closed::name).containsExactlyInAnyOrder("communications.", "decisions.");
		assertThat(plan.fields())
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
		final var e = refused(grant(new LabelRoute(LABELS, null), null, allBut(ProtectedResource.COMMUNICATION)), query, UNSORTED);

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
		final var e = refused(grant(new LabelRoute(LABELS, null), null, allBut(ProtectedResource.COMMUNICATION)), query, UNSORTED);

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
		final var resolved = access.resolve(grant(new LabelRoute(LABELS, null), null, allBut(ProtectedResource.COMMUNICATION)));

		assertThatCode(() -> access.plan(query, Sort.by("created"), resolved)).doesNotThrowAnyException();
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
		final var grant = grant(new LabelRoute(LABELS, role), null, EVERY_RESOURCE);
		final var resolved = access.resolve(grant);

		// Free text keeps to the title and to the resources guarded on their own, which the roles do not govern; the
		// values of parameters and JSON parameters are shared by every key
		assertThat(access.plan("", UNSORTED, resolved).fields())
			.contains("title", "communications.subject", "decisions.title", "attachments.fileName")
			.doesNotContain("description", "stakeholders.lastName", "jsonParametersText", "parameters.values", "externalTags.value", "contactReasonDescription");

		// Open: the fields, and the JSON parameter key, the role sees
		assertThatCode(() -> access.plan("title:x AND status:new AND jsonParameters.granted-json.visible:true AND jsonParameters.granted-json.deep.path.raw:x", UNSORTED, resolved))
			.doesNotThrowAnyException();
		assertThatCode(() -> access.plan("", Sort.by("title"), resolved)).doesNotThrowAnyException();

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
		final var grant = grant(null, new ReporterRoute("joe01doe", reporterFields), Set.of());
		final var resolved = access.resolve(grant);

		assertThat(resolved.reported()).isNull();
		assertThat(access.plan("title:x", UNSORTED, resolved).fields())
			.contains("errandNumber", "title")
			.doesNotContain("description", "stakeholders.lastName", "jsonParametersText", "communications.subject");
		assertThat(access.plan("title:x", UNSORTED, resolved).scope()).isEqualTo(grant.scope());
		assertThat(refused(grant, "description:x", UNSORTED).getDetail()).isEqualTo("Field 'description' not searchable by user 'joe01doe'");
	}

	@Test
	void ownErrandsAreSearchedOnlyWithinTheReporterFields() {
		final var reporterFields = Map.of(ErrandField.ERRAND_NUMBER, Set.<String>of(), ErrandField.TITLE, Set.<String>of());
		final var resolved = access.resolve(grant(new LabelRoute(LABELS, null), new ReporterRoute("joe01doe", reporterFields), EVERY_RESOURCE));

		// Within the reporter fields: both routes
		assertThat(access.plan("title:x", Sort.by("title"), resolved).scope().reporterAdAccount()).isEqualTo("joe01doe");
		// Fielded terms alone, with groups and ranges, are told apart from free text
		assertThat(access.plan("title:(x OR y) AND NOT errandNumber:[a TO b]", UNSORTED, resolved).scope().reporterAdAccount()).isEqualTo("joe01doe");
		// Beyond them, whether by a field, a sort or the free text: the label covered errands alone, and no refusal
		assertThat(access.plan("description:x", UNSORTED, resolved).scope().reporterAdAccount()).isNull();
		assertThat(access.plan("title:x", Sort.by("created"), resolved).scope().reporterAdAccount()).isNull();
		assertThat(access.plan("vatten", UNSORTED, resolved).scope().reporterAdAccount()).isNull();
		assertThat(access.plan("vatten", UNSORTED, resolved).fields()).isEqualTo(ErrandSearchPredicates.DEFAULT_FIELDS);
	}
}

class ErrandSearchAccessFreeTermsTest {

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {
		"vatten", "title:x vatten", "title:(a OR b) läcka", "\"a phrase\" AND word", "-läcka", "berg*"
	})
	void freeTerms(final String query) {
		assertThat(ErrandSearchAccess.hasFreeTerms(query)).isTrue();
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {
		"", " ", "title:x", "title:(a OR b) AND NOT status:new", "created:[2025-01-01 TO 2025-12-31]", "created:{* TO now-7d}", "title:\"a phrase\"", "_exists_:assignedUserId", "+title:x -status:closed"
	})
	void fieldedOnly(final String query) {
		assertThat(ErrandSearchAccess.hasFreeTerms(query)).isFalse();
	}
}
