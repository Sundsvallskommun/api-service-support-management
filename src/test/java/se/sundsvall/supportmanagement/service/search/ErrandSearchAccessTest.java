package se.sundsvall.supportmanagement.service.search;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.support.Identifier;
import se.sundsvall.supportmanagement.integration.db.model.MetadataLabelEntity;
import se.sundsvall.supportmanagement.integration.db.model.enums.ErrandField;
import se.sundsvall.supportmanagement.integration.db.model.enums.ProtectedResource;
import se.sundsvall.supportmanagement.service.AccessControlService;
import se.sundsvall.supportmanagement.service.AccessControlService.AccessScope;
import se.sundsvall.supportmanagement.service.AccessControlService.SearchFieldAccess;

import static generated.se.sundsvall.accessmapper.Access.AccessLevelEnum.R;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class ErrandSearchAccessTest {

	private static final String NAMESPACE = "namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final Set<MetadataLabelEntity> LABELS = Set.of(MetadataLabelEntity.create().withId("label"));
	private static final Sort UNSORTED = Sort.unsorted();

	@Mock
	private AccessControlService accessControlServiceMock;

	private Identifier user;

	@BeforeEach
	void setUp() {
		user = Identifier.create().withType(Identifier.Type.AD_ACCOUNT).withValue("joe01doe");
		Identifier.set(user);
	}

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	private ErrandSearchAccess access() {
		return new ErrandSearchAccess(accessControlServiceMock);
	}

	/** Every resource reached through the labels, no field restriction, and the given routes on the errand. */
	private void everythingOpen(final AccessScope errand) {
		when(accessControlServiceMock.accessScope(any(), any(), any(), any(), any())).thenReturn(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R)).thenReturn(errand);
		when(accessControlServiceMock.searchFieldAccess(NAMESPACE, MUNICIPALITY_ID, user)).thenReturn(new SearchFieldAccess(null, null));
	}

	private ThrowableProblem refused(final String query, final Sort sort) {
		final var resolved = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);
		return assertThrows(ThrowableProblem.class, () -> access().plan(query, sort, resolved));
	}

	@Test
	void everythingIsOpenWithoutAccessControl() {
		when(accessControlServiceMock.accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.ERRAND, R)).thenReturn(new AccessScope(false, null, null));

		final var resolved = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);
		final var plan = access().plan("communications.subject:x AND \\*.probability:3", Sort.by("created"), resolved);

		assertThat(resolved.closed()).isEmpty();
		assertThat(resolved.reported()).isNull();
		assertThat(plan.fields()).isEqualTo(ErrandSearchPredicates.DEFAULT_FIELDS);
		assertThat(plan.scope().enforced()).isFalse();
		verify(accessControlServiceMock, never()).searchFieldAccess(any(), any(), any());
	}

	@Test
	void errandsAndResourcesAreResolvedAtFullRead() {
		everythingOpen(new AccessScope(true, LABELS, null));

		access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		Stream.of(ProtectedResource.values())
			.filter(resource -> !resource.getSearchFields().isEmpty())
			.forEach(resource -> verify(accessControlServiceMock).accessScope(NAMESPACE, MUNICIPALITY_ID, user, resource, R));
		verify(accessControlServiceMock, never()).accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.NOTE, R);
	}

	@Test
	void resourcesTheLabelsDoNotReachAreClosed() {
		everythingOpen(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.COMMUNICATION, R)).thenReturn(new AccessScope(true, null, null));
		// Reached through reporting only, which does not open it for the search
		when(accessControlServiceMock.accessScope(NAMESPACE, MUNICIPALITY_ID, user, ProtectedResource.DECISION, R)).thenReturn(new AccessScope(true, Set.of(), "joe01doe"));

		final var resolved = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);
		final var plan = access().plan("title:vatten", UNSORTED, resolved);

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
		everythingOpen(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(user), eq(ProtectedResource.COMMUNICATION), eq(R))).thenReturn(new AccessScope(true, null, null));

		final var e = refused(query, UNSORTED);

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
		everythingOpen(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(user), eq(ProtectedResource.COMMUNICATION), eq(R))).thenReturn(new AccessScope(true, null, null));

		final var e = refused(query, UNSORTED);

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
		everythingOpen(new AccessScope(true, LABELS, null));
		when(accessControlServiceMock.accessScope(eq(NAMESPACE), eq(MUNICIPALITY_ID), eq(user), eq(ProtectedResource.COMMUNICATION), eq(R))).thenReturn(new AccessScope(true, null, null));
		final var resolved = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		assertThatCode(() -> access().plan(query, Sort.by("created"), resolved)).doesNotThrowAnyException();
	}

	@Test
	void fieldsTheRolesKeepFromTheUserAreClosed() {
		everythingOpen(new AccessScope(true, LABELS, null));
		// A role seeing the title, the status and one key of each keyed field
		when(accessControlServiceMock.searchFieldAccess(NAMESPACE, MUNICIPALITY_ID, user)).thenReturn(new SearchFieldAccess(Map.of(
			ErrandField.TITLE, Set.of(),
			ErrandField.STATUS, Set.of(),
			ErrandField.PARAMETERS, Set.of("granted-key"),
			ErrandField.JSON_PARAMETERS, Set.of("granted-json"),
			ErrandField.EXTERNAL_TAGS, Set.of("caseId")), null));
		final var resolved = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		// Free text keeps to the title and to the resources guarded on their own, which the roles do not govern; the
		// values of parameters and JSON parameters are shared by every key
		assertThat(access().plan("", UNSORTED, resolved).fields())
			.contains("title", "communications.subject", "decisions.title", "attachments.fileName")
			.doesNotContain("description", "stakeholders.lastName", "jsonParametersText", "parameters.values", "externalTags.value", "contactReasonDescription");

		// Open: the fields, and the JSON parameter key, the role sees
		assertThatCode(() -> access().plan("title:x AND status:new AND jsonParameters.granted-json.visible:true AND jsonParameters.granted-json.deep.path.raw:x", UNSORTED, resolved))
			.doesNotThrowAnyException();

		assertThat(refused("description:x", UNSORTED).getDetail()).isEqualTo("Field 'description' not searchable by user 'joe01doe'");
		assertThat(refused("stakeholders.lastName:berg", UNSORTED).getDetail()).isEqualTo("Field 'stakeholders' not searchable by user 'joe01doe'");
		assertThat(refused("jsonParameters.hidden-json.secret:x", UNSORTED).getDetail()).isEqualTo("Key 'hidden-json' of Field 'jsonParameters' not searchable by user 'joe01doe'");
		assertThat(refused("parameters.values:x", UNSORTED).getDetail()).isEqualTo("Field 'parameters' beyond its keys not searchable by user 'joe01doe'");
		assertThat(refused("externalTags.value:x", UNSORTED).getDetail()).isEqualTo("Field 'externalTags' beyond its keys not searchable by user 'joe01doe'");
		assertThat(refused("", Sort.by("created")).getDetail()).isEqualTo("Field 'created' not sortable by user 'joe01doe'");
		assertThatCode(() -> access().plan("", Sort.by("title"), resolved)).doesNotThrowAnyException();
	}

	@Test
	void reporterAloneIsHeldToTheReporterFields() {
		everythingOpen(new AccessScope(true, null, "joe01doe"));
		when(accessControlServiceMock.searchFieldAccess(NAMESPACE, MUNICIPALITY_ID, user)).thenReturn(new SearchFieldAccess(null, Map.of(
			ErrandField.ERRAND_NUMBER, Set.of(),
			ErrandField.TITLE, Set.of(),
			ErrandField.STATUS, Set.of())));
		final var resolved = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		assertThat(resolved.reported()).isNull();
		assertThat(access().plan("title:x", UNSORTED, resolved).fields())
			.contains("errandNumber", "title")
			.doesNotContain("description", "stakeholders.lastName", "jsonParametersText");
		assertThat(access().plan("title:x", UNSORTED, resolved).scope()).isEqualTo(resolved.errand());
		assertThat(refused("description:x", UNSORTED).getDetail()).isEqualTo("Field 'description' not searchable by user 'joe01doe'");
	}

	@Test
	void ownErrandsAreSearchedOnlyWithinTheReporterFields() {
		everythingOpen(new AccessScope(true, LABELS, "joe01doe"));
		when(accessControlServiceMock.searchFieldAccess(NAMESPACE, MUNICIPALITY_ID, user)).thenReturn(new SearchFieldAccess(null, Map.of(
			ErrandField.ERRAND_NUMBER, Set.of(),
			ErrandField.TITLE, Set.of())));
		final var resolved = access().resolve(NAMESPACE, MUNICIPALITY_ID, user);

		// Within the reporter fields: both routes
		assertThat(access().plan("title:x", Sort.by("title"), resolved).scope().reporterAdAccount()).isEqualTo("joe01doe");
		// Fielded terms alone, with groups and ranges, are told apart from free text
		assertThat(access().plan("title:(x OR y) AND NOT errandNumber:[a TO b]", UNSORTED, resolved).scope().reporterAdAccount()).isEqualTo("joe01doe");
		// Beyond them, whether by a field, a sort or the free text: the label covered errands alone, and no refusal
		assertThat(access().plan("description:x", UNSORTED, resolved).scope().reporterAdAccount()).isNull();
		assertThat(access().plan("title:x", Sort.by("created"), resolved).scope().reporterAdAccount()).isNull();
		assertThat(access().plan("vatten", UNSORTED, resolved).scope().reporterAdAccount()).isNull();
		assertThat(access().plan("vatten", UNSORTED, resolved).fields()).isEqualTo(ErrandSearchPredicates.DEFAULT_FIELDS);
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
